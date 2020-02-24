/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.egula.capp.ModuloRest;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Classe responsável por remover endereços dos usuários do Egula. Na realidade não deletamos eles da base,
 * somente alteramos uma coluna que faz com que não sejam mais disponibilizados aos usuários.
 *
 * @author Matheus Felipe Amelco
 * @since 1.4
 */
public class RemoveEnderecoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();

            CSPLog.info(RemoveEnderecoHandler.class, "adiciona-endereco(" + hostAddress + ")");

            String requisicao = ModuloRest.readPostContent(httpExchange);

            if (!requisicao.trim().isEmpty()) {
                JSONObject json = new JSONObject(requisicao);
                CSPInstrucoesSQLBase conn = CLSCapp.connDb();

                int idEndereco = json.getInt("endereco_id");
                int usuarioId = json.getInt("usuario_id");

                conn.execute(
                        "UPDATE USUARIO_ENDERECO a SET a.APP_STATUS = 0 WHERE a.ENDERECO_ID = ? AND a.USUARIO_ID = ?;",
                        idEndereco,
                        usuarioId
                );

                // Confirma a criação e envia o ID do endereço gerado
                writeResponse(httpExchange, 200, String.valueOf(idEndereco));
            } else {
                writeResponse(httpExchange, 403, "Proibido");
            }


        } catch (Exception e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }
}
