/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest.garcom;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.egula.capp.ModuloRest;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.model.Dispositivo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Classe responsável por realizar o logoff de garçons do Egula Garçom.
 *
 * @author Matheus Felipe Amelco
 * @since 1.4
 */
public class LogoffGarcomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String conteudo = ModuloRest.readPostContent(httpExchange);

            if (conteudo.length() > 0) {

                String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();
                CSPLog.info(LogoffGarcomHandler.class, "logoff-garcom(" + hostAddress + "): " + conteudo);

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject content = new JSONObject(conteudo);

                Dispositivo dev = Dispositivo.getDispositivo(content.getString("hardware_id"), conn);


                if (dev != null) {
                    int userId = content.getInt("garcom_id");
                    long devId = dev.getId();

                    conn.execute("update DISPOSITIVO d set d.FORCE_LOGOFF_PENDENTE = 0 where d.id = ?", devId);
                    conn.execute(
                            "EXECUTE PROCEDURE PR_REGISTRA_LOGIN_LOGOFF_GARCOM (?, ?, ?, 0);",
                            userId,
                            devId,
                            hostAddress
                    );

                    writeResponse(httpExchange, 200, "OK");


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
