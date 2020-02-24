/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */
package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangJson;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.classes.CLSUtilidades;
import br.com.egula.capp.model.Dispositivo;
import br.com.egula.capp.model.Usuario;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Classe responsável por realizar o login de usuários do Egula que estão migrando da versão 2.0 para 3.0.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class LoginMigracaoHandler implements HttpHandler {
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
                CSPLog.info(LoginMigracaoHandler.class, "login-migracao(" + hostAddress + "): " + sb.toString());

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject input = new JSONObject(sb.toString());
                Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                if (dev != null) {
                    int usuarioId = Integer.parseInt(CSPUtilidadesLangJson.getFromJson(input, "id", "").trim());

                    if (usuarioId != 0) {
                        conn.execute("update DISPOSITIVO d set d.FORCE_LOGOFF_PENDENTE = 0 where d.id = ?", dev.getId());
                        conn.execute(
                                "EXECUTE PROCEDURE PR_REGISTRA_LOGADO (?, ?, ?, 0);",
                                usuarioId,
                                dev.getId(),
                                hostAddress);

                        conn.execute(
                                "EXECUTE PROCEDURE PR_REGISTRA_LOGADO( ?, ?, ?, 1)",
                                usuarioId,
                                dev.getId(),
                                hostAddress
                        );

                        JSONObject output;

                        final Usuario usuario = CLSUtilidades.getUsuario(usuarioId, conn);

                        if (usuario != null) {
                            Gson gson = new Gson();
                            output = new JSONObject(gson.toJson(usuario));

                            // Confirma o login e envia os dados do usuário
                            writeResponse(httpExchange, 200, output.toString());

                        } else {
                            writeResponse(httpExchange, 500, "Internal Server Error");
                        }


                    }
                }
            }
        } catch (Exception e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }
}
