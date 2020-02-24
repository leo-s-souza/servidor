/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

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
import br.com.egula.capp.model.Dispositivo;
import br.com.egula.capp.model.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;

import static br.com.egula.capp.ModuloRest.writeResponse;

public class VerificaLoginHandler implements HttpHandler {

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
                String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();
                CSPLog.info(LoginHandler.class, "verifica-login(" + hostAddress + "): " + sb.toString());

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject input = new JSONObject(sb.toString());
                Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                if (dev == null) {
                    writeResponse(httpExchange, 401, "Id de hardware não encontrado.");
                }

                int id = CSPUtilidadesLangJson.getFromJson(input, "id", 0);

                if(input.has("usuarioCpf")){
                    int idUserCpf = Usuario.getIdUsuarioCpf(input.getString("usuarioCpf"), conn);

                    if(idUserCpf != id){
                        //Login NO

                        writeResponse(httpExchange, 400, "Logado em outro dispositivo");
                        return;
                    }
                }

                final ResultSet validacao = conn.selectOneRow((StringBuilder s) -> {
                    s.append("SELECT ");
                    s.append("	p.IS_LOGADO_EM_OUTRO ");
                    s.append("FROM ");
                    s.append("	PR_VERIFICA_LOGIN (?, ?) p;");
                }, id, dev.getId());

                final int isLogadoEmOutro = validacao.getInt("IS_LOGADO_EM_OUTRO");

                if (isLogadoEmOutro == 0) {
                    //Login verificado e válido
                    writeResponse(httpExchange, 200, "Login válido");
                } else {
                    //Login NO - Logado em outro dev
                    writeResponse(httpExchange, 400, "Logado em outro dispositivo");

                }
            }
        } catch (Exception e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }
}
