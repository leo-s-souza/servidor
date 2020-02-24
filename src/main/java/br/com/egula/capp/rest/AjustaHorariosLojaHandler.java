/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.classes.CLSUtilidades;
import br.com.egula.capp.model.Dispositivo;
import br.com.egula.capp.model.Horario;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

import static br.com.egula.capp.ModuloRest.writeResponse;

public class AjustaHorariosLojaHandler implements HttpHandler {

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
                CSPLog.info(PagamentoHandler.class, "Ajusta Horarios - (" + hostAddress + "): " + stringBuilder.toString());

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject input = new JSONObject(stringBuilder.toString());
                Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                int lojaId = input.getInt("lojaId");

                SimpleDateFormat formatHora = new SimpleDateFormat("HH:mm:ss");
                SimpleDateFormat formatData = new SimpleDateFormat("dd.MM.yyyy");

                Horario horario = new Horario(
                        input.getInt("horarioId"),
                        input.getInt("diaSemana"),
                        formatHora.parse(input.getString("horaInicio")),
                        formatHora.parse(input.getString("horaFim")),
                        input.getDouble("valor"),
                        input.getInt("tipo"),
                        (!input.getString("dataExata").isEmpty()) ? formatData.parse(input.getString("dataExata")) : null
                );

                conn.execute((StringBuilder s) -> {
                            s.append(" EXECUTE PROCEDURE PR_REGISTRA_HORARIO_ATENDIMENTO( ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	? ");
                            s.append("); ");
                        },
                        horario.getId(),
                        lojaId,
                        horario.getDiaSemana(),
                        horario.getDataExata(),
                        horario.getHoraInicio(),
                        horario.getHoraFim(),
                        horario.getValor(),
                        1,
                        horario.getTipo()
                );

                ArrayList<HashMap<String, Object>> horarios = new ArrayList<>();

                ResultSet resultSet = conn.select((StringBuilder sb) -> {
                    sb.append("SELECT ");
                    sb.append("    a.ID, ");
                    sb.append("    a.DIA_SEMANA, ");
                    sb.append("    a.HORA_INICIO, ");
                    sb.append("    a.HORA_FIM, ");
                    sb.append("    a.VALOR_ACRESCIMO, ");
                    sb.append("    a.TIPO_ATENDIMENTO, ");
                    sb.append("    a.DATA_EXATA ");
                    sb.append("FROM ");
                    sb.append("    HORARIO_ATENDIMENTO a ");
                    sb.append("WHERE ");
                    sb.append("     a.LOJA_ID = ? ");
                    sb.append("AND ");
                    sb.append("     a.APP_STATUS = 1 ");
                }, lojaId);

                while (resultSet.next()) {
                    horarios.add(
                            new HashMap<String, Object>() {
                                {
                                    put("id", resultSet.getInt("ID"));
                                    put("diaSemana", resultSet.getInt("DIA_SEMANA"));
                                    put("horaInicio", resultSet.getTime("HORA_INICIO"));
                                    put("horaFim", resultSet.getTime("HORA_FIM"));
                                    put("valor", resultSet.getDouble("VALOR_ACRESCIMO"));
                                    put("tipo", resultSet.getInt("TIPO_ATENDIMENTO"));
                                    put("dataExata", resultSet.getDate("DATA_EXATA"));
                                }
                            }
                    );
                }

                if (horarios.size() != 0) {

                    CLSUtilidades.atualizaHorariosLojaFirebird(lojaId, horarios);

                    // Confirma o login e envia os dados do usuário
                    writeResponse(httpExchange, 200, "horario cadastrado com sucesso.");
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