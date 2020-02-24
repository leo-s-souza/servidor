/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLang;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangJson;
import br.com.egula.capp.classes.*;
import br.com.egula.capp.model.Dispositivo;
import br.com.egula.capp.model.Usuario;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;

import static br.com.egula.capp.ModuloRest.writeResponse;
import static br.com.egula.capp.classes.CLSUtilidades.atualizaBonusUsuarioFirebird;

/**
 * Classe responsável por realizar o login de usuários do Egula.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class LoginHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            StringBuilder sb = new StringBuilder();

            // Lê o conteúdo vindo no POST.
            {
                String inputLine;

                BufferedReader in = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()));
                while ((inputLine = in.readLine()) != null) {
                    sb.append(inputLine);
                }
                in.close();
            }

            if (sb.length() > 0 && sb.toString().trim().length() > 0) {
                String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();
                CSPLog.info(LoginHandler.class, "login(" + hostAddress + "): " + sb.toString());

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject input = new JSONObject(sb.toString());
                Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                if (dev != null) {
                    String emailCpf = CSPUtilidadesLangJson.getFromJson(input, "email", "").trim();
                    String senha = CSPUtilidadesLangJson.getFromJson(input, "senha", "").trim().toLowerCase();
                    String facebookId = CSPUtilidadesLangJson.getFromJson(input, "facebook_id", "").trim();

                    if ((!CSPUtilidadesLang.isEmailValid(emailCpf) && !CSPUtilidadesLang.isCpfValid(emailCpf)) && facebookId.isEmpty()) {
                        writeResponse(httpExchange, 403, "CPF, Email ou Facebook ID inválido");
                        return;
                    }

                    final ResultSet validacao = conn.selectOneRow((StringBuilder s) -> {
                        s.append("SELECT ");
                        s.append("	p.USER_ID, ");
                        s.append("	p.LOGADO_EM_OUTRO ");
                        s.append("FROM ");
                        s.append("	PR_VALIDA_LOGIN (?, ?, ?, ?) p; ");

                    }, emailCpf, senha, facebookId, dev.getId());

                    final int idUser = validacao.getInt("USER_ID");

                    if (idUser != 0) {
                        //Login OK - Podemos registrar que logou
                        conn.execute(
                                "EXECUTE PROCEDURE PR_REGISTRA_LOGADO( ?, ?, ?, 1)",
                                idUser,
                                dev.getId(),
                                hostAddress
                        );

//                        if(!conn.getConfs().getHost().equals(CLSUtilidadesServidores.getHostPrincipal())) {
//                            CLSPendencias.addPendencia(input, "login", hostAddress);
//                        }

                        JSONObject output;

                        final Usuario usuario = CLSUtilidades.getUsuario(idUser, conn);

                        if (usuario != null) {
                            Gson gson = new Gson();
                            output = new JSONObject(gson.toJson(usuario));

                            Firestore database = FirestoreClient.getFirestore();
                            atualizaBonusUsuarioFirebird(database.collection("usuario").document(String.valueOf(usuario.getId())), usuario.getId(), conn);

                            // Confirma o login e envia os dados do usuário
                            writeResponse(httpExchange, 200, output.toString());

                        } else {
                            writeResponse(httpExchange, 500, "Internal Server Error");
                        }

                    } else if (validacao.getLong("LOGADO_EM_OUTRO") != 0) {
                        //Login NO - Logado em outro dev
                        writeResponse(httpExchange, 400, "Logado em outro dispositivo");

                    } else {
                        //Login NO - Usuário/Senha incorretos
                        writeResponse(httpExchange, 401, "Usuário/Senha incorretos");
                    }
                }
            }
        } catch (Exception e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }

    /**
     * Método para execução de pendencia de login
     *
     * @param input - Json com as informações do login.
     * @param remoteAddress - Endereço ip do usuário que fez o processo.
     * @param conn - Conexão com a base.
     * @throws SQLException
     */
    public static void processaLoginPendente(JSONObject input, String remoteAddress, CSPInstrucoesSQLBase conn) throws SQLException {

        CSPLog.info(LoginHandler.class, "Processando pendência de login: " + input.toString());

        String emailCpf = CSPUtilidadesLangJson.getFromJson(input, "email", "").trim();
        String senha = CSPUtilidadesLangJson.getFromJson(input, "senha", "").trim().toLowerCase();
        String facebookId = CSPUtilidadesLangJson.getFromJson(input, "facebook_id", "").trim();
        Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

        final ResultSet validacao = conn.selectOneRow((StringBuilder s) -> {
            s.append("SELECT ");
            s.append("	p.USER_ID, ");
            s.append("	p.LOGADO_EM_OUTRO ");
            s.append("FROM ");
            s.append("	PR_VALIDA_LOGIN (?, ?, ?, ?) p; ");

        }, emailCpf, senha, facebookId, dev.getId());

        final int idUser = validacao.getInt("USER_ID");

        if (idUser != 0) {
            //Login OK - Podemos registrar que logou
            conn.execute(
                    "EXECUTE PROCEDURE PR_REGISTRA_LOGADO( ?, ?, ?, 1)",
                    idUser,
                    dev.getId(),
                    remoteAddress
            );
        }

        CSPLog.info(LoginHandler.class, "Pendência de login processada!");
    }
}
