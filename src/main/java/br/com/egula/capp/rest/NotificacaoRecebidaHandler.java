/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.model.Dispositivo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringJoiner;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Recebe os IDs das notificações recebidas no dispositivo, apenas para confirmação.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class NotificacaoRecebidaHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
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
                JSONObject input = new JSONObject(sb.toString());
                final CSPInstrucoesSQLBase conn = CLSCapp.connDb();

                // Verificamos se existe o código do dispositivo na requisição. Se não existir, não podemos atender.
                if (!input.has("hardware_id") || input.isNull("hardware_id")) {
                    writeResponse(httpExchange, 403, "Proibido");
                    return;
                }
                final Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                if (dev != null) {

                    final ResultSet rs = conn.selectOneRow((StringBuilder s) -> {
                        s.append("SELECT ");
                        s.append("	r.USUARIO_ID ");
                        s.append("FROM ");
                        s.append("	VW_USUARIO_LOGADO r ");
                        s.append("where ");
                        s.append("    r.DISPOSITIVO_ID = ? ");
                    }, dev.getId());

                    final long idUserLogado = rs == null ? -1 : rs.getLong(1);

                    final StringJoiner ids = new StringJoiner(",");
                    for (Object id : input.getJSONArray("notificacoes")) {
                        ids.add(id.toString());
                    }

                    conn.execute(
                            "EXECUTE PROCEDURE PR_UP_STATUS_NOTIFS (?, ?, ?, ?, ?)",
                            ids.toString(),
                            input.getBoolean("visualizada") ? 1 : 0,
                            idUserLogado,
                            httpExchange.getRemoteAddress().getAddress().getHostAddress(),
                            dev.getId()
                    );

                    writeResponse(httpExchange, 200, "OK");
                }
            }

        } catch (Exception e) {
            CLSException.register(e);
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }
}
