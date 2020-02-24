/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangJson;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.model.Dispositivo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Classe responsável por registrar os tokens do APNS
 *
 * @author Djeferson Preis
 */
public class RegistraTokenApnsHandler implements HttpHandler {

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

                //Inicia conexão com a base e insere o conteúdo do POST em um JSONObject
                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject input = new JSONObject(sb.toString());
                if (!input.has("hardware_id") || input.isNull("hardware_id")) {
                    writeResponse(httpExchange, 403, "Proibido");
                    return;
                }
                Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                if (dev != null) {
                    Long idDispositivo = dev.getId();
                    String token = CSPUtilidadesLangJson.getFromJson(input, "token", "").trim();

                    //Realiza a chamada da procedure que realizará o registro do Token
                    conn.execute((StringBuilder s) -> {
                        s.append("EXECUTE PROCEDURE ");
                        s.append("	PR_REGISTRA_NOVO_TOKEN_APNS (?, ?); ");

                    }, idDispositivo, token);


                    // Escreve a resposta no de sucesso.
                    writeResponse(httpExchange, 200, "");

                } else {
                    // Escreve a resposta no output avisando que houve algum problema interno.
                    writeResponse(httpExchange, 500, "Internal Server Error");
                }
            }
        } catch (Exception e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }
}
