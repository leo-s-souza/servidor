/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */


package br.com.egula.capp.rest.garcom;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.egula.capp.ModuloRest;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.model.Dispositivo;
import br.com.egula.capp.model.PedidoItem;
import br.com.egula.capp.model.garcom.PedidoGarcom;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static br.com.egula.capp.ModuloRest.writeResponse;

/**
 * Atende os pedidos efetuados e os registra na base.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class PedidoGarcomHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            // Pega o endereço de IP do dispositivo que está comunicando.
            String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();

            CSPLog.info(PedidoGarcomHandler.class, "efetua-pedido-garcom(" + hostAddress + ")");

            String sb = ModuloRest.readPostContent(httpExchange);

            if (sb.length() > 0 && sb.trim().length() > 0) {

                JSONObject input = new JSONObject(sb);
                final CSPInstrucoesSQLBase conn = CLSCapp.connDb();

                // Verificamos se existe o código do dispositivo na requisição. Se não existir, não podemos atender.
                if (!input.has("hardware_id") || input.isNull("hardware_id")) {
                    writeResponse(httpExchange, 403, "Proibido");
                    return;
                }

                // Registra o pedido.
                processaPedido(input, conn);

                // Escreve a resposta no output avisando que tudo ocorreu normalmente.
                writeResponse(httpExchange, 200, "OK");
            }
        } catch (Exception ex) {
            CLSException.register(ex);

            // Escreve a resposta no output avisando que houve algum problema interno.
            writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }

    /**
     * Registra um novo pedido na abse de dados.
     *
     * @param input JSONObject - Json da requisição
     * @param conn  CSPInstrucoesSQLBase - Conexão com a base de dados.
     */
    private void processaPedido(JSONObject input, CSPInstrucoesSQLBase conn) {
        try {
            Gson gson = new GsonBuilder().create();
            PedidoGarcom pedido = gson.fromJson(input.toString(), PedidoGarcom.class);

            Dispositivo dev = Dispositivo.getDispositivo(pedido.getDispositivoId(), conn);

            if (dev == null) {
                CSPLog.error(PedidoGarcomHandler.class, "DISPOSITIVO NÃO IDENTIFICADO! PEDIDO NÃO PODE SER SALVO");
                return;
            }

            final long idPedido;

            // Não precisamos informar nada aqui além das observações, pois existe uma trigger na base que preenche
            // os dados necessários quando não informamos.
            idPedido = conn.selectOneRow((StringBuilder sb) -> {
                sb.append("INSERT INTO PEDIDO_CLIENTE (OBSERVACOES) ");
                sb.append("VALUES (?) RETURNING ID; ");
            }, "").getLong("ID");
//
//            conn.execute((StringBuilder sb) -> {
//                sb.append("INSERT INTO PEDIDO_CLIENTE_GARCOM (PEDIDO_CLIENTE_ID, GARCOM_ID, MESA, COMANDA) ");
//                sb.append("VALUES (?, ?, ?, ?); ");
//            }, idPedido, pedido.getGarcomId(), pedido.getMesa(), pedido.getComanda());

            /*
              Os dados de comanda e mesa estão alterados de propósido devido a um problema que eu não entendi.
              NÃO ME JULGUE
              */
            conn.execute((StringBuilder sb) -> {
                sb.append("INSERT INTO PEDIDO_CLIENTE_GARCOM (PEDIDO_CLIENTE_ID, GARCOM_ID, MESA, COMANDA) ");
                sb.append("VALUES (?, ?, ?, ?); ");
            }, idPedido, pedido.getGarcomId(), pedido.getComanda(), pedido.getMesa());

            for (PedidoItem item : pedido.getProdutos()) {

                final long produtoId = item.getId();
                final long tamanhoId = item.getTamanhoId();
                final int quantidade = item.getQuantidade();


                // Produto + Item + Tamanho
                final int itemId = conn.selectOneRow(
                        (StringBuilder sb) -> {
                            sb.append("execute block ( ");
                            sb.append("    PEDIDO_ID bigint = ?, ");
                            sb.append("    PRODUTO_ID integer = ?, ");
                            sb.append("    QTDE integer = ?, ");
                            sb.append("    TAMANHO_ID integer = ? ");
                            sb.append(") returns ( ");
                            sb.append("    ID bigint ");
                            sb.append(") AS ");
                            sb.append("DECLARE VARIABLE preco_venda_final DM_MONEY = null; ");
                            sb.append("DECLARE VARIABLE preco_tamanho DM_MONEY = null; ");
                            sb.append("BEGIN ");
                            sb.append("    if (:tamanho_id <= 0 or :tamanho_id is null) then ");
                            sb.append("    begin ");
                            sb.append("        tamanho_id = null; ");
                            sb.append("    end ");
                            sb.append("    else ");
                            sb.append("    begin ");
                            /*
                             * Setamos o preço do tamanho do produto
                             */
                            sb.append("        SELECT ");
                            sb.append("            tpd.PRECO_VENDA ");
                            sb.append("        from ");
                            sb.append("            TAMANHO_PRODUTO_DISPONIVEL tpd ");
                            sb.append("        where ");
                            sb.append("            tpd.TAMANHO_PRODUTO_ID = :tamanho_id ");
                            sb.append("        and ");
                            sb.append("            tpd.PRODUTO_ID = :PRODUTO_ID ");
                            sb.append("        into :preco_tamanho; ");
                            sb.append("    end ");
                            /*
                             * Somamos ao preço do tamanho do produto o seu preço de
                             * venda em vez de usar apenas um dos dois. Em teoria
                             * não deveria existir um produto com tamanho e preço
                             * diferente de 0, mas é bom garantir.
                             */
                            sb.append("    select ");
                            sb.append("         p.PRECO_VENDA + coalesce(:preco_tamanho, 0) ");
                            sb.append("    from ");
                            sb.append("        PRODUTO p ");
                            sb.append("    where ");
                            sb.append("        p.ID = :PRODUTO_ID ");
                            sb.append("    into :preco_venda_final;  ");
                            sb.append("    IN AUTONOMOUS transaction do  ");
                            sb.append("    begin   ");
                            sb.append("        insert into PEDIDO_CLIENTE_ITEM (TAMANHO_PRODUTO_ID, PRODUTO_ID, PEDIDO_CLIENTE_ID, QUANTIDADE, VALOR_UNIDADE_COBRADO)  ");
                            sb.append("            values (:tamanho_id, :produto_id, :pedido_id, :qtde, coalesce(:preco_venda_final, 0))  ");
                            sb.append("            returning ID into ID;  ");
                            sb.append("    end     ");
                            sb.append("    suspend;  ");
                            sb.append("end ");
                        },
                        idPedido,
                        produtoId,
                        quantidade,
                        tamanhoId > 0 ? tamanhoId : null
                ).getInt("ID");

                // Opcionais
                if (item.getOpcionaisId() != null && item.getOpcionaisId().size() > 0) {
                    for (long opg : item.getOpcionaisId()) {

                        conn.execute(
                                (StringBuilder sb) -> {
                                    sb.append("execute block ( ");
                                    sb.append("    LOJA_ID integer = ?, ");
                                    sb.append("    id_pedido_item bigint = ?, ");
                                    sb.append("    PRODUTO_ID integer = ?, ");
                                    sb.append("    opcional_id integer = ?, ");
                                    sb.append("    opcional_valor_cobrado DM_MONEY = ?, ");
                                    sb.append("    TAMANHO_ID integer = ? ");
                                    sb.append(") AS ");
                                    sb.append("BEGIN ");
                                    sb.append("select first 1 ID from ( ");
                                    sb.append("	select ID from OPCIONAL_PRODUTO  ");
                                    sb.append("	where LOJA_ID = :LOJA_ID and ID = :opcional_id ");
                                    sb.append(") into :opcional_id; ");
                                    sb.append("    if (:tamanho_id is not null and :opcional_valor_cobrado = 0) then ");
                                    sb.append("    begin ");
                                    sb.append("        select ");
                                    sb.append("            coalesce(opvtp.VALOR_OPCIONAL, 0) ");
                                    sb.append("        from ");
                                    sb.append("            OP_PROD_VALOR_TAMANHO_PROD opvtp ");
                                    sb.append("        where ");
                                    sb.append("            opvtp.PRODUTO_ID = :produto_id ");
                                    sb.append("            and opvtp.OPD_OPCIONAL_ID = :opcional_id ");
                                    sb.append("            and opvtp.TPD_TAMANHO_ID = :tamanho_id ");
                                    sb.append("        into :opcional_valor_cobrado; ");
                                    sb.append("    end ");
                                    sb.append("    insert into PEDIDO_CLIENTE_ITEM_OPC(PEDIDO_CLIENTE_ITEM_ID, OPCIONAL_PRODUTO_ID, VALOR_COBRADO) ");
                                    sb.append("    values (:id_pedido_item, :opcional_id, :opcional_valor_cobrado); ");
                                    sb.append("END ");
                                },
                                pedido.getLojaId(),
                                itemId,
                                produtoId,
                                opg,
                                item.getValorAplicadoOpcionais() != null ? item.getValorAplicadoOpcionais().getOrDefault(opg, 0.0) : 0.0,
                                tamanhoId > 0 ? tamanhoId : null
                        );
                    }

                }

                //Ingredientes
                if ((item.getIngredientesRemovidosId() != null && item.getIngredientesRemovidosId().size() > 0) || (item.getIngredientesAdicionadosId() != null && item.getIngredientesAdicionadosId().size() > 0)) {

                    Map<Long, Boolean> ingredientes = new HashMap<>();

                    if (item.getIngredientesRemovidosId() != null && item.getIngredientesRemovidosId().size() > 0) {
                        for (long ing : item.getIngredientesRemovidosId()) {
                            ingredientes.put(ing, true);
                        }
                    }

                    if (item.getIngredientesAdicionadosId() != null && item.getIngredientesAdicionadosId().size() > 0) {
                        for (long ing : item.getIngredientesAdicionadosId()) {
                            ingredientes.put(ing, false);
                        }
                    }

                    for (Map.Entry<Long, Boolean> e : ingredientes.entrySet()) {
                        conn.execute(
                                (StringBuilder sb) -> {
                                    sb.append("execute block ( ");
                                    sb.append("    loja_id integer = ?, ");
                                    sb.append("    id_pedido_item bigint = ?, ");
                                    sb.append("    PRODUTO_ID integer = ?, ");
                                    sb.append("    id_ingrediente integer = ?, ");
                                    sb.append("    valor_ingrediente DM_MONEY = ?, ");
                                    sb.append("    ingrediente_is_removido DM_BOOL = ? ");
                                    sb.append(") AS ");
                                    sb.append("BEGIN ");
                                    sb.append("select first 1 INGREDIENTE_ID ");
                                    sb.append("from ( ");
                                    sb.append("    select r.INGREDIENTE_ID  ");
                                    sb.append("    from PRODUTO_INGREDIENTE r   ");
                                    sb.append("    inner join PRODUTO p on p.ID = r.INGREDIENTE_ID  ");
                                    sb.append("         where p.LOJA_ID = :loja_id  and p.ID = :id_ingrediente ");
                                    sb.append(")  into :id_ingrediente; ");
                                    /*
                                     * Quando não veio do app o valor cobrado pelo
                                     * ingrediente adicionado
                                     */
                                    sb.append("    if (:ingrediente_is_removido = 0 and :valor_ingrediente = 0) then ");
                                    sb.append("    begin ");
                                    sb.append("        select ");
                                    sb.append("            coalesce(pi.VALOR_ACRESCIMO, 0) ");
                                    sb.append("        from ");
                                    sb.append("            PRODUTO_INGREDIENTE pi ");
                                    sb.append("        where ");
                                    sb.append("            pi.PRODUTO_ID = :produto_id ");
                                    sb.append("            and pi.INGREDIENTE_ID = :id_ingrediente ");
                                    sb.append("        into :valor_ingrediente; ");
                                    sb.append("    end ");
                                    sb.append("    insert into PEDIDO_CLIENTE_ITEM_INGR(PEDIDO_CLIENTE_ITEM_ID, PI_INGREDIENTE_ID, PI_PRODUTO_ID, VALOR_COBRADO, ");
                                    sb.append("        IS_REMOVIDO) ");
                                    sb.append("    values (:id_pedido_item, :id_ingrediente, :produto_id, :valor_ingrediente, :ingrediente_is_removido); ");
                                    sb.append("end ");
                                },
                                pedido.getLojaId(),
                                itemId,
                                produtoId,
                                e.getKey(),
                                0,
                                e.getValue() ? 1 : 0
                        );

                    }

                    ingredientes.clear();
                }

                //Sabores + ingredientes dos sabores
                if (item.getSaboresId() != null && item.getSaboresId().size() > 0) {

                    int pedidoItemSaborProdId = -1;

                    for (long sab : item.getSaboresId()) {
                        pedidoItemSaborProdId = conn.selectOneRow(
                                (StringBuilder sb) -> {
                                    sb.append("execute block ( ");
                                    sb.append("    loja_id integer = ?, ");
                                    sb.append("    id_pedido_item bigint = ?, ");
                                    sb.append("    produto_id integer = ?, ");
                                    sb.append("    sabor_prod_id integer = ?, ");
                                    sb.append("    tamanho_id integer = ?, ");
                                    sb.append("    valor_prod_cobrado DM_MONEY = ? ");
                                    sb.append("    ) returns (ID integer) ");
                                    sb.append(" AS ");
                                    sb.append("BEGIN ");
                                    sb.append("select first 1 ID from ( ");
                                    sb.append("	select r.ID from SABOR r   ");
                                    sb.append("		where r.LOJA_ID = :loja_id and r.ID = :sabor_prod_id ");
                                    sb.append(") into  :sabor_prod_id; ");
                                    sb.append("    if (:valor_prod_cobrado = 0 and :tamanho_id is not null and :tamanho_id > 0) then ");
                                    sb.append("    begin ");
                                    sb.append("        select ");
                                    sb.append("             coalesce(r.VALOR_SABOR, 0) ");
                                    sb.append("         from  ");
                                    sb.append("             TPD_SPD_VALOR r ");
                                    sb.append("         where  ");
                                    sb.append("             r.PRODUTO_ID = :produto_id ");
                                    sb.append("             and r.SABOR_ID = :sabor_prod_id ");
                                    sb.append("             and r.TAMANHO_PRODUTO_ID = :tamanho_id ");
                                    sb.append("        into :valor_prod_cobrado; ");
                                    sb.append("    end ");
                                    sb.append("    IN AUTONOMOUS transaction do  ");
                                    sb.append("    begin   ");
                                    sb.append("    insert into PEDIDO_CLIENTE_ITEM_SABOR(PEDIDO_CLIENTE_ITEM_ID, SABOR_ID, VALOR_COBRADO) ");
                                    sb.append("    values (:id_pedido_item, :sabor_prod_id, :valor_prod_cobrado) returning ID into ID; ");
                                    sb.append("    end     ");
                                    sb.append(" suspend; ");
                                    sb.append("end ");
                                },
                                pedido.getLojaId(),
                                itemId,
                                produtoId,
                                sab,
                                tamanhoId,
                                item.getValorAplicadoSabor() != null ? item.getValorAplicadoSabor().getOrDefault(sab, 0.0) : 0.0
                        ).getInt("ID");

                    }
/*
                for (String sab : item.getString("SABORES").split(";")) {

                    Matcher m = Pattern.compile("\\[([^]]+)\\]").matcher(sab);
                    if (m.find()) {
                        for (String ing : m.group(1).split(",")) {

                            conn.execute(
                                    (StringBuilder sb) -> {
                                        sb.append("execute block ( ");
                                        sb.append("    loja_id integer = ?, ");
                                        sb.append("    pis_pedido_sabor_prod_id bigint = ?, ");
                                        sb.append("    PRODUTO_ID integer = ?, ");
                                        sb.append("    id_sabor integer = ?, ");
                                        sb.append("    id_sabor_ingrediente integer = ?, ");
                                        sb.append("    valor_cobrado_app DM_MONEY = ?, ");
                                        sb.append("    item_is_removido DM_BOOL = ? ");
                                        sb.append(") AS ");
                                        sb.append("BEGIN ");
                                        sb.append("select first 1 ID from ( ");
                                        sb.append("	select r.ID from SABOR r   ");
                                        sb.append("		where r.LOJA_ID = :loja_id and r.ID = :id_sabor ");
                                        sb.append("	union all  ");
                                        sb.append("	select r.ID from SABOR r   ");
                                        sb.append("		where r.LOJA_ID = :loja_id and r.BASE_CONTRATANTE_ID = :id_sabor ");
                                        sb.append(") into  :id_sabor; ");

                                        sb.append("select first 1 ID from ( ");
                                        sb.append("	select p.ID from PRODUTO p   ");
                                        sb.append("	where   ");
                                        sb.append("	    p.LOJA_ID = :loja_id and p.ID = :id_sabor_ingrediente ");
                                        sb.append("	union all  ");
                                        sb.append("	select p.ID from PRODUTO p   ");
                                        sb.append("	where   ");
                                        sb.append("	    p.LOJA_ID = :loja_id and p.BASE_CONTRATANTE_ID = :id_sabor_ingrediente ");
                                        sb.append(") into :id_sabor_ingrediente; 	 ");


                                        sb.append("     if (:item_is_removido = 0 and :valor_cobrado_app = 0) then ");
                                        sb.append("    begin ");
                                        sb.append("        select ");
                                        sb.append("            coalesce(spi.VALOR_ACRESCIMO, 0) ");
                                        sb.append("        from ");
                                        sb.append("            SABOR_PRODUTO_INGREDIENTE spi ");
                                        sb.append("        where ");
                                        sb.append("            spi.SPD_PRODUTO_ID = :produto_id ");
                                        sb.append("            and spi.SPD_SABOR_ID = :id_sabor ");
                                        sb.append("            and spi.INGREDIENTE_ID = :id_sabor_ingrediente ");
                                        sb.append("        into :valor_cobrado_app; ");
                                        sb.append("    end ");
                                        sb.append("    insert into PEDIDO_ITEM_SABOR_INGR2 (SBI_INGREDIENTE_ID, PIS_PEDIDO_SABOR_PROD_ID, ");
                                        sb.append("        SPD_SABOR_ID, SPD_PRODUTO_ID, VALOR_COBRADO, ");
                                        sb.append("        IS_REMOVIDO) ");
                                        sb.append("    values ( ");
                                        sb.append("        :id_sabor_ingrediente,  ");
                                        sb.append("        :pis_pedido_sabor_prod_id,  ");
                                        sb.append("        :id_sabor,  ");
                                        sb.append("        :PRODUTO_ID,  ");
                                        sb.append("        :valor_cobrado_app,  ");
                                        sb.append("        :item_is_removido ");
                                        sb.append("    ); ");
                                        sb.append("end ");
                                    },
                                    loja.getId(),
                                    pedidoItemSaborProdId,
                                    produtoId,
                                    Integer.parseInt(sab.split("\\[")[0]),
                                    Integer.parseInt(ing.replaceAll("[^0-9]", "").trim()),
                                    0,
                                    ing.startsWith("-") ? 1 : 0
                            );
                        }
                    }
                }*/

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            CLSException.register(e);
        }
    }
}