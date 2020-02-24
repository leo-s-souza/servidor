/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLang;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangJson;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.classes.CLSPendencias;
import br.com.egula.capp.classes.CLSUtilidadesServidores;
import br.com.egula.capp.model.Dispositivo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Classe responsável por realizar o logoff forçado de usuários.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class LogoffForcadoHandler implements HttpHandler {
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
                CSPLog.info(LogoffForcadoHandler.class, "forca-logoff(" + hostAddress + "): " + sb.toString());

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject input = new JSONObject(sb.toString());
                Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                if (dev != null) {
                    String emailCpf = CSPUtilidadesLangJson.getFromJson(input, "email", "").trim();
                    String senha = CSPUtilidadesLangJson.getFromJson(input, "senha", "").trim().toLowerCase();
                    String facebookId = CSPUtilidadesLangJson.getFromJson(input, "facebook_id", "").trim();

                    if ((!CSPUtilidadesLang.isEmailValid(emailCpf) && !CSPUtilidadesLang.isCpfValid(emailCpf)) && facebookId.isEmpty()) {
                        writeResponse(httpExchange, 400, "CPF, Email ou Facebook ID inválido");
                        return;
                    }

                    final ResultSet validacao = conn.selectOneRow((StringBuilder s) -> {
                        s.append("SELECT ");
                        s.append("	p.LOGADO_EM_OUTRO ");
                        s.append("FROM ");
                        s.append("	PR_VALIDA_LOGIN (?, ?, ?, ?) p; ");

                    }, emailCpf, senha, facebookId, dev.getId());

                    final long outroDev = validacao.getLong("LOGADO_EM_OUTRO");

                    if (outroDev != 0) {
                        conn.execute(
                                "EXECUTE PROCEDURE PR_REGISTRA_LOGADO (?, ?, ?, 0);",
                                null,
                                outroDev,
                                hostAddress
                        );

//                        if(!conn.getConfs().getHost().equals(CLSUtilidadesServidores.getHostPrincipal())) {
//                            CLSPendencias.addPendencia(input, "forca-logoff", hostAddress);
//                        }

                        writeResponse(httpExchange, 200, "OK");

                    }

                } else {
                    writeResponse(httpExchange, 500, "Internal Server Error");
                }
            }
        } catch (Exception e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }

    /**
     * Processa um logoff forçado de usuário pendente no servidor secundário.
     *
     * @param input
     * @param remoteAddress
     * @param conn
     * @throws SQLException
     */
    public static void processaLogoffForcadoPendente (JSONObject input, String remoteAddress, CSPInstrucoesSQLBase conn) throws SQLException {
        CSPLog.info(AdicionaUsuarioHandler.class, "Processando pendência de logoff forçado de usuário: " + input.toString());

        Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

        String emailCpf = CSPUtilidadesLangJson.getFromJson(input, "email", "").trim();
        String senha = CSPUtilidadesLangJson.getFromJson(input, "senha", "").trim().toLowerCase();
        String facebookId = CSPUtilidadesLangJson.getFromJson(input, "facebook_id", "").trim();


        final ResultSet validacao = conn.selectOneRow((StringBuilder s) -> {
            s.append("SELECT ");
            s.append("	p.LOGADO_EM_OUTRO ");
            s.append("FROM ");
            s.append("	PR_VALIDA_LOGIN (?, ?, ?, ?) p; ");

        }, emailCpf, senha, facebookId, dev.getId());

        final long outroDev = validacao.getLong("LOGADO_EM_OUTRO");

        if (outroDev != 0) {
            conn.execute(
                    "EXECUTE PROCEDURE PR_REGISTRA_LOGADO (?, ?, ?, 0);",
                    null,
                    outroDev,
                    remoteAddress
            );

        }

        CSPLog.info(AdicionaUsuarioHandler.class, "Pendência de logoff forçado de usuário processada!");
    }
}
