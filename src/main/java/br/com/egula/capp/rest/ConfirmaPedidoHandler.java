/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.FrmModuloPaiBase;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLang;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangDateTime;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.classes.CLSNotificacao;
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
 * Atende requisições da loja para confirmação de pedidos.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class ConfirmaPedidoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) {

        try {
            CSPLog.info(ConfirmaPedidoHandler.class, "confirma-pedido-web(" + t.getRemoteAddress().getAddress().getHostAddress() + ")");

            // Pra garantir que apenas a aplicação web seja capaz de confirmar pedidos
            if (!t.getRemoteAddress().getAddress().getHostAddress().equals("177.54.11.194") && !t.getRemoteAddress().getAddress().getHostAddress().equals("35.199.104.29") && !t.getRemoteAddress().getAddress().getHostAddress().equals("177.37.93.25")) {
                writeResponse(t, 403, "Proibido");
                return;
            }

            final Map<String, String> params = queryToMap(t.getRequestURI().getQuery());

            final long pedidoId = Long.parseLong(params.get("id"));
            final int previsao = Integer.parseInt(params.get("previsao"));
            final String host = params.get("host");

            final java.util.Date entregaEstimada = CSPUtilidadesLangDateTime.getDataHoraObj(0, previsao);

            params.clear();

            final CSPInstrucoesSQLBase conn = CLSCapp.connDb();

            conn.execute(
                    "execute procedure PR_CONFIRMA_PEDIDO_WEB ( ?, ?, ?)",
                    pedidoId, host, entregaEstimada
            );

            // Registra e dispara notificação do pedido confirmado para o usuário que fez o mesmo.
            try {
                ResultSet rs = conn.select(
                        "SELECT  l.ID, l.nome as loja, USUARIO_ENDERECO_USUARIO_ID as USUARIO_ID, PAGO_EM_BONUS as IS_PAGO_EM_BONUS, IS_NOVO FROM PEDIDO join loja l on l.id = LOJA_ENDERECO_LOJA_ID WHERE pedido.ID = ?",
                        pedidoId
                );
                if (rs.next()) {
                    long usuarioId = rs.getLong("USUARIO_ID");
                    long lojaId = rs.getLong("ID");
                    String loja = rs.getString("LOJA");
                    boolean isNovo = rs.getInt("IS_NOVO") == 1;
                    boolean pagoEmBonus = rs.getInt("IS_PAGO_EM_BONUS") == 1;

                    String imagemLoja = "not-found";
                    String imagemBonus = "https://firebasestorage.googleapis.com/v0/b/e-gula.appspot.com/o/ic_bonus_small.png?alt=media&token=a47a9803-db25-42a5-87d1-6d309fdafeaf";

                    ResultSet resultSet = conn.select("SELECT a.IMAGEM FROM VW_INFOS_LOJA_FIREBASE_NOVO a WHERE a.ID = ? ", lojaId);

                    if (resultSet.next()) {
                        imagemLoja = resultSet.getString("IMAGEM");
                    }

                    Firestore database = FirestoreClient.getFirestore();
                    DocumentReference refUsuario = database.collection("usuario").document(String.valueOf(usuarioId));
                    CollectionReference refNotificacao = database.collection("usuario").document(String.valueOf(usuarioId)).collection("notificacao");

                    if (isNovo) {
                        // Firebase
                        Notificacao notificacao = new Notificacao(
                                "Pedido Confirmado",
                                loja + " agradece a sua preferência. Seu pedido foi confirmado e entregaremos aproximadamente as " + CSPUtilidadesLangDateTime.formataDataHora(entregaEstimada, "HH:mm"),
                                imagemLoja,
                                0,
                                new Date(),
                                null);

                        resultSet = conn.selectOneRow(sb -> {
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

                        if (resultSet.getString("ID_HARDWARE") != null && resultSet.getString("ID_HARDWARE").startsWith("io")) {
                            pushNotificationAppApns(notificacao, resultSet.getString("ID_HARDWARE"), conn);
                        }

                        refNotificacao.add(notificacao);


                    } else {
                        CLSNotificacao.registraNotificacaoUsuariosFCM(
                                CLSNotificacao.Notificacao.P5,
                                usuarioId,
                                pedidoId,
                                loja + " agradece a sua preferência. Seu pedido foi confirmado e entregaremos aproximadamente as " + CSPUtilidadesLangDateTime.formataDataHora(entregaEstimada, "HH:mm")
                        );
                    }

                    if (pagoEmBonus) {
                        if (isNovo) {
                            ResultSet res = conn.selectOneRow("select (select MONEY_REAL from PR_FORMAT_MONEY_REAL((r.TOTAL / 100) * lbc.PERCENTUAL,2)) AS BONUS_FORMAT, r.TOTAL FROM PEDIDO r join LOJA_BONUS_CONF lbc on lbc.LOJA_ID = r.LOJA_ENDERECO_LOJA_ID where lbc.ID = r.LOJA_BONUS_CONF_ID and r.id = ?",
                                    pedidoId
                            );

                            String bonusFormatado = res.getString("BONUS_FORMAT");
                            Double total = res.getDouble("TOTAL");

                            conn.execute("execute procedure PR_ATUALIZA_BONUS_CLIENTE(?, ?, 0)", usuarioId, total);

                            double bonusLivre = conn.selectOneRow("SELECT p.TOTAL_BONUS_LIVRE FROM PR_RETORNA_BONUS_USUARIO (?) p",
                                    usuarioId
                            ).getDouble("TOTAL_BONUS_LIVRE");

                            Notificacao notificacao = new Notificacao(
                                    "Bônus Gerado",
                                    "Parabéns, seu pedido #"
                                            + pedidoId
                                            + " foi aceito e pago com seus bônus e-GULA. \n\nSeu saldo atual de bônus é de "
                                            + CSPUtilidadesLang.currencyRealFormat(bonusLivre)
                                            + " já creditado " + bonusFormatado
                                            + " referente ao bônus gerado pelo pedido.",
                                    imagemBonus,
                                    0,
                                    new Date(),
                                    null
                            );

                            resultSet = conn.selectOneRow(sb -> {
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

                            if (resultSet.getString("ID_HARDWARE") != null && resultSet.getString("ID_HARDWARE").startsWith("io")) {
                                pushNotificationAppApns(notificacao, resultSet.getString("ID_HARDWARE"), conn);
                            }

                            refNotificacao.add(notificacao);

                            atualizaBonusUsuarioFirebird(refUsuario, usuarioId, conn);

                        } else {
                            CLSNotificacao.registraNotificacaoUsuariosFCM(
                                    CLSNotificacao.Notificacao.B3,
                                    usuarioId,
                                    pedidoId
                            );
                        }

                    } else {
                        if (isNovo) {
                            ResultSet res = conn.selectOneRow("select (select MONEY_REAL from PR_FORMAT_MONEY_REAL((r.TOTAL / 100) * lbc.PERCENTUAL,2)) AS BONUS_FORMAT, ((r.TOTAL / 100) * lbc.PERCENTUAL) as BONUS FROM PEDIDO r join LOJA_BONUS_CONF lbc on lbc.LOJA_ID = r.LOJA_ENDERECO_LOJA_ID where lbc.ID = r.LOJA_BONUS_CONF_ID and r.id = ?",
                                    pedidoId
                            );

                            String bonusFormatado = res.getString("BONUS_FORMAT");
                            Double bonusDouble = res.getDouble("BONUS");

                            conn.execute("execute procedure PR_ATUALIZA_BONUS_CLIENTE(?, ?, 1)", usuarioId, bonusDouble);

                            double bonusLivre = conn.selectOneRow("SELECT p.TOTAL_BONUS_LIVRE FROM PR_RETORNA_BONUS_USUARIO (?) p",
                                    usuarioId
                            ).getDouble("TOTAL_BONUS_LIVRE");


                            Notificacao notificacao = new Notificacao(
                                    "Bônus Gerado",
                                    "e-GULA informa que foram creditados "
                                            + bonusFormatado
                                            + " de bônus referentes ao pedido #"
                                            + pedidoId
                                            + ". Seu saldo de bônus agora é "
                                            + CSPUtilidadesLang.currencyRealFormat(bonusLivre)
                                            + ".",
                                    imagemBonus,
                                    0,
                                    new Date(),
                                    null
                            );

                            resultSet = conn.selectOneRow(sb -> {
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

                            if (resultSet.getString("ID_HARDWARE") != null && resultSet.getString("ID_HARDWARE").startsWith("io")) {
                                pushNotificationAppApns(notificacao, resultSet.getString("ID_HARDWARE"), conn);
                            }

                            refNotificacao.add(notificacao);

                            atualizaBonusUsuarioFirebird(refUsuario, usuarioId, conn);
                        } else {
                            CLSNotificacao.registraNotificacaoUsuariosFCM(
                                    CLSNotificacao.Notificacao.B1,
                                    usuarioId,
                                    pedidoId
                            );
                        }
                    }
                }
            } catch (Exception ex) {
                CLSException.register(ex);
                ex.printStackTrace();
            }

            try {
                final List<Integer> usuarioIds = new ArrayList<>();

                ResultSet resultSet = CLSCapp.connDb().select("SELECT USUARIO_ID FROM VW_USUARIO_MONITORAMENTO");
                while (resultSet.next()) {
                    usuarioIds.add(resultSet.getInt("USUARIO_ID"));
                }

                String texto = CLSCapp.connDb().selectOneRow(
                        (StringBuilder sb) -> {
                            sb.append("execute block ( ");
                            sb.append("    PEDIDO_ID BIGINT = ?, ");
                            sb.append("    PREVISAO_ENTREGA TIMESTAMP = ? ");
                            sb.append(") returns ( ");
                            sb.append("    TEXTO VARCHAR(3000) ");
                            sb.append(") AS ");
                            sb.append("DECLARE VARIABLE NOME_LOJA DM_NOME; ");
                            sb.append("DECLARE VARIABLE NOME_USER DM_NOME; ");
                            sb.append("BEGIN ");
                            sb.append("    SELECT ");
                            sb.append("        l.NOME, ");
                            sb.append("        u.NOME_COMPLETO ");
                            sb.append("    FROM ");
                            sb.append("        PEDIDO p ");
                            sb.append("    JOIN USUARIO u ");
                            sb.append("        on u.ID = p.USUARIO_ENDERECO_USUARIO_ID ");
                            sb.append("    JOIN loja l ");
                            sb.append("        on l.ID = p.LOJA_ENDERECO_LOJA_ID ");
                            sb.append("    WHERE ");
                            sb.append("        p.ID = :PEDIDO_ID ");
                            sb.append("    INTO ");
                            sb.append("        :NOME_LOJA, ");
                            sb.append("        :NOME_USER; ");
                            sb.append("    TEXTO = 'Confirmado ' ");
                            sb.append("            || :nome_user ");
                            sb.append("            || ' #' ");
                            sb.append("            || :PEDIDO_ID ");
                            sb.append("            || ' ' ");
                            sb.append("            || (SELECT p.FORMATADO FROM PR_FORMAT_HORARIO ('now') p) ");
                            sb.append("            || '<n>Loja: ' ");
                            sb.append("            || :nome_loja ");
                            sb.append("            || '<n>Previsão de entrega informada pela loja: ' ");
                            sb.append("            || (SELECT p.FORMATADO FROM PR_FORMAT_HORARIO(:PREVISAO_ENTREGA) p ); ");
                            sb.append("    SUSPEND; ");
                            sb.append("END ");
                        },
                        pedidoId,
                        entregaEstimada
                ).getString("TEXTO");

                FrmModuloPaiBase.executor((executor) -> {
                    // Notifica os dispositivos de monitoramento que um pedido foi efetuado.
                    Firestore database = FirestoreClient.getFirestore();
                    CollectionReference colRef;

                    for (int usuarioId : usuarioIds) {
                        colRef = database.collection("usuario").document(String.valueOf(usuarioId)).collection("notificacao");

                        Notificacao notificacao = new Notificacao(
                                "Monitoramento: Confirmado",
                                texto,
                                "https://firebasestorage.googleapis.com/v0/b/e-gula.appspot.com/o/icone_monitoramento.png?alt=media&token=5f0ecb1c-76f5-4757-8cb7-7e38fa37bd99",
                                0,
                                new Date(),
                                null
                        );

                        colRef.add(notificacao);
                    }

                    executor.shutdown();
                }, 0, 1);

            } catch (SQLException exc) {
                CLSException.register(exc);
            }

            conn.close();
            writeResponse(t, 200, "OK");

        } catch (Exception ex) {
            CLSException.register(ex);
            writeResponse(t, 500, "Internal Server Error");
        }
    }
}
