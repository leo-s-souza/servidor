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
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.classes.CLSUtilidades;
import br.com.egula.capp.model.Dispositivo;
import br.com.egula.capp.model.garcom.Garcom;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Classe responsável por realizar o login de garçons do Egula Garçom.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class LoginGarcomHandler implements HttpHandler {

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
                CSPLog.info(LoginGarcomHandler.class, "garçom-login(" + hostAddress + "): " + sb.toString());

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject input = new JSONObject(sb.toString());
                Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                if (dev != null) {
                    String emailCpf = CSPUtilidadesLangJson.getFromJson(input, "email", "").trim();
                    String senha = CSPUtilidadesLangJson.getFromJson(input, "senha", "").trim().toLowerCase();

                    if (!CSPUtilidadesLang.isEmailValid(emailCpf) && !CSPUtilidadesLang.isCpfValid(emailCpf)) {
                        writeResponse(httpExchange, 403, "CPF ou Email inválido");
                        return;
                    }

                    final ResultSet validacao = conn.selectOneRow((StringBuilder s) -> {
                        s.append("SELECT ");
                        s.append("	p.G_ID, ");
                        s.append("	p.LOGADO_EM_OUTRO ");
                        s.append("FROM ");
                        s.append("	PR_VALIDA_LOGIN_GARCOM (?, ?, ?) p; ");

                    }, emailCpf, senha, dev.getId());

                    final int idGarcom = validacao.getInt("G_ID");

                    if (idGarcom != 0) {
                        //Login OK - Podemos registrar que logou
                        conn.execute(
                                "EXECUTE PROCEDURE PR_REGISTRA_LOGIN_LOGOFF_GARCOM( ?, ?, ?, 1)",
                                idGarcom,
                                dev.getId(),
                                hostAddress
                        );


                        final Garcom garcom = CLSUtilidades.getGarcom(idGarcom, conn);

                        if (garcom != null) {
                            Gson gson = new Gson();
                            String garcomJSON = gson.toJson(garcom);

                            // Confirma o login e envia os dados do garçom
                            writeResponse(httpExchange, 200, garcomJSON);

                        } else {
                            writeResponse(httpExchange, 500, "Internal Server Error");
                        }

                    } else if (validacao.getLong("LOGADO_EM_OUTRO") != 0) {
                        //Login NO - Logado em outro dev
                        writeResponse(httpExchange, 400, "Logado em outro dispositivo");

                    } else {
                        //Login NO - Usuário/Senha incorretos
                        writeResponse(httpExchange, 401, "Email/Senha incorretos");
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
