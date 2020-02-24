/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.FrmModuloPaiBase;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.rede.CSPEmailClientSender;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLang;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;

import static br.com.egula.capp.ModuloRest.writeResponse;

public class RecuperarSenhaHandler implements HttpHandler {
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
                CSPLog.info(RecuperarSenhaHandler.class, "login(" + hostAddress + "): " + sb.toString());

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject input = new JSONObject(sb.toString());

                final ResultSet rs = conn.select((StringBuilder s) -> {
                    s.append("SELECT ");
                    s.append("    u.NOME, ");
                    s.append("    u.SENHA, ");
                    s.append("    u.EMAIL ");
                    s.append("FROM ");
                    s.append("    USUARIO u ");
                    s.append("where ");
                    s.append("    u.ID = ? ");
                }, input.getInt("usuario_id"));

                if (rs.next()) {
                    String nome = rs.getString("NOME");
                    String email = rs.getString("EMAIL");
                    String senha = rs.getString("SENHA");

                    FrmModuloPaiBase.executor((executor) -> {
                        String sbHtml = "Olá, " +
                                "<b>" + nome + "</b>! " +
                                "<br/>" +
                                "Recebemos a sua solicitação de recuperação de senha. " +
                                "<br/>" +
                                "<br/>" +
                                "Sua senha de acesso: " +
                                "<b>" + senha + "</b> " +
                                "<br/>" +
                                "<br/>" +
                                "Caso não tenha solicitado a recuperação de senha e/ou tenha outro tipo de dificuldade, entre em contato com a equipe e-GULA através dos seguintes canais:" +
                                "<br/>" +
                                "E-mail para central@egula.com.br" +
                                "<br/>" +
                                "WhatsApp para (47) 988755567" +
                                "<br/>" +
                                "<br/>" +
                                "Atenciosamente, Serviço e-GULA.";

                        String sbPlaiText = "Olá, " +
                                nome + "! " +
                                "\n\r" +
                                "Recebemos a sua solicitação de recuperação de senha. " +
                                "\n\r" +
                                "\n\r" +
                                "Sua senha de acesso: " +
                                senha +
                                "\n\r" +
                                "\n\r" +
                                "Caso não tenha solicitado a recuperação de senha e/ou tenha outro tipo de dificuldade, entre em contato com a equipe e-GULA através dos seguintes canais:" +
                                "\n\r" +
                                "E-mail para central@egula.com.br" +
                                "\n\r" +
                                "WhatsApp para (47) 988755567" +
                                "\n\r" +
                                "\n\r" +
                                "Atenciosamente, Serviço e-GULA.";

                        ///

                        CSPEmailClientSender emailSender = new CSPEmailClientSender("mail.egula.com.br", 587, "central@egula.com.br", "Central e-GULA", "cri$$@2001");
                        emailSender.setSubject("Recuperação de senha e-GULA");
                        emailSender.addAddress(email);
                        emailSender.setContent(sbHtml, sbPlaiText);
                        emailSender.send();

                        executor.shutdown();
                    },0, 1);

                    writeResponse(httpExchange, 200, "OK");

                } else {
                    // Escreve a resposta no output avisando que houve algum problema interno.
                    writeResponse(httpExchange, 500, "Internal Server Error");
                }
            }

        } catch (SQLException e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }
}
