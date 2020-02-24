/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangJson;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.model.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.StringJoiner;

import static br.com.egula.capp.ModuloRest.writeResponse;

public class ProcuraProdutosHandler implements HttpHandler {
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

            String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();
            CSPLog.info(PagamentoHandler.class, "Procura Produtos - (" + hostAddress + "): " + stringBuilder.toString());

            CSPInstrucoesSQLBase conn = CLSCapp.connDb();
            JSONObject input = new JSONObject(stringBuilder.toString());


            int lojaId = 0;
            int grupoId = 0;

            if(input.has("lojaId")){
                lojaId = CSPUtilidadesLangJson.getFromJson(input, "lojaId", 0);
            }

            if(input.has("grupoId")){
                grupoId = CSPUtilidadesLangJson.getFromJson(input, "grupoId", 0);
            }

            int finalLojaId = lojaId;
            int finalGrupoId = grupoId;
            ResultSet rs = conn.select((StringBuilder sb) -> {
                sb.append("SELECT ");
                sb.append("    a.PRODUTO_ID, ");
                sb.append("    a.GRUPO_ID, ");
                sb.append("    a.ARQUIVO_DISPONIVEL_ID, ");
                sb.append("    a.LOJA_ID, ");
                sb.append("    a.LOJA_NOME, ");
                sb.append("    a.DESCRICAO, ");
                sb.append("    a.PRECO_VENDA, ");
                sb.append("    a.IMAGEM, ");
                sb.append("    a.GRUPOS ");
                sb.append("FROM ");
                sb.append("    VW_PESQUISA_PRODUTOS a ");
                sb.append("WHERE ");
                sb.append("    a.PRODUTO_ID is not null ");
                if(finalLojaId > 0){
                    sb.append(" and ");
                    sb.append(" a.LOJA_ID = ").append(finalLojaId);
                }
                if(finalGrupoId > 0){
                    sb.append(" and ");
                    sb.append(" (a.GRUPO_ID = ").append(finalGrupoId);
                    sb.append(" or ");
                    sb.append(" a.grupos like '%").append(finalGrupoId).append("%') ");
                }
                sb.append(" and ");
                sb.append(montaWherePesquisaProdutos(input.getString("pesquisaProd")));
            });

            JSONArray respostaArray = new JSONArray();

            while (rs.next()) {

                respostaArray.put(new JSONObject() {
                    {
                        put("prodId", rs.getInt("PRODUTO_ID"));
                        put("lojaId", rs.getInt("LOJA_ID"));
                        put("lojaNome", rs.getString("LOJA_NOME"));
                        put("descricao", rs.getString("DESCRICAO"));
                        put("imagem", rs.getString("IMAGEM"));
                        put("precoVenda", rs.getDouble("PRECO_VENDA"));
                        put("grupoProduto", rs.getInt("GRUPO_ID"));
                        put("listaGrupos", rs.getString("GRUPOS"));
                    }
                });
            }

            writeResponse(httpExchange, 200, respostaArray.toString());
        } catch (Exception ex) {
            CLSException.register(ex);
            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }

    private String montaWherePesquisaProdutos(String pesquisa) {

        String[] arrPesquisa = pesquisa.split(" ");

        StringJoiner sj = new StringJoiner(" and ");

        for (String s : arrPesquisa) {
            sj.add(" LOWER(DESCRICAO) like LOWER('%" + s + "%')");
        }

        return sj.toString();
    }
}
