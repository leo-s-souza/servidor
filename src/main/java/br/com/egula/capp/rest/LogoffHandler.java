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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import static br.com.egula.capp.ModuloRest.writeResponse;
import static br.com.egula.capp.classes.CLSUtilidades.retornaDistanciaEnderecoLoja;

/**
 * Classe responsável por realizar o logoff de usuários do Egula.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class LogoffHandler implements HttpHandler {
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
                CSPLog.info(LogoffHandler.class, "logoff(" + hostAddress + "): " + sb.toString());

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject content = new JSONObject(sb.toString());

                Dispositivo dev = Dispositivo.getDispositivo(content.getString("hardware_id"), conn);


                if (dev != null) {
                    int userId = content.getInt("usuario_id");
                    long devId = dev.getId();

                    conn.execute("update DISPOSITIVO d set d.FORCE_LOGOFF_PENDENTE = 0 where d.id = ?", devId);
                    conn.execute(
                            "EXECUTE PROCEDURE PR_REGISTRA_LOGADO (?, ?, ?, 0);",
                            userId,
                            devId,
                            hostAddress
                    );

//                    if(!conn.getConfs().getHost().equals(CLSUtilidadesServidores.getHostPrincipal())) {
//                        CLSPendencias.addPendencia(content, "logoff", hostAddress);
//                    }

                    writeResponse(httpExchange, 200, "OK");


                } else {
                    // Escreve a resposta no output avisando que houve algum problema interno.
                    writeResponse(httpExchange, 500, "Internal Server Error");
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
    public static void processaLogoffPendente (JSONObject input, String remoteAddress, CSPInstrucoesSQLBase conn) throws Exception {
        CSPLog.info(AdicionaUsuarioHandler.class, "Processando pendência de logoff de usuário: " + input.toString());

        Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);
        System.out.println("____________2____________");

        String cpf = input.getString("usuarioCpf");
        System.out.println("____________3____________");

        ResultSet result = conn.select("SELECT a.ID FROM USUARIO a WHERE a.CPF = ?", cpf);
        System.out.println("____________4____________");

        Long idUsuario = null;

        if (result.next()) {
            idUsuario = result.getLong("ID");
        }

        long devId = dev.getId();

        conn.execute("update DISPOSITIVO d set d.FORCE_LOGOFF_PENDENTE = 0 where d.id = ?", devId);
        conn.execute(
                "EXECUTE PROCEDURE PR_REGISTRA_LOGADO (?, ?, ?, 0);",
                idUsuario,
                devId,
                remoteAddress
        );

        CSPLog.info(AdicionaUsuarioHandler.class, "Pendência de logoff de usuário processada!");
    }
}
