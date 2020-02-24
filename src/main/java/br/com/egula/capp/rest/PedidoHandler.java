/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.FrmModuloPaiBase;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLang;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangDateTime;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangJson;
import br.com.egula.capp.classes.*;
import br.com.egula.capp.model.*;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

import static br.com.egula.capp.ModuloRest.writeResponse;
import static br.com.egula.capp.classes.CLSCapp.KEY_GOOGLE_API;
import static br.com.egula.capp.classes.CLSCapp.listaModuloPessoa;

/**
 * Atende os pedidos efetuados e registra-os na base.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class PedidoHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        // Pega o endereço de IP do dispositivo que está comunicando.
        String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();
        CSPLog.info(PedidoHandler.class, "efetua-pedido(" + hostAddress + ")");

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

            CSPLog.info(PedidoHandler.class, input.toString());

            // Verificamos se existe o código do dispositivo na requisição. Se não existir, não podemos atender.
            if (!input.has("hardware_id") || input.isNull("hardware_id")) {
                writeResponse(httpExchange, 403, "Proibido");
                return;
            }

            {//Esse bloco foi feito temporariamente devido a um problema de pedidos repetidos.
                if (listaModuloPessoa.get(input.getInt("usuarioId")) != null) {
                    Object[] dataValor = listaModuloPessoa.get(input.getInt("usuarioId"));

                    if (CSPUtilidadesLangDateTime.getIntervaloTempo((Date) dataValor[0], CSPUtilidadesLangDateTime.getDataHoraObj(0), CSPUtilidadesLangDateTime.IntervaloTempo.SEGUNDOS) < 60) {

                        if ((Integer) dataValor[1] == input.getInt("lojaId")) {
                            if ((Double) dataValor[2] == input.getDouble("totalPedido")) {
                                CSPLog.info(PedidoHandler.class, "Este usuário já tem um pedido em andamento, o pedido vai ser ignorado.");
                                writeResponse(httpExchange, 200, "OK");
                                return;
                            } else {
                                listaModuloPessoa.put(input.getInt("usuarioId"), new Object[]{
                                                CSPUtilidadesLangDateTime.getDataHoraObj(0),
                                                input.getInt("lojaId"),
                                                input.getDouble("totalPedido")
                                        }
                                );
                            }
                        } else {
                            listaModuloPessoa.put(input.getInt("usuarioId"), new Object[]{
                                            CSPUtilidadesLangDateTime.getDataHoraObj(0),
                                            input.getInt("lojaId"),
                                            input.getDouble("totalPedido")
                                    }
                            );
                        }
                    } else {
                        listaModuloPessoa.put(input.getInt("usuarioId"), new Object[]{
                                        CSPUtilidadesLangDateTime.getDataHoraObj(0),
                                        input.getInt("lojaId"),
                                        input.getDouble("totalPedido")
                                }
                        );
                    }
                } else {
                    listaModuloPessoa.put(input.getInt("usuarioId"), new Object[]{
                            CSPUtilidadesLangDateTime.getDataHoraObj(0),
                            input.getInt("lojaId"),
                            input.getDouble("totalPedido")
                    });
                }
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(input);

            try {
                final CSPInstrucoesSQLBase conn = CLSCapp.connDb();

                // Registra o pedido.
                boolean sucesso = processaPedido(input, hostAddress, conn);

                if (sucesso) {

                    // Obtém o Model do dispositivo e registra suas informações atualizadas na base.
                    Dispositivo dispositivo = Dispositivo.getDispositivo(input.getString("hardware_id"), conn);

                    if (dispositivo != null) {
                        try {
                            Dispositivo.registraAppTokenFcm(dispositivo, input, hostAddress, conn);
                            Dispositivo.registraAppVersao(dispositivo, input, hostAddress, conn);
                            Dispositivo.registraInfosDev(dispositivo, input, conn);
                        } catch (Exception ex) {
                            CLSException.register(ex);
                        }
                    }


                    ResultSet result = conn.select("SELECT a.USUARIO_ID, a.BONUS FROM VW_BONUS_LIVRE_USUARIO a WHERE a.USUARIO_ID = ?", input.getLong("usuarioId"));

                    // Escreve a resposta no output avisando que tudo ocorreu normalmente.
                    if (result.next()) {

                        double bonus = result.getDouble("BONUS");

                        conn.close();

//                        if(!CLSUtilidadesServidores.isServidorPrincipal() && conn.getConfs().getHost().equals("localhost")) {
//                            CLSPendencias.addPendencia(input, "pedido", hostAddress);
//                        }

                        writeResponse(httpExchange, 200, String.valueOf(bonus));
                    } else {
                        conn.close();

//                        if(!CLSUtilidadesServidores.isServidorPrincipal() && conn.getConfs().getHost().equals("localhost")) {
//                            CLSPendencias.addPendencia(input, "pedido", hostAddress);
//                        }

                        writeResponse(httpExchange, 200, "OK");
                    }

                } else {

                    conn.close();
                    // Escreve a resposta no output avisando que houve algum problema interno.
                    writeResponse(httpExchange, 500, "Internal Server Error");
                }

            } catch (SQLException ex) {
                CLSException.register(ex);

                // Envia notificação de erro no pedido para o monitoramento.
                try {
                    final List<Integer> usuarioIds = new ArrayList<>();

                    CSPInstrucoesSQLBase conn = CLSCapp.connDb();

                    ResultSet resultSet = conn.select("SELECT USUARIO_ID FROM VW_USUARIO_MONITORAMENTO");
                    while (resultSet.next()) {
                        usuarioIds.add(resultSet.getInt("USUARIO_ID"));
                    }

                    FrmModuloPaiBase.executor((executor) -> {
                        // Notifica os dispositivos de monitoramento que um pedido foi efetuado.
                        Firestore database = FirestoreClient.getFirestore();
                        CollectionReference colRef;

                        for (int usuarioId : usuarioIds) {
                            colRef = database.collection("usuario").document(String.valueOf(usuarioId)).collection("notificacao");

                            Notificacao notificacao = new Notificacao(
                                    "Monitoramento: Erro",
                                    json,
                                    "not-found",
                                    0,
                                    new Date(),
                                    null
                            );

                            colRef.add(notificacao);
                        }

                        executor.shutdown();
                    }, 0, 1);

                    conn.close();

                } catch (SQLException exc) {
                    CLSException.register(exc);
                }

                // Escreve a resposta no output avisando que houve algum problema interno.
                writeResponse(httpExchange, 500, "Internal Server Error");
            } catch (Exception e) {
                e.printStackTrace();
                CLSException.register(e);
            }
        }
    }

    /**
     * Registra um novo pedido na abse de dados.
     *
     * @param input         JSONObject - Json da requisição
     * @param remoteAddress String - Host que enviou o pedido.
     * @param conn          CSPInstrucoesSQLBase - Conexão com a base de dados.
     */
    private static boolean processaPedido(JSONObject input, String remoteAddress, CSPInstrucoesSQLBase conn) throws SQLException {
        Gson gson = new GsonBuilder().create();

        //Bloco para fix do caso de ingredientes cortesia
        {
            JSONArray arr = input.getJSONArray("produtos");

            for (Object o : arr) {
                JSONObject json = (JSONObject) o;

                if (!json.has("ingredientesCortesiaId")) {
                    json.put("ingredientesCortesiaId", new int[0]);
                }
            }
        }

        Pedido pedido = gson.fromJson(input.toString(), Pedido.class);

        Dispositivo dev = Dispositivo.getDispositivo(pedido.getDispositivoId(), conn);

        if (dev == null) {
            CSPLog.error(PedidoHandler.class, "DISPOSITIVO NÃO IDENTIFICADO! PEDIDO NÃO PODE SER SALVO");
            return false;
        }

        boolean wasPagoEmBonus = false;

        if (pedido.getPagamentoId() > 0) {
            ResultSet result = conn.select("SELECT IS_BONUS FROM FORMA_PAGAMENTO WHERE ID = ?", pedido.getPagamentoId());

            if (result.next()) {
                wasPagoEmBonus = result.getInt("IS_BONUS") == 1;

                //Esse bloco é utilizado devido a problemas de verificação de versões antigas do IOS
                if (wasPagoEmBonus) {
                    result = conn.select("SELECT a.USUARIO_ID, a.BONUS FROM VW_BONUS_LIVRE_USUARIO a WHERE a.USUARIO_ID = ?", pedido.getUsuarioId());

                    if (result.next()) {
                        if (pedido.getTotalPedido() > result.getDouble("BONUS")) {
                            CSPLog.error(PedidoHandler.class, "BÔNUS INSUFICIENTE! PEDIDO NÃO PODE SER SALVO");
                            return false;
                        }
                    }
                }
            }
        }

        int cupomDescontoId = 0;

        if (input.has("cupom_desconto_id")) {
            cupomDescontoId = input.getInt("cupom_desconto_id");
        }

        final long idPedido;
        idPedido = conn.selectOneRow(
                "select ID_PEDIDO from PR_REGISTRA_PEDIDO_REST (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                pedido.getLojaId(),
                pedido.getUsuarioId(),
                dev.getId(),
                remoteAddress,
                pedido.getPagamentoId() > 0 ? pedido.getPagamentoId() : null,
                wasPagoEmBonus ? 1 : 0,
                pedido.getTroco() > 0 ? pedido.getTroco() : null,
                pedido.getValorEntrega(),
                pedido.getEntregaId() > 0 ? pedido.getEntregaId() : null,
                pedido.getEnderecoId() > 0 ? pedido.getEnderecoId() : null,
                CSPUtilidadesLangJson.getFromJson(input, "observacao", ""),
                (cupomDescontoId == 0) ? null : input.getInt("cupom_desconto_id")
        ).getLong("ID_PEDIDO");

        /*
         * Esse try está aqui para que em caso de erro em algum momento após o cadastro do pedido, o pedido seja
         * automaticamente cancelado.
         */
        try {
            for (PedidoItem item : pedido.getProdutos()) {

                final long produtoId = item.getId();
                final long tamanhoId = item.getTamanhoId();
                final int quantidade = item.getQuantidade();

                //Item + produto + tamanho
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
                            sb.append("        insert into PEDIDO_ITEM (PEDIDO_ID, TAMANHO_PRODUTO_ID, PRODUTO_ID, VALOR_UNIDADE_COBRADO, QUANTIDADE)  ");
                            sb.append("            values (:pedido_id, :tamanho_id, :produto_id, coalesce(:preco_venda_final, 0), :qtde)  ");
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

                    List<Long> listaOpcionais = new ArrayList<>();
                    for (long opc : item.getOpcionaisId()){
                        if(!listaOpcionais.contains(opc)){
                            listaOpcionais.add(opc);
                        }
                    }

                    for (long opg : listaOpcionais) {

                        conn.execute(
                                (StringBuilder sb) -> {
                                    sb.append("execute block ( ");
                                    sb.append("    LOJA_ID integer = ?, ");
                                    sb.append("    id_pedido_item bigint = ?, ");
                                    sb.append("    produto_id integer = ?, ");
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
                                    sb.append("    if (:opcional_valor_cobrado = 0) then  ");
                                    sb.append("    begin ");
                                    sb.append("        SELECT ");
                                    sb.append("            coalesce(opd.ACRESCIMO, 0) ");
                                    sb.append("        FROM ");
                                    sb.append("            OPCIONAL_PRODUTO_DISPONIVEL opd ");
                                    sb.append("        WHERE ");
                                    sb.append("            opd.OPCIONAL_PRODUTO_ID = :opcional_id ");
                                    sb.append("            AND opd.PRODUTO_ID = :produto_id ");
                                    sb.append("            AND APP_STATUS <> 0 ");
                                    sb.append("        INTO :opcional_valor_cobrado; ");
                                    sb.append("    end ");
                                    sb.append("    insert into PEDIDO_ITEM_OPCIONAL_PROD(PEDIDO_ITEM_ID, OPCIONAL_PRODUTO_ID, VALOR_COBRADO) ");
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
                                    sb.append("    ingrediente_is_removido DM_BOOL = ?, ");
                                    sb.append("    ingrediente_is_cortesia DM_BOOL = ? ");
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
                                    sb.append("    insert into PEDIDO_ITEM_INGR(PEDIDO_ITEM_ID, PI_INGREDIENTE_ID, PI_PRODUTO_ID, VALOR_COBRADO, ");
                                    sb.append("        IS_REMOVIDO, IS_CORTESIA) ");
                                    sb.append("    values (:id_pedido_item, :id_ingrediente, :produto_id, :valor_ingrediente, :ingrediente_is_removido, :ingrediente_is_cortesia); ");
                                    sb.append("end ");
                                },
                                pedido.getLojaId(),
                                itemId,
                                produtoId,
                                e.getKey(),
                                0,
                                e.getValue() ? 1 : 0,
                                item.getIngredientesCortesiaId().contains(e.getKey()) ? 1 : 0
                        );

                    }

                    ingredientes.clear();
                }

                //Sabores + ingredientes dos sabores
                if (item.getSabores() != null && item.getSabores().size() > 0) {

                    //Lista de sabores para verificação das divisões
                    List<Sabor> saboresLista = new ArrayList<>();
                    int verifSaborDuplicado = 0;

                    for (Sabor sab : item.getSabores()) {
                        sab.setQuantidade(1);
                        for (Sabor sabDuplicado : saboresLista) {
                            if (sabDuplicado.getId() == (sab.getId())) {
                                sabDuplicado.setQuantidade(sabDuplicado.getQuantidade() + 1);
                                verifSaborDuplicado = 1;
                            }
                        }
                        if (verifSaborDuplicado == 0) {
                            saboresLista.add(sab);
                        } else {
                            verifSaborDuplicado = 0;
                        }
                    }


                    int pedidoItemSaborProdId;

                    for (Sabor sab : saboresLista) {
                        pedidoItemSaborProdId = conn.selectOneRow(
                                (StringBuilder sb) -> {
                                    sb.append("execute block ( ");
                                    sb.append("    loja_id integer = ?, ");
                                    sb.append("    id_pedido_item bigint = ?, ");
                                    sb.append("    produto_id integer = ?, ");
                                    sb.append("    sabor_prod_id integer = ?, ");
                                    sb.append("    tamanho_id integer = ?, ");
                                    sb.append("    valor_prod_cobrado DM_MONEY = ?, ");
                                    sb.append("    quantidade_divisao integer = ? ");
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
                                    sb.append("    insert into PEDIDO_ITEM_SABOR_PROD2(PEDIDO_ITEM_ID, SABOR_ID, VALOR_COBRADO, QUANT_DIVISAO) ");
                                    sb.append("    values (:id_pedido_item, :sabor_prod_id, :valor_prod_cobrado, :quantidade_divisao) returning ID into ID; ");
                                    sb.append("    end     ");
                                    sb.append(" suspend; ");
                                    sb.append("end ");
                                },
                                pedido.getLojaId(),
                                itemId,
                                produtoId,
                                sab.getId(),
                                tamanhoId,
                                item.getValorAplicadoSabor().getOrDefault(sab.getId(), 0.0),
                                sab.getQuantidade()
                        ).getInt("ID");

                        Map<Long, Boolean> ingredientes = new HashMap<>();

                        if (sab.getIngredientesAdicionados() != null && !sab.getIngredientesAdicionados().isEmpty()) {
                            for (Long ingrediente : sab.getIngredientesAdicionados()) {
                                ingredientes.put(ingrediente, false);
                            }
                        }

                        if (sab.getIngredientesRemovidos() != null && !sab.getIngredientesRemovidos().isEmpty()) {
                            for (Long ingrediente : sab.getIngredientesRemovidos()) {
                                ingredientes.put(ingrediente, true);
                            }
                        }

                        for (Map.Entry<Long, Boolean> ingrediente : ingredientes.entrySet()) {
                            conn.execute((StringBuilder sb) -> {
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
                                    pedido.getLojaId(),
                                    pedidoItemSaborProdId,
                                    produtoId,
                                    sab.getId(),
                                    ingrediente.getKey(),
                                    0,
                                    ingrediente.getValue() ? 1 : 0
                            );
                        }
                    }
                } else if (item.getSaboresId() != null && item.getSaboresId().size() > 0) {

                    //TODO: Retirar esse bloco quando possivel.
                    for (long sab : item.getSaboresId()) {
                        conn.selectOneRow(
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
                                    sb.append("    insert into PEDIDO_ITEM_SABOR_PROD2(PEDIDO_ITEM_ID, SABOR_ID, VALOR_COBRADO) ");
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
                                item.getValorAplicadoSabor().getOrDefault(sab, 0.0)
                        ).getInt("ID");
                    }
                }
            }

            try {
                Distancia distancia = pedido.getDistancia();
                if (distancia != null && distancia.getEnderecoOrigemID() > 0 && distancia.getEnderecoDestinoID() > 0) {
                    ResultSet rs = conn.select((sb -> {
                                sb.append("SELECT ");
                                sb.append("    a.ID, ");
                                sb.append("    a.DISTANCIA, ");
                                sb.append("    a.TEMPO ");
                                sb.append("FROM ");
                                sb.append("    DISTANCIA_ENDERECO a ");
                                sb.append("WHERE ");
                                sb.append("    a.ORIGEM_ENDERECO_ID = ?  ");
                                sb.append("    AND a.DESTINO_ENDERECO_ID = ? ");
                            }),
                            distancia.getEnderecoOrigemID(),
                            distancia.getEnderecoDestinoID()
                    );

                    if (!rs.next()) {
                        conn.execute((sb -> {
                                    sb.append("INSERT INTO ");
                                    sb.append("    DISTANCIA_ENDERECO ( ");
                                    sb.append("        ORIGEM_ENDERECO_ID, ");
                                    sb.append("        DESTINO_ENDERECO_ID, ");
                                    sb.append("        DISTANCIA, ");
                                    sb.append("        TEMPO ");
                                    sb.append("    ) ");
                                    sb.append("    VALUES ( ");
                                    sb.append("        ?, ");
                                    sb.append("        ?, ");
                                    sb.append("        ?, ");
                                    sb.append("        ? ");
                                    sb.append("    ) ");
                                    sb.append("RETURNING ");
                                    sb.append("    ID;");
                                }),
                                distancia.getEnderecoOrigemID(),
                                distancia.getEnderecoDestinoID(),
                                distancia.getDistancia(),
                                distancia.getTempo()
                        );
                    }

                    conn.execute(
                            "update PEDIDO a set a.DISTANCIA_LOJA_ENTREGA = ? where a.ID = ?",
                            distancia.getDistancia(),
                            idPedido
                    );

                } else {
                    trataDistanciaLojaUsuarioPedido(conn, idPedido);
                }
            } catch (Exception ex) {
                cancelaPedidoPosErro(idPedido, remoteAddress, conn);
                CLSException.register(ex);

                return false;
            }

        } catch (Exception ex) {
            cancelaPedidoPosErro(idPedido, remoteAddress, conn);
            CLSException.register(ex);

            return false;
        }

        // Prepara informações dos itens do pedido para enviar notificação de monitoramento.
        try {
            final List<Integer> usuarioIds = new ArrayList<>();
            final String textoNotificacao = montaNotificacaoMonitoramentoPedidoEmitido(idPedido, conn);

            ResultSet resultSet = conn.select("SELECT USUARIO_ID FROM VW_USUARIO_MONITORAMENTO");
            while (resultSet.next()) {
                usuarioIds.add(resultSet.getInt("USUARIO_ID"));
            }

            FrmModuloPaiBase.executor((executor) -> {
                // Notifica os dispositivos de monitoramento que um pedido foi efetuado.
                Firestore database = FirestoreClient.getFirestore();
                CollectionReference colRef;

                for (int usuarioId : usuarioIds) {
                    colRef = database.collection("usuario").document(String.valueOf(usuarioId)).collection("notificacao");

                    Notificacao notificacao = new Notificacao(
                            "Monitoramento: Pedido Efetuado",
                            textoNotificacao,
                            "https://firebasestorage.googleapis.com/v0/b/e-gula.appspot.com/o/icone_monitoramento.png?alt=media&token=5f0ecb1c-76f5-4757-8cb7-7e38fa37bd99",
                            0,
                            new Date(),
                            null
                    );

                    colRef.add(notificacao);
                }

                executor.shutdown();
            }, 0, 1);

        } catch (Exception ex) {
            CLSException.register(ex);
        }

        return true;
    }

    private static void cancelaPedidoPosErro(Long idPedido, String host, CSPInstrucoesSQLBase conn) throws SQLException {
        conn.execute(
                "execute procedure PR_CANCELA_PEDIDO_WEB (?, ?)",
                idPedido, host
        );
    }

    /**
     * Efetua  o tratamento necessário para determinar a distância entre a
     * loja e o endereço de entrega do pedido, registrando-o na base.
     * <p>
     * O processo funciona calculando a distância em Km entre as lat/long dos
     * dois endereços(loja e entrea). Se os endereços a serem usados não possuem
     * uma lat/long registrada usa-se a api do google para determinar essa
     * informação e registra-se a mesma na base para utilização futura
     *
     * @param conn     CSPInstrucoesSQLBase
     * @param idPedido long
     */
    private static void trataDistanciaLojaUsuarioPedido(CSPInstrucoesSQLBase conn, long idPedido) throws Exception {

        final ResultSet ped = conn.selectOneRow((StringBuilder sb) -> {
            sb.append("select  ");
            sb.append("    iif(e.LATITUDE is not null and e.LONGITUDE is not null, null, e.LOGRADOURO || ', ' || IIF(COALESCE(e.NUMERO, e.NUMERO, 0) = 0, '', e.NUMERO) || ' ' || e.BAIRRO || ' ' || e.CIDADE || ' ' || e.ESTADO || ' ' || e.PAIS) as ENDERECO_USER, ");
            sb.append("    e.LATITUDE as LATITUDE_USER, ");
            sb.append("    e.LONGITUDE as LONGITUDE_USER, ");
            sb.append("    e.ID as ENDERECO_ID_USER, ");
            sb.append("    iif(e2.LATITUDE is not null and e2.LONGITUDE is not null, null, e2.LOGRADOURO || ', ' || IIF(COALESCE(e2.NUMERO, e2.NUMERO, 0) = 0, '', e2.NUMERO) || ' ' || e2.BAIRRO || ' ' || e2.CIDADE || ' ' || e2.ESTADO || ' ' || e2.PAIS) as ENDERECO_LOJA, ");
            sb.append("    e2.LATITUDE as LATITUDE_LOJA, ");
            sb.append("    e2.LONGITUDE as LONGITUDE_LOJA, ");
            sb.append("    e2.ID as ENDERECO_ID_LOJA ");
            sb.append("from PEDIDO r ");
            sb.append("inner join VW_INFOS_ENDERECO e ");
            sb.append("    on e.ID = r.USUARIO_ENDERECO_ENDERECO_ID ");
            sb.append("inner join VW_INFOS_ENDERECO e2 ");
            sb.append("    on e2.ID = r.LOJA_ENDERECO_ENDERECO_ID ");
            sb.append("where  ");
            sb.append("    e.ID <> e2.ID ");
            sb.append("    and e.NUMERO <> '' and e2.NUMERO <> '' ");
            sb.append("    and e.LOGRADOURO <> '' and e2.LOGRADOURO <> '' ");
            sb.append("    and r.DISTANCIA_LOJA_ENTREGA is null ");
            sb.append("    and r.ID = ? ");
            sb.append("order by  ");
            sb.append("    r.ID desc  ");
        }, idPedido);

        if (ped != null) {
            final String enderecoUser = ped.getString("ENDERECO_USER");
            final long idEnderecoUser = ped.getLong("ENDERECO_ID_USER");
            double latUser = ped.getDouble("LATITUDE_USER");
            double longUser = ped.getDouble("LONGITUDE_USER");

            final String enderecoLoja = ped.getString("ENDERECO_LOJA");
            final long idEnderecoLoja = ped.getLong("ENDERECO_ID_LOJA");
            double latLoja = ped.getDouble("LATITUDE_LOJA");
            double longLoja = ped.getDouble("LONGITUDE_LOJA");

            final GeoApiContext context = new GeoApiContext.Builder().apiKey(KEY_GOOGLE_API).build();

            if ((latUser == 0 || longUser == 0) && enderecoUser != null) {
                final GeocodingResult[] results = GeocodingApi.geocode(context, enderecoUser).await();

                if (results != null && results.length > 0 && results[0] != null) {
                    latUser = results[0].geometry.location.lat;
                    longUser = results[0].geometry.location.lng;
                    conn.execute(
                            "update ENDERECO a set a.LATITUDE = ?, a.LONGITUDE = ? where a.ID = ?",
                            latUser,
                            longUser,
                            idEnderecoUser
                    );
                } else {
                    latUser = 0;
                    longUser = 0;
                }
            }

            if ((latLoja == 0 || longLoja == 0) && enderecoLoja != null) {
                final GeocodingResult[] results = GeocodingApi.geocode(context, enderecoLoja).await();

                if (results != null && results.length > 0 && results[0] != null) {
                    latLoja = results[0].geometry.location.lat;
                    longLoja = results[0].geometry.location.lng;
                    conn.execute(
                            "update ENDERECO a set a.LATITUDE = ?, a.LONGITUDE = ? where a.ID = ?",
                            latLoja,
                            longLoja,
                            idEnderecoLoja
                    );
                } else {
                    latLoja = 0;
                    longLoja = 0;
                }
            }

            Endereco enderecoOrigem = new Endereco(Integer.parseInt(String.valueOf(idEnderecoLoja)), "", "", "", "", "", "", "", "", "", latLoja, longLoja);
            Endereco enderecoDestino = new Endereco(Integer.parseInt(String.valueOf(idEnderecoUser)), "", "", "", "", "", "", "", "", "", latUser, longUser);

            Distancia distancia = CLSUtilidades.getDistanciaPorRota(conn, context, enderecoOrigem, enderecoDestino);

            if (distancia != null) {
                conn.execute(
                        "update PEDIDO a set a.DISTANCIA_LOJA_ENTREGA = ? where a.ID = ?",
                        distancia.getDistancia(),
                        idPedido
                );
            }
        }

    }

    /**
     * Monta a notificação de monitoramento para um contratante
     *
     * @param conn CSPInstrucoesSQLBase
     * @throws Exception -
     */
    private static String montaNotificacaoMonitoramentoPedidoEmitido(long pedidoId, CSPInstrucoesSQLBase conn) throws Exception {
        final StringBuilder produtos = new StringBuilder();

        final ResultSet rs = conn.select(
                (StringBuilder sb) -> {
                    sb.append("select  ");
                    sb.append("    r.TAMANHO,  ");
                    sb.append("    r.PRODUTO,  ");
                    sb.append("    r.TOTAL,  ");
                    sb.append("    r.QUANTIDADE,  ");
                    sb.append("    r.OPCS, ");
                    sb.append("    r.INGRS,  ");
                    sb.append("    r.SABS ");
                    sb.append("from  ");
                    sb.append("    VW_INFOS_PEDIDOS_ITENS_MONITO r ");
                    sb.append("where  ");
                    sb.append("    r.PEDIDO_ID = ? ");
                },
                pedidoId
        );

        while (rs.next()) {
            produtos.append(rs.getString("PRODUTO"));

            if (rs.getString("TAMANHO") != null) {
                produtos.append(":");
                produtos.append(rs.getString("TAMANHO"));
            }

            if (rs.getString("OPCS") != null) {
                produtos.append(" (");
                produtos.append(rs.getString("OPCS"));
                produtos.append(") ");
            }

            if (rs.getString("INGRS") != null) {
                produtos.append(" (");
                produtos.append(rs.getString("INGRS"));
                produtos.append(") ");
            }

            if (rs.getString("SABS") != null) {
                produtos.append(" (");
                produtos.append(rs.getString("SABS"));
                produtos.append(") ");
            }

            produtos.append(" - ");
            produtos.append(rs.getInt("QUANTIDADE"));
            produtos.append(" - ");
            produtos.append(CSPUtilidadesLang.currencyRealFormat(rs.getDouble("TOTAL")));
            produtos.append("<n>");
        }

        return conn.selectOneRow(
                (StringBuilder sb) -> {
                    sb.append("execute block ( ");
                    sb.append("    PEDIDO_ID BIGINT = ?, ");
                    sb.append("    ITENS VARCHAR(3000) = ? ");
                    sb.append(") returns ( ");
                    sb.append("    TEXTO VARCHAR(3000) ");
                    sb.append(") AS ");
                    sb.append("DECLARE VARIABLE NOME_USUARIO VARCHAR(100); ");
                    sb.append("DECLARE VARIABLE TELEFONE_USUARIO VARCHAR(20); ");
                    sb.append("DECLARE VARIABLE DISPOSITIVO VARCHAR(100); ");
                    sb.append("DECLARE VARIABLE LOJA VARCHAR(100); ");
                    sb.append("DECLARE VARIABLE LOJA_STATUS INTEGER; ");
                    sb.append("DECLARE VARIABLE TOTAL varchar(50); ");
                    sb.append("DECLARE VARIABLE ENDERECO varchar(500); ");
                    sb.append("BEGIN ");
                    sb.append("    SELECT ");
                    sb.append("        u.NOME_COMPLETO, ");
                    sb.append("        tel.NUMERO, ");
                    sb.append("        l.NOME, ");
                    sb.append("        l.LOJA_STATUS, ");
                    sb.append("        (SELECT MONEY_REAL FROM PR_FORMAT_MONEY_REAL(p.TOTAL, 2)), ");
                    sb.append("        dd.ID_HARDWARE, ");
                    sb.append("        d.LOGRADOURO || ', ' || d.NUMERO || '<n>' || d.BAIRRO || ', ' || d.CIDADE || '<n>' || coalesce(iif(d.COMPLEMENTO <> '', d.COMPLEMENTO, null) || '. ', '') || coalesce(iif(d.REFERENCIA <> '', d.REFERENCIA, null), '') ");
                    sb.append("    FROM ");
                    sb.append("        PEDIDO p ");
                    sb.append("    JOIN VW_INFOS_ENDERECO d ");
                    sb.append("        on d.ID = p.USUARIO_ENDERECO_ENDERECO_ID ");
                    sb.append("    JOIN USUARIO u ");
                    sb.append("        on u.ID = p.USUARIO_ENDERECO_USUARIO_ID ");
                    sb.append("    JOIN LOJA l ");
                    sb.append("        on l.ID = p.LOJA_ENDERECO_LOJA_ID ");
                    sb.append("    JOIN DISPOSITIVO_ACAO da ");
                    sb.append("        on da.ID = p.DISPOSITIVO_ACAO_ID ");
                    sb.append("    JOIN DISPOSITIVO dd ");
                    sb.append("        on dd.ID = da.DISPOSITIVO_ID ");
                    sb.append("    JOIN USUARIO_TELEFONE t ");
                    sb.append("        on t.ID = p.USUARIO_TELEFONE_ID ");
                    sb.append("    JOIN TELEFONE tel ");
                    sb.append("        on tel.ID = t.TELEFONE_ID ");
                    sb.append("    WHERE ");
                    sb.append("        p.ID = :PEDIDO_ID ");
                    sb.append("    INTO ");
                    sb.append("        :NOME_USUARIO, ");
                    sb.append("        :TELEFONE_USUARIO, ");
                    sb.append("        :LOJA, ");
                    sb.append("        :LOJA_STATUS, ");
                    sb.append("        :TOTAL, ");
                    sb.append("        :DISPOSITIVO, ");
                    sb.append("        :ENDERECO; ");
                    sb.append("    TEXTO = IIF (:LOJA_STATUS = 0, 'LOJA DESATIVADA!!'|| '<n>'|| '<n>', '') || ");
                    sb.append("            'Emitido: '|| :NOME_USUARIO ||' '|| :TOTAL ||' '|| (SELECT p.FORMATADO FROM PR_FORMAT_HORARIO ('now') p) ");
                    sb.append("            ||'<n>' ");
                    sb.append("            ||'Loja: ' || :LOJA ");
                    sb.append("            ||'<n>' ");
                    sb.append("            ||'Pedido Nº: #' ||:PEDIDO_ID ");
                    sb.append("            ||'<n>' ");
                    sb.append("            ||'Dispositivo: ' || :DISPOSITIVO ");
                    sb.append("            ||'<n>' ");
                    sb.append("            ||'Telefone Usuário: ' || :TELEFONE_USUARIO ");
                    sb.append("            ||'<n>' ");
                    sb.append("            ||'Endereço: ' || :ENDERECO ");
                    sb.append("            ||'<n>' ");
                    sb.append("            ||'Produtos: ' || :ITENS; ");
                    sb.append("    SUSPEND; ");
                    sb.append("END ");

                },
                pedidoId,
                produtos.toString()
        ).getString("TEXTO");
    }
}