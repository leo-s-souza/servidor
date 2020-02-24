/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.FrmModuloPaiBase;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.classes.CLSNotificacao;
import br.com.egula.capp.classes.CLSNotificacaoApns;
import br.com.egula.capp.model.Notificacao;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static br.com.egula.capp.ModuloRest.queryToMap;
import static br.com.egula.capp.ModuloRest.writeResponse;
import static br.com.egula.capp.classes.CLSNotificacaoApns.pushNotificationAppApns;
import static br.com.egula.capp.classes.CLSUtilidades.atualizaBonusUsuarioFirebird;

/**
 * Atende requisições da loja para cancelamento de pedidos.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class CancelaPedidoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) {

        try {
            CSPLog.info(CancelaPedidoHandler.class, "cancela-pedido-web(" + t.getRemoteAddress().getAddress().getHostAddress() + ")");

            // Pra garantir que apenas a aplicação web seja capaz de confirmar pedidos
            if (!t.getRemoteAddress().getAddress().getHostAddress().equals("177.54.11.194") && !t.getRemoteAddress().getAddress().getHostAddress().equals("35.199.104.29") && !t.getRemoteAddress().getAddress().getHostAddress().equals("177.37.93.25")) {
                writeResponse(t, 403, "Proibido");
                return;
            }

            final Map<String, String> params = queryToMap(t.getRequestURI().getQuery());

            final long pedidoId = Long.parseLong(params.get("id"));
            final String host = params.get("host");

            params.clear();

            final CSPInstrucoesSQLBase conn = CLSCapp.connDb();

            conn.execute(
                    "execute procedure PR_CANCELA_PEDIDO_WEB (?, ?)",
                    pedidoId, host
            );

            // Registra e dispara notificação do pedido cancelado para o usuário que fez o mesmo.
            try {
                ResultSet rs = conn.select(
                        "SELECT l.ID, l.nome as LOJA, USUARIO_ENDERECO_USUARIO_ID as USUARIO_ID, IS_NOVO FROM PEDIDO join loja l on l.id = LOJA_ENDERECO_LOJA_ID WHERE pedido.ID = ?",
                        pedidoId
                );

                if (rs.next()) {
                    long usuarioId = rs.getLong("USUARIO_ID");
                    long lojaId = rs.getLong("ID");
                    String loja = rs.getString("LOJA");
                    boolean isNovo = rs.getInt("IS_NOVO") == 1;

                    String imagemLoja = "not-found";

                    ResultSet resSet = conn.select("SELECT a.IMAGEM FROM VW_INFOS_LOJA_FIREBASE_NOVO a WHERE a.ID = ? ", lojaId);

                    if (resSet.next()) {
                        imagemLoja = resSet.getString("IMAGEM");
                    }

                    Firestore database = FirestoreClient.getFirestore();
                    DocumentReference refUsuario = database.collection("usuario").document(String.valueOf(usuarioId));
                    CollectionReference colRef = database.collection("usuario").document(String.valueOf(usuarioId)).collection("notificacao");

                    if (isNovo) {
                        // Firebase
                        Notificacao notificacao = new Notificacao(
                                "Pedido Cancelado",
                                loja + " agradece sua preferência, mas seu pedido foi cancelado.",
                                imagemLoja,
                                0,
                                new Date(),
                                null);

                        resSet = conn.selectOneRow(sb -> {
                            sb.append("select ");
                            sb.append("    d.ID_HARDWARE ");
                            sb.append("from ");
                            sb.append("    PEDIDO p ");
                            sb.append("join ");
                            sb.append("    DISPOSITIVO_ACAO da ON da.ID = p.DISPOSITIVO_ACAO_ID ");
                            sb.append("join ");
                            sb.append("    DISPOSITIVO d ON d.ID = da.DISPOSITIVO_ID ");
                            sb.append("where ");
                            sb.append("    p.ID = ? ");
                        }, pedidoId);

                        if(resSet.getString("ID_HARDWARE") != null && resSet.getString("ID_HARDWARE").startsWith("io")){
                            pushNotificationAppApns(notificacao, resSet.getString("ID_HARDWARE"), conn);
                        }

                        colRef.add(notificacao);

                        atualizaBonusUsuarioFirebird(refUsuario, usuarioId, conn);
                    } else {

                        CLSNotificacao.registraNotificacaoUsuariosFCM(
                                CLSNotificacao.Notificacao.P6,
                                rs.getLong("USUARIO_ID"),
                                pedidoId,
                                rs.getString("LOJA") + " agradece sua preferência, mas seu pedido foi cancelado"
                        );
                    }


                    // Notificação de Monitoramento.
                    try {
                        final List<Integer> usuarioIds = new ArrayList<>();

                        ResultSet resultSet = CLSCapp.connDb().select("SELECT USUARIO_ID FROM VW_USUARIO_MONITORAMENTO");
                        while (resultSet.next()) {
                            usuarioIds.add(resultSet.getInt("USUARIO_ID"));
                        }

                        FrmModuloPaiBase.executor((executor) -> {
                            // Notifica os dispositivos de monitoramento que um pedido foi efetuado.
                            Firestore db = FirestoreClient.getFirestore();
                            CollectionReference collectionReference;

                            for (int usuarioMonitorId : usuarioIds) {
                                collectionReference = db.collection("usuario").document(String.valueOf(usuarioMonitorId)).collection("notificacao");

                                Notificacao notificacao = new Notificacao(
                                        "Monitoramento: Cancelado",
                                        "Pedido #" + pedidoId + " da loja " + loja + " foi cancelado por um operador.",
                                        "https://firebasestorage.googleapis.com/v0/b/e-gula.appspot.com/o/icone_monitoramento.png?alt=media&token=5f0ecb1c-76f5-4757-8cb7-7e38fa37bd99",
                                        0,
                                        new Date(),
                                        null
                                );

                                collectionReference.add(notificacao);
                            }

                            executor.shutdown();
                        },0, 1);

                    } catch (SQLException exc) {
                        CLSException.register(exc);
                    }


                }
            } catch (Exception ex) {
                CLSException.register(ex);
            }

            conn.close();
            writeResponse(t, 200, "OK");

        } catch (Exception ex) {
            CLSException.register(ex);
            writeResponse(t, 500, "Internal Server Error");
        }
    }
}