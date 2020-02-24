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
import br.com.egula.capp.model.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;

import static br.com.egula.capp.ModuloRest.writeResponse;

public class PagamentoHandler implements HttpHandler {

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
                CSPLog.info(PagamentoHandler.class, "Pagamento(" + hostAddress + "): " + sb.toString());

                CSPInstrucoesSQLBase conn = CLSCapp.connDb();
                JSONObject input = new JSONObject(sb.toString());

                JSONObject resposta = new JSONObject();

                String usuarioCpf = CSPUtilidadesLangJson.getFromJson(input, "usuario_cpf", "");
                int lojaId = CSPUtilidadesLangJson.getFromJson(input, "loja_id", 0);

                int usuarioId = Usuario.getIdUsuarioCpf(usuarioCpf, conn);

                double valorPedido = 0;

                if(input.has("valorPedido")){
                    valorPedido = CSPUtilidadesLangJson.getFromJson(input, "valorPedido", 0);
                }

                double bonusLivre = conn.selectOneRow("SELECT p.TOTAL_BONUS_LIVRE FROM PR_RETORNA_BONUS_USUARIO (?) p",
                        usuarioId
                ).getDouble("TOTAL_BONUS_LIVRE");

                final ResultSet pagamentos = conn.select((StringBuilder s) -> {
                    s.append("SELECT ");
                    s.append("    a.ID, ");
                    s.append("    a.LOJA_ID, ");
                    s.append("    a.DESCRICAO, ");
                    s.append("    a.IS_COM_TROCO, ");
                    s.append("    a.APP_STATUS,");
                    s.append("    a.LAST_UPDATE, ");
                    s.append("    a.BASE_CONTRATANTE_ID, ");
                    s.append("    a.IS_BONUS ");
                    s.append("FROM ");
                    s.append("    FORMA_PAGAMENTO a ");
                    s.append("WHERE ");
                    s.append("    a.APP_STATUS <> 0 ");
                    s.append("AND ");
                    s.append("    a.LOJA_ID = ?");
                }, lojaId);

                JSONArray jarrayInfos = new JSONArray();

                boolean pagamentoBonusPossivel = valorPedido <= bonusLivre;

                while (pagamentos.next()) {

                    if(pagamentos.getInt("IS_BONUS") == 1 && !pagamentoBonusPossivel){
                        continue;
                    }

                    jarrayInfos.put(new JSONObject() {
                        {
                            put("pagamentoId", pagamentos.getInt("ID"));
                            put("lojaId", pagamentos.getInt("LOJA_ID"));
                            put("descricao", pagamentos.getString("DESCRICAO"));
                            put("isComTroco", pagamentos.getInt("IS_COM_TROCO"));
                            put("appStatus", pagamentos.getInt("APP_STATUS"));
                            put("isBonus", pagamentos.getInt("IS_BONUS"));
                        }
                    });
                }

                if (jarrayInfos.length() > 0) {
                    resposta.put("pagamentos", jarrayInfos);
                } else {
                    writeResponse(httpExchange, 400, "Não existe tipo de pagamento cadastrado para essa loja");
                }

                final ResultSet cuponsDesconto = conn.select((StringBuilder s) -> {
                    s.append("SELECT ");
                    s.append("    a.ID, ");
                    s.append("    a.LOJA_ID, ");
                    s.append("    a.USUARIO_ID, ");
                    s.append("    a.TIPO_CUPOM_DESCONTO_ID,");
                    s.append("    a.CHAVE_UTILIZACAO, ");
                    s.append("    a.VALOR_DESCONTO, ");
                    s.append("    a.IS_ATIVO,");
                    s.append("    (Select ");
                    s.append("        COUNT(0) ");
                    s.append("    from ");
                    s.append("        VW_CUPONS_DESCONTO_PEDIDO cp ");
                    s.append("    join ");
                    s.append("        PEDIDO p ON p.ID = cp.PEDIDO_ID ");
                    s.append("    where ");
                    s.append("        p.USUARIO_ENDERECO_USUARIO_ID = ? ");
                    s.append("    and ");
                    s.append("        cp.CUPOM_DESCONTO_ID = a.ID) AS CUPOM_UTILIZADO  ");
                    s.append("FROM  ");
                    s.append("    VW_CUPONS_DESCONTO_ATIVOS a  ");
                    s.append("where ");
                    s.append("    a.USUARIO_ID = ?  ");
                    s.append("OR  ");
                    s.append("    a.USUARIO_ID IS NULL  ");
                    s.append("OR  ");
                    s.append("    a.LOJA_ID = ?  ");
                    s.append("OR  ");
                    s.append("    a.LOJA_ID IS NULL  ");
                }, usuarioId, usuarioId, lojaId);

                jarrayInfos = new JSONArray();

                while (cuponsDesconto.next()) {
                    jarrayInfos.put(new JSONObject() {
                        {
                            put("cupomDescontoId", cuponsDesconto.getInt("ID"));
                            put("lojaId", (cuponsDesconto.getString("ID") != null) ? cuponsDesconto.getInt("LOJA_ID") : 0);
                            put("usuarioId", (cuponsDesconto.getString("ID") != null) ? cuponsDesconto.getInt("USUARIO_ID") : 0);
                            put("tipoCupomDescontoId", cuponsDesconto.getInt("TIPO_CUPOM_DESCONTO_ID"));
                            put("chaveUtilizacao", cuponsDesconto.getString("CHAVE_UTILIZACAO"));
                            put("valorDesconto", cuponsDesconto.getDouble("VALOR_DESCONTO"));
                            put("isAtivo", cuponsDesconto.getInt("IS_ATIVO"));
                            put("isUtilizavel", cuponsDesconto.getInt("CUPOM_UTILIZADO") == 0);
                        }
                    });
                }

                resposta.put("cuponsDesconto", jarrayInfos);

                if (!resposta.isNull("pagamentos")) {
                    // Confirma o login e envia os dados do usuário
                    writeResponse(httpExchange, 200, resposta.toString());
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
}
