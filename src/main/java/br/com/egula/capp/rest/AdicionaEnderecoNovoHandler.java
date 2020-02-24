/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangJson;
import br.com.egula.capp.classes.*;
import br.com.egula.capp.model.Dispositivo;
import br.com.egula.capp.model.Distancia;
import br.com.egula.capp.model.Endereco;
import com.google.maps.GeoApiContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import static br.com.egula.capp.ModuloRest.writeResponse;
import static br.com.egula.capp.classes.CLSCapp.KEY_GOOGLE_API;
import static br.com.egula.capp.classes.CLSUtilidades.retornaDistanciaEnderecoLoja;

/**
 * Classe responsável por cadastrar novos endereços para usuários do Egula.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class AdicionaEnderecoNovoHandler implements HttpHandler {
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
                CSPLog.info(AdicionaEnderecoNovoHandler.class, "adiciona-endereco-novo(" + hostAddress + "): " + sb.toString());

                JSONObject input = new JSONObject(sb.toString());
                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                int idEndereco;
                int idLoja;

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

                idLoja = CSPUtilidadesLangJson.getFromJson(input, "idLoja", 0);

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
                    endereco.setId(idEndereco);

                    int idUsuario = CSPUtilidadesLangJson.getFromJson(input, "usuario_id", 0);

                    Distancia distancia = retornaDistanciaEnderecoLoja(idLoja, endereco, conn);

                    HashMap<String, Object> dados = new HashMap<>();
                    dados.put("ENDERECO_ID", idEndereco);
                    dados.put("USUARIO_ID", idUsuario);

                    if (!conn.exists("USUARIO_ENDERECO", "ENDERECO_ID = ? AND USUARIO_ID = ?", idEndereco, idUsuario)) {
                        conn.insertComposto("USUARIO_ENDERECO", dados);
                    }

                    JSONObject resposta = new JSONObject();

                    resposta.put("idDistancia",distancia.getId());
                    resposta.put("enderecoDestino",distancia.getEnderecoDestinoID());
                    resposta.put("enderecoOrigem",distancia.getEnderecoOrigemID());
                    resposta.put("distancia",distancia.getDistancia());
                    resposta.put("tempo",distancia.getTempo());

//                    if(!conn.getConfs().getHost().equals(CLSUtilidadesServidores.getHostPrincipal())) {
//                        CLSPendencias.addPendencia(input, "adiciona-endereco-novo", hostAddress);
//                    }

                    // Confirma a criação e envia o ID do endereço gerado
                    writeResponse(httpExchange, 200, String.valueOf(resposta));
                }
            }
        } catch (Exception e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }

    /**
     * Processa um novo endereço de usuário pendente no servidor secundário.
     *
     * @param input
     * @param remoteAddress
     * @param conn
     * @throws SQLException
     */
    public static void processaEnderecoNovoPendente (JSONObject input, String remoteAddress, CSPInstrucoesSQLBase conn) throws Exception {
        CSPLog.info(AdicionaUsuarioHandler.class, "Processando pendência de novo endereço de usuário: " + input.toString());

        int idEndereco;
        int idLoja;

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

        idLoja = CSPUtilidadesLangJson.getFromJson(input, "idLoja", 0);

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
            endereco.setId(idEndereco);

            String cpf = input.getString("usuarioCpf");

            ResultSet result = conn.select("SELECT a.ID FROM USUARIO a WHERE a.CPF = ?", cpf);

            Long idUsuario = null;

            if (result.next()) {
                idUsuario = result.getLong("ID");
            }

            Distancia distancia = retornaDistanciaEnderecoLoja(idLoja, endereco, conn);

            HashMap<String, Object> dados = new HashMap<>();
            dados.put("ENDERECO_ID", idEndereco);
            dados.put("USUARIO_ID", idUsuario);

            if (!conn.exists("USUARIO_ENDERECO", "ENDERECO_ID = ? AND USUARIO_ID = ?", idEndereco, idUsuario)) {
                conn.insertComposto("USUARIO_ENDERECO", dados);
            }

            JSONObject resposta = new JSONObject();

            resposta.put("idDistancia",distancia.getId());
            resposta.put("enderecoDestino",distancia.getEnderecoDestinoID());
            resposta.put("enderecoOrigem",distancia.getEnderecoOrigemID());
            resposta.put("distancia",distancia.getDistancia());
            resposta.put("tempo",distancia.getTempo());
        }

        CSPLog.info(AdicionaUsuarioHandler.class, "Pendência de novo endereço de usuário processada!");
    }
}
