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
import br.com.egula.capp.model.Distancia;
import br.com.egula.capp.model.Endereco;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.HashMap;

import static br.com.egula.capp.ModuloRest.writeResponse;
import static br.com.egula.capp.classes.CLSUtilidades.retornaDistanciaEnderecoLoja;

public class EnderecoDistanciaHandler implements HttpHandler {

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
                CSPLog.info(EnderecoDistanciaHandler.class, "calcula-distancias-enderecos(" + hostAddress + "): " + sb.toString());

                JSONObject input = new JSONObject(sb.toString());
                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                int idLoja = CSPUtilidadesLangJson.getFromJson(input, "loja", 0);
                int idUsuario = CSPUtilidadesLangJson.getFromJson(input, "usuario", 0);
                JSONArray enderecosIds = CSPUtilidadesLangJson.getFromJson(input, "enderecosIds", new JSONArray());

                ResultSet rs = conn.selectOneRow((StringBuilder s) -> {
                    s.append("SELECT ");
                    s.append("    a.ENDERECO_ID  ");
                    s.append("FROM  ");
                    s.append("    LOJA_ENDERECO a  ");
                    s.append("where ");
                    s.append("    a.LOJA_ID = ? ");
                    s.append("and ");
                    s.append("    a.STATUS_ENDERECO = 1 ");
                }, idLoja);

                int idEnderecoLoja = rs.getInt("ENDERECO_ID");

                JSONObject resposta = new JSONObject();

                for (Object obj : enderecosIds) {
                    int idEndereco = (int) obj;

                    rs = conn.selectOneRow((StringBuilder s) -> {
                        s.append("  SELECT ");
                        s.append("      v.ID, ");
                        s.append("      v.LOGRADOURO, ");
                        s.append("      v.NUMERO, ");
                        s.append("      v.COMPLEMENTO, ");
                        s.append("      v.REFERENCIA, ");
                        s.append("      v.CEP,");
                        s.append("      v.LATITUDE, ");
                        s.append("      v.LONGITUDE, ");
                        s.append("      v.BAIRRO, ");
                        s.append("      v.BAIRRO_ID, ");
                        s.append("      v.CIDADE, ");
                        s.append("      v.CIDADE_ID,");
                        s.append("      v.ESTADO, ");
                        s.append("      v.ESTADO_ID, ");
                        s.append("      v.PAIS, ");
                        s.append("      v.PAIS_ID ");
                        s.append("  FROM ");
                        s.append("      VW_INFOS_ENDERECO v ");
                        s.append("  WHERE ");
                        s.append("      v.ID = ? ");
                    }, idEndereco);

                    Endereco endereco = new Endereco(
                            rs.getInt("ID"),
                            rs.getString("LOGRADOURO"),
                            rs.getString("NUMERO"),
                            rs.getString("COMPLEMENTO"),
                            rs.getString("REFERENCIA"),
                            rs.getString("BAIRRO"),
                            rs.getString("CIDADE"),
                            rs.getString("ESTADO"),
                            rs.getString("PAIS"),
                            rs.getString("CEP"),
                            rs.getDouble("LATITUDE"),
                            rs.getDouble("LONGITUDE")
                    );

                    rs = conn.select((StringBuilder s) -> {
                        s.append(" SELECT ");
                        s.append("     a.ID, ");
                        s.append("     a.DESTINO_ENDERECO_ID, ");
                        s.append("     a.ORIGEM_ENDERECO_ID, ");
                        s.append("     a.DISTANCIA, ");
                        s.append("     a.TEMPO");
                        s.append(" FROM ");
                        s.append("     DISTANCIA_ENDERECO a");
                        s.append(" where");
                        s.append("     a.DESTINO_ENDERECO_ID = ?");
                        s.append(" and");
                        s.append("     a.ORIGEM_ENDERECO_ID = ?");

                    }, idEndereco, idEnderecoLoja);

                    if (rs.next()) {
                        JSONObject end = new JSONObject();

                        if(rs.getDouble("DISTANCIA") == 0){

                            conn.deleteComposto("DISTANCIA_ENDERECO", "DESTINO_ENDERECO_ID = ? AND ORIGEM_ENDERECO_ID = ?", idEndereco, idEnderecoLoja);

                            Distancia distancia = calculaDistancia(endereco, idEndereco, idLoja, conn);

                            end.put("idDistancia",distancia.getId());
                            end.put("enderecoDestino",distancia.getEnderecoDestinoID());
                            end.put("enderecoOrigem",distancia.getEnderecoOrigemID());
                            end.put("distancia",distancia.getDistancia());
                            end.put("tempo",distancia.getTempo());

                        } else {
                            end.put("idDistancia",rs.getInt("ID"));
                            end.put("enderecoDestino",rs.getInt("DESTINO_ENDERECO_ID"));
                            end.put("enderecoOrigem",rs.getInt("ORIGEM_ENDERECO_ID"));
                            end.put("distancia",rs.getDouble("DISTANCIA"));
                            end.put("tempo",rs.getLong("TEMPO"));
                        }

                        resposta.put(String.valueOf(idEndereco), end);
                    } else {

                        Distancia distancia = calculaDistancia(endereco, idEndereco, idLoja, conn);

                        HashMap<String, Object> dados = new HashMap<>();
                        dados.put("ENDERECO_ID", idEndereco);
                        dados.put("USUARIO_ID", idUsuario);

                        if (!conn.exists("USUARIO_ENDERECO", "ENDERECO_ID = ? AND USUARIO_ID = ?", idEndereco, idUsuario)) {
                            conn.insertComposto("USUARIO_ENDERECO", dados);
                        }

                        JSONObject end = new JSONObject();

                        end.put("idDistancia",distancia.getId());
                        end.put("enderecoDestino",distancia.getEnderecoDestinoID());
                        end.put("enderecoOrigem",distancia.getEnderecoOrigemID());
                        end.put("distancia",distancia.getDistancia());
                        end.put("tempo",distancia.getTempo());

                        resposta.put(String.valueOf(idEndereco), end);
                    }

                }
                writeResponse(httpExchange, 200, String.valueOf(resposta));
            }

        } catch (Exception e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }

    private Distancia calculaDistancia(Endereco endereco, int idEndereco, int idLoja, CSPInstrucoesSQLBase conn) throws Exception {

        if(endereco.getLatitude() == 0 || endereco.getLongitude() == 0){
            endereco = CLSUtilidades.getLatLngEndereco(endereco);
            endereco.setId(idEndereco);
        }

        return retornaDistanciaEnderecoLoja(idLoja, endereco, conn);
    }
}
