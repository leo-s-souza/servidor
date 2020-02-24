/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.model.Dispositivo;
import br.com.egula.capp.model.Horario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static br.com.egula.capp.ModuloRest.writeResponse;

public class RequisitaHorariosLojaHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        try {

            StringBuilder stringBuilder = new StringBuilder();

            // Lê o conteúdo vindo no POST.
            {
                String inputLine;

                BufferedReader in = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()));
                while ((inputLine = in.readLine()) != null) {
                    stringBuilder.append(inputLine);
                }
                in.close();
            }

            if (stringBuilder.length() > 0 && stringBuilder.toString().trim().length() > 0) {
                String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();
                CSPLog.info(PagamentoHandler.class, "Requisita Horarios - (" + hostAddress + "): " + stringBuilder.toString());

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject input = new JSONObject(stringBuilder.toString());
                Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                JSONArray horarios = new JSONArray();

                ResultSet resultSet = conn.select((StringBuilder sb) -> {
                    sb.append("SELECT ");
                    sb.append("    a.ID, ");
                    sb.append("    a.DIA_SEMANA, ");
                    sb.append("    a.HORA_INICIO, ");
                    sb.append("    a.HORA_FIM, ");
                    sb.append("    a.VALOR_ACRESCIMO, ");
                    sb.append("    a.TIPO_ATENDIMENTO ");
                    sb.append("FROM ");
                    sb.append("    HORARIO_ATENDIMENTO a ");
                    sb.append("WHERE ");
                    sb.append("     a.LOJA_ID = ? ");
                    sb.append("     AND a.APP_STATUS <> 0;");
                }, input.getInt("lojaId"));

                while (resultSet.next()) {

                    horarios.put(new JSONObject() {
                        {
                            put("horarioID", resultSet.getInt("ID"));
                            put("diaSemana", resultSet.getInt("DIA_SEMANA"));
                            put("horaInicio", resultSet.getTime("HORA_INICIO"));
                            put("horaFim", resultSet.getTime("HORA_FIM"));
                            put("valorAcrescimo", resultSet.getDouble("VALOR_ACRESCIMO"));
                            put("tipoAtendimento", resultSet.getInt("TIPO_ATENDIMENTO"));
                        }
                    });
                }

                if (horarios.length() != 0) {
                    // Confirma o login e envia os dados do usuário
                    writeResponse(httpExchange, 200, horarios.toString());
                } else {
                    // Escreve a resposta no output avisando que houve algum problema interno.
                    writeResponse(httpExchange, 401, "Sem registros de horários dessa loja");
                }

            } else {
                // Escreve a resposta no output avisando que houve algum problema interno.
                writeResponse(httpExchange, 400, "Conteúdo inexistente");
            }
        } catch (Exception e) {
            CLSException.register(e);
            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }
}