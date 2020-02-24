/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLang;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangJson;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.classes.CLSPendencias;
import br.com.egula.capp.classes.CLSUtilidadesServidores;
import br.com.egula.capp.model.Dispositivo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Classe responsável por cadastrar novos usuários do Egula.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class AdicionaUsuarioHandler implements HttpHandler {


    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();

            CSPLog.info(AdicionaUsuarioHandler.class, "adiciona-usuario(" + hostAddress + ")");

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

                JSONObject input = new JSONObject(sb.toString());

                CSPLog.info(AdicionaUsuarioHandler.class, input.toString());

                final CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                final String cpf = input.getString("cpf").replaceAll("[^0-9]", "").trim();
                final String email = input.getString("email").trim();
                final String facebookId = CSPUtilidadesLangJson.getFromJson(input, "facebook_id", (String) null);

                if (!CSPUtilidadesLang.isCpfValid(cpf) || !CSPUtilidadesLang.isEmailValid(email)) {
                    writeResponse(httpExchange, 400, "CPF ou Email inválido");
                    return;
                }

                //Usuário já cadastrado
                final ResultSet exists = conn.selectOneRow((StringBuilder s) -> {
                    s.append("SELECT ");
                    s.append("    a.ID, ");
                    s.append("    a.EMAIL, ");
                    s.append("    a.FACEBOOK_ID ");
                    s.append("FROM ");
                    s.append("    USUARIO a ");
                    s.append("WHERE ");
                    s.append("    a.CPF = ? or ");
                    s.append("    a.EMAIL = ? ");
                }, cpf, email);

                if (exists != null) {
                    JSONObject js = new JSONObject();
                    js.put("id", exists.getInt("ID"));
                    js.put("email", exists.getString("EMAIL"));

                    if(facebookId == null){
                        writeResponse(httpExchange, 409, js.toString());
                        return;
                    } else if(exists.getString("FACEBOOK_ID") != null && exists.getString("FACEBOOK_ID").equals(facebookId)){
                        writeResponse(httpExchange, 409, js.toString());
                        return;
                    }

                }

                final String nome = input.getString("nome").trim();
                final String sobrenome = input.getString("sobrenome").trim();
                final String telefone = input.getString("telefone");
                String senha = CSPUtilidadesLangJson.getFromJson(input, "senha", (String) null);

                senha = (senha != null) ? senha.trim() : senha;

                Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                if (dev != null) {
                    conn.execute("EXECUTE PROCEDURE PR_REGISTRA_USUARIO_NOVO (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            dev.getId(),
                            hostAddress,
                            facebookId,
                            cpf,
                            nome,
                            sobrenome,
                            email,
                            telefone,
                            senha
                    );
                }

//                if(!conn.getConfs().getHost().equals(CLSUtilidadesServidores.getHostPrincipal())) {
//                    CLSPendencias.addPendencia(input, "adiciona-usuario", hostAddress);
//                }

                //Confirma a criação da conta!
                writeResponse(httpExchange, 200, "OK");
            }

        } catch (Exception e) {
            CLSException.register(e);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");

        }
    }

    /**
     * Processa um cadastro de usuário pendente no servidor secundário.
     *
     * @param input
     * @param remoteAddress
     * @param conn
     * @throws SQLException
     */
    public static void processaAdicionaUsuarioPendente (JSONObject input, String remoteAddress, CSPInstrucoesSQLBase conn) throws SQLException {
        CSPLog.info(AdicionaUsuarioHandler.class, "Processando pendência de cadastro de usuário: " + input.toString());

        final String cpf = input.getString("cpf").replaceAll("[^0-9]", "").trim();
        final String email = input.getString("email").trim();
        final String facebookId = CSPUtilidadesLangJson.getFromJson(input, "facebook_id", (String) null);

        //Usuário já cadastrado
        final ResultSet exists = conn.selectOneRow((StringBuilder s) -> {
            s.append("SELECT ");
            s.append("    a.ID, ");
            s.append("    a.EMAIL, ");
            s.append("    a.FACEBOOK_ID ");
            s.append("FROM ");
            s.append("    USUARIO a ");
            s.append("WHERE ");
            s.append("    a.CPF = ? or ");
            s.append("    a.EMAIL = ? ");
        }, cpf, email);

        if (exists != null) {
            JSONObject js = new JSONObject();
            js.put("id", exists.getInt("ID"));
            js.put("email", exists.getString("EMAIL"));
        }

        final String nome = input.getString("nome").trim();
        final String sobrenome = input.getString("sobrenome").trim();
        final String telefone = input.getString("telefone");
        String senha = CSPUtilidadesLangJson.getFromJson(input, "senha", (String) null);

        senha = (senha != null) ? senha.trim() : senha;

        Dispositivo dev = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

        if (dev != null) {
            conn.execute("EXECUTE PROCEDURE PR_REGISTRA_USUARIO_NOVO (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    dev.getId(),
                    remoteAddress,
                    facebookId,
                    cpf,
                    nome,
                    sobrenome,
                    email,
                    telefone,
                    senha
            );
        }

        CSPLog.info(AdicionaUsuarioHandler.class, "Pendência de cadastro de usuário processada!");
    }
}
