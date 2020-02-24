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

import java.io.IOException;
import java.sql.SQLException;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Classe responsável por alterar os dados cadastrais dos usuários do Egula.
 *
 * @author Matheus Felipe Amelco
 * @since 1.4
 */
public class AlteraUsuarioHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();

            CSPLog.info(AlteraUsuarioHandler.class, "altera-usuario(" + hostAddress + ")");

            String requisicao = ModuloRest.readPostContent(httpExchange);

            if (!requisicao.trim().isEmpty()) {
                JSONObject json = new JSONObject(requisicao);
                CSPInstrucoesSQLBase conn = CLSCapp.connDb();

                int usuarioId = json.has("id") ? json.getInt("id") : 0;
                String usuarioNome = json.has("nome") ? json.getString("nome") : "";
                String usuarioSobrenome = json.has("sobrenome") ? json.getString("sobrenome") : "";
                String usuarioEmail = json.has("email") ? json.getString("email") : "";
                String usuarioTelefone = json.has("telefone") ? json.getString("telefone") : "";

                if (usuarioId > 0) {
                    conn.execute(
                            "EXECUTE PROCEDURE PR_ALTERA_USUARIO(?, ?, ?, ?, ?);",
                            usuarioId,
                            usuarioNome,
                            usuarioSobrenome,
                            usuarioEmail,
                            usuarioTelefone
                    );
                }

                // Confirma a criação e envia o ID do endereço gerado
                writeResponse(httpExchange, 200, String.valueOf(usuarioId));
            } else {
                writeResponse(httpExchange, 403, "Proibido");
            }

        } catch (IOException | SQLException e) {
            CLSException.register(e);
            e.printStackTrace();

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }
}
