/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.arquivos.CSPArquivos;
import br.com.egula.capp.classes.CLSException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.util.Map;

import static br.com.egula.capp.ModuloRest.queryToMap;
import static br.com.egula.capp.ModuloRest.writeResponse;
import static br.com.egula.capp.classes.CLSUtilidades.recebeArquivo;

public class NovaImagemHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) {

        try {

            final Map<String, String> params = queryToMap(httpExchange.getRequestURI().getQuery());
            String caminho = params.get("caminho");
            String arqNome = params.get("arquivo_nome");

            if(!caminho.endsWith("/") || !caminho.endsWith("\\")){
                caminho = caminho + "/";
            }

            CSPArquivos arq = recebeArquivo(httpExchange, arqNome, caminho);
            if (arq != null) {
                writeResponse(httpExchange, 200, "Arquivo recebido e gravado com sucesso.");
            } else {
                writeResponse(httpExchange, 500, "Problema ao gravar o arquivo.");
            }
        } catch (Exception e) {
            CLSException.register(e);
            writeResponse(httpExchange, 500, "Problema ao gravar o arquivo.");
        }


    }
}
