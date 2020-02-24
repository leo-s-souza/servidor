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
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLang;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangJson;
import br.com.egula.capp.ModuloRest;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.model.Dispositivo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.sql.ResultSet;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Classe responsável por realizar o logoff forçado de garçons do Egula Garçom.
 *
 * @author Matheus Felipe Amelco
 * @since 1.4
 */
public class LogoffForcadoGarcomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String conteudo = ModuloRest.readPostContent(httpExchange);

            if (!conteudo.isEmpty()) {
                String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();
                CSPLog.info(LogoffForcadoGarcomHandler.class, "forca-logoff(" + hostAddress + "): " + conteudo);

                JSONObject input = new JSONObject(conteudo);
                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                if (dev != null) {
                    String emailCpf = CSPUtilidadesLangJson.getFromJson(input, "email", "").trim();
                    String senha = CSPUtilidadesLangJson.getFromJson(input, "senha", "").trim().toLowerCase();

                    if ((!CSPUtilidadesLang.isEmailValid(emailCpf) && !CSPUtilidadesLang.isCpfValid(emailCpf))) {
                        writeResponse(httpExchange, 400, "CPF ou Email inválido");
                        return;
                    }

                    final ResultSet validacao = conn.selectOneRow((StringBuilder s) -> {
                        s.append("SELECT ");
                        s.append("	p.LOGADO_EM_OUTRO ");
                        s.append("FROM ");
                        s.append("	PR_VALIDA_LOGIN_GARCOM (?, ?, ?) p; ");

                    }, emailCpf, senha, dev.getId());

                    final long outroDev = validacao.getLong("LOGADO_EM_OUTRO");

                    if (outroDev != 0) {
                        conn.execute(
                                "EXECUTE PROCEDURE PR_REGISTRA_LOGIN_LOGOFF_GARCOM (?, ?, ?, 0);",
                                null,
                                outroDev,
                                hostAddress
                        );

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
}
