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
import br.com.egula.capp.model.Endereco;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.HashMap;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Classe responsável por cadastrar novos endereços para usuários do Egula.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class AdicionaEnderecoHandler implements HttpHandler {
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
                CSPLog.info(AdicionaEnderecoHandler.class, "adiciona-endereco(" + hostAddress + "): " + sb.toString());

                JSONObject input = new JSONObject(sb.toString());
                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                int idEndereco;

                Endereco endereco = new Endereco(
                        0,
                        CSPUtilidadesLangJson.getFromJson(input, "logradouro", ""),
                        CSPUtilidadesLangJson.getFromJson(input, "numero", ""),
                        CSPUtilidadesLangJson.getFromJson(input, "complemento", (String) null),
                        CSPUtilidadesLangJson.getFromJson(input, "referencia", (String) null),
                        CSPUtilidadesLangJson.getFromJson(input, "bairro", ""),
                        CSPUtilidadesLangJson.getFromJson(input, "cidade", ""),
                        CSPUtilidadesLangJson.getFromJson(input, "estado", ""),
                        CSPUtilidadesLangJson.getFromJson(input, "pais", ""),
                        CSPUtilidadesLangJson.getFromJson(input, "cep", (String) null),
                        CSPUtilidadesLangJson.getFromJson(input, "latitude", 0.0),
                        CSPUtilidadesLangJson.getFromJson(input, "longitude", 0.0)
                );

                if (endereco.getLatitude() == 0 || endereco.getLongitude() == 0) {
                    endereco = CLSUtilidades.getLatLngEndereco(endereco);
                }

                ResultSet rs = conn.select((StringBuilder s) -> {
                            s.append(" SELECT ID FROM PR_REGISTRA_ENDERECO( ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("	?, ");
                            s.append("  ?, ");
                            s.append("	? ");
                            s.append("); ");
                        },
                        endereco.getLogradouro(),
                        endereco.getNumero(),
                        endereco.getComplemento(),
                        endereco.getReferencia(),
                        endereco.getCep(),
                        endereco.getLatitude(),
                        endereco.getLongitude(),
                        endereco.getBairro(),
                        null,
                        endereco.getCidade(),
                        null,
                        endereco.getEstado(),
                        null,
                        endereco.getPais(),
                        null
                );

                if (rs.next()) {
                    idEndereco = rs.getInt("ID");

                    int idUsuario = CSPUtilidadesLangJson.getFromJson(input, "usuario_id", 0);

                    HashMap<String, Object> dados = new HashMap<>();
                    dados.put("ENDERECO_ID", idEndereco);
                    dados.put("USUARIO_ID", idUsuario);

                    if (!conn.exists("USUARIO_ENDERECO", "ENDERECO_ID = ? AND USUARIO_ID = ?", idEndereco, idUsuario)) {
                        conn.insertComposto("USUARIO_ENDERECO", dados);
                    }

                    // Confirma a criação e envia o ID do endereço gerado
                    writeResponse(httpExchange, 200, String.valueOf(idEndereco));
                }
            }
        } catch (Exception e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }
}
