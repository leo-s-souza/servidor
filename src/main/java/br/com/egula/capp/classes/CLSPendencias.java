/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.classes;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.FrmModuloPaiBase;
import br.com.casaautomacao.casagold.classes.arquivos.CSPArquivosJson;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangDateTime;
import br.com.egula.capp.rest.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.util.Date;

import static br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesApplication.PATH;

/**
 * Essa classe é utilizada em servidor secundário para verificar as pendencias de execuções para serem enviadas a base
 * principal.
 */
public class CLSPendencias {
//
//    private static CSPArquivosJson arqPendencias;
//
//    /* Necessário para iniciar o arquivo de pendências. */
//    static {
//        try {
//            arqPendencias = new CSPArquivosJson(PATH + "/pendencia-principal/pendencias.json");
//        } catch (Exception e) {
//            CLSException.register(e);
//            e.printStackTrace();
//        }
//    }
//
//    private static boolean executandoThreadPendencias = false;
//
//    /**
//     * Método utilizado para executar as pendências existentes no arqPendencias.
//     *
//     * @throws Exception
//     */
//    static void resolvePendencias() throws Exception {
//
//        if (arqPendencias.exists() && arqPendencias.getArray().length() > 0) {
//
//            if (!executandoThreadPendencias) {
//                executandoThreadPendencias = true;
//
//                FrmModuloPaiBase.executor((executor) -> {
//                    try {
//
//                        final CSPInstrucoesSQLBase conn = CLSUtilidadesServidores.getConexaoBase();
//
//                        if (!conn.getConfs().getHost().equals("localhost")) {
//
//                            CSPLog.info(CLSPendencias.class, "Executando pendências -> " + arqPendencias.getContent());
//
//                            JSONArray jarray = arqPendencias.getArray();
//
//                            for (Object obj : jarray) {
//
//                                JSONObject json = (JSONObject) obj;
//
//                                String modulo = json.getString("modulo");
//                                String remoteAdress = json.getString("remoteAdress");
//
//                                json.remove("modulo");
//                                json.remove("remoteAdress");
//
//                                CSPLog.info(CLSPendencias.class, "Executando pendêcia do módulo " + modulo + " Info: " + json);
//
//                                switch (modulo) {
//                                    case "pedido":
//                                        CSPLog.info(PedidoHandler.class, "Pendência de pedido sendo processada: " + json.toString());
//
//                                        if (verificaPendenciaPedido(json, conn)) {
//
//                                            int jsonEndereco = 0;
//
//                                            if (json.has("enderecoId")) {
//                                                jsonEndereco = json.getInt("enderecoId");
//                                                json.put("enderecoId", ajustaEnderecoPedidoPendente(json.getInt("enderecoId"), conn));
//                                            }
//
//                                            PedidoHandler.processaPedido(json, remoteAdress, CLSUtilidadesServidores.getConexaoBase());
//
//                                            if(jsonEndereco != 0){
//                                                json.put("enderecoId", jsonEndereco);
//                                            }
//                                        }
//
//                                        json.put("modulo", modulo);
//                                        json.put("remoteAdress", remoteAdress);
//                                        removePendencia(json);
//
//                                        CSPLog.info(PedidoHandler.class, "Pendência de pedido processada!");
//
//                                        break;
//                                    case "login":
//                                        LoginHandler.processaLoginPendente(json, remoteAdress, CLSUtilidadesServidores.getConexaoBase());
//                                        json.put("modulo", modulo);
//                                        json.put("remoteAdress", remoteAdress);
//                                        removePendencia(json);
//                                        break;
//                                    case "adiciona-usuario":
//                                        AdicionaUsuarioHandler.processaAdicionaUsuarioPendente(json, remoteAdress, CLSUtilidadesServidores.getConexaoBase());
//                                        json.put("modulo", modulo);
//                                        json.put("remoteAdress", remoteAdress);
//                                        removePendencia(json);
//                                        break;
//                                    case "forca-logoff":
//                                        LogoffForcadoHandler.processaLogoffForcadoPendente(json, remoteAdress, CLSUtilidadesServidores.getConexaoBase());
//                                        json.put("modulo", modulo);
//                                        json.put("remoteAdress", remoteAdress);
//                                        removePendencia(json);
//                                        break;
//                                    case "adiciona-endereco-novo":
//                                        AdicionaEnderecoNovoHandler.processaEnderecoNovoPendente(json, remoteAdress, CLSUtilidadesServidores.getConexaoBase());
//                                        json.put("modulo", modulo);
//                                        json.put("remoteAdress", remoteAdress);
//                                        removePendencia(json);
//                                        break;
//                                    case "logoff":
//                                        LogoffHandler.processaLogoffPendente(json, remoteAdress, CLSUtilidadesServidores.getConexaoBase());
//                                        json.put("modulo", modulo);
//                                        json.put("remoteAdress", remoteAdress);
//                                        removePendencia(json);
//                                        break;
//                                }
//                            }
//                        }
//                        conn.close();
//
//                        if (arqPendencias.getArray().length() == 0) {
//                            executandoThreadPendencias = false;
//                            CSPLog.info(CLSPendencias.class, "Pendências executadas");
//                            executor.shutdown();
//                        }
//
//                    } catch (Exception e) {
//                        CLSException.register(e);
//                    }
//
//                }, 60, 120);
//            }
//        }
//
//    }
//
//    /**
//     * Adiciona pendência para ser executada no arquivo de pendecias.
//     *
//     * @param json         - Registro pendente que deve ser executado.
//     * @param modulo       - Módulo ao qual a pendência pertence.
//     * @param remoteAdress - Ip de onde veio a comunicação que gerou a pendência.
//     */
//    public static void addPendencia(JSONObject json, String modulo, String remoteAdress) {
//
//        CSPLog.info(CLSPendencias.class, "Adicionando pendência de " + modulo + "com as informações " + json);
//        try {
//            json.put("modulo", modulo);
//            json.put("remoteAdress", remoteAdress);
//
//            JSONArray jarray = new JSONArray();
//
//            if (arqPendencias.exists()) {
//                jarray = arqPendencias.getArray();
//            }
//
//            jarray.put(json);
//            arqPendencias.setArray(jarray);
//
////            if (arqPendencias.getArray().length() > 0 && !executandoThreadPendencias) {
////                resolvePendencias();
////            }
//
//        } catch (Exception e) {
//            CLSException.register(e);
//        }
//    }
//
//    /**
//     * Remove a pendência do arquivo de registro de pendências.
//     *
//     * @param json - Pendência executada que deve ser retirada do arquivo de pendências.
//     * @throws Exception
//     */
//    private static void removePendencia(JSONObject json) throws Exception {
//
//        JSONArray newArray = new JSONArray();
//
//        for (Object obj : arqPendencias.getArray()) {
//            JSONObject oldJson = (JSONObject) obj;
//
//            if (!oldJson.similar(json)) {
//                newArray.put(oldJson);
//            }
//        }
//
//        arqPendencias.setArray(newArray);
//    }
//
//    /**
//     * Método utilizado para verificar se a pendência de pedido pode ser
//     * executada.
//     *
//     * @param json - Info pedido.
//     * @param conn - connexão base.
//     * @return - True = pendência pode ser executada / False = Pendência não pode ser executada.
//     */
//    private static boolean verificaPendenciaPedido(JSONObject json, CSPInstrucoesSQLBase conn) throws Exception {
//
//        String cpf = json.getString("usuarioCpf");
//
//        if (cpf == null) {
//            return false;
//        }
//
//        ResultSet result = conn.select("SELECT a.ID FROM USUARIO a WHERE a.CPF = ?", cpf);
//
//        Long usuarioId = null;
//
//        if (result.next()) {
//            usuarioId = result.getLong("ID");
//        }
//
//        if (usuarioId == null) {
//            return false;
//        }
//
//        //Pega o ultimo pedido do cliente na base principal.
//        result = conn.select((StringBuilder sb) -> {
//            sb.append("SELECT First 1 ");
//            sb.append("    p.ID,  ");
//            sb.append("    a.LOJA_ID, ");
//            sb.append("    coalesce(p.FORMA_ENTREGA_ID, 0) as FORMA_ENTREGA_ID, ");
//            sb.append("    coalesce(p.FORMA_PAGAMENTO_ID, 0) as FORMA_PAGAMENTO_ID, ");
//            sb.append("    a.TOTAL, ");
//            sb.append("    a.HORARIO_EMISSAO,  ");
//            sb.append("    (SELECT COUNT(0) FROM VW_INFOS_PEDIDO_ITEM_DETALHADO pid where pid.PEDIDO_ID = p.ID) AS QUANT_ITENS  ");
//            sb.append("FROM  ");
//            sb.append("    VW_INFOS_PEDIDO_DETALHADO a  ");
//            sb.append("join  ");
//            sb.append("    PEDIDO p ON p.ID = a.ID  ");
//            sb.append("WHERE  ");
//            sb.append("    a.USUARIO_CPF = ? ");
//            sb.append("ORDER BY a.ID desc; ");
//        }, cpf);
//
//
//        if (result.next()) {
//            int pedidoIdBase = result.getInt("ID");
//            int pedidoLojaId = result.getInt("LOJA_ID");
//            int pedidoFormaEntregaId = result.getInt("FORMA_ENTREGA_ID");
//            int pedidoFormaPagamentoId = result.getInt("FORMA_PAGAMENTO_ID");
//            int pedidoTotal = result.getInt("TOTAL");
//            Date pedidoHorarioEmissao = result.getDate("HORARIO_EMISSAO");
//            int quantItens = result.getInt("QUANT_ITENS");
//
//            /*
//             * Caso o cliente tenha um pedido na base principal, é verificado se o último pedido tem as mesmas
//             * características dessa pendência.
//             */
//            if (pedidoLojaId == json.getInt("lojaId")
//                    && pedidoFormaEntregaId == json.getInt("entregaId")
//                    && pedidoFormaPagamentoId == json.getInt("pagamentoId")
//                    && pedidoTotal == json.getDouble("totalPedido")
//                    && quantItens == json.getJSONArray("produtos").length()) {
//
//                /*
//                 * Caso o pedido tenha as mesmas características, verifica-se se ele tem menos de 15 minutos.
//                 */
//                if (CSPUtilidadesLangDateTime.getIntervaloTempo(pedidoHorarioEmissao, CSPUtilidadesLangDateTime.getDataHoraObj(0), CSPUtilidadesLangDateTime.IntervaloTempo.MINUTOS) < 15) {
//
//                    boolean igual = true;
//
//                    /*
//                     * Percorre os produtos existentes na pendência, e verifica se existem produtos correspondentes
//                     * dentro do ultimo pedido do usuário.
//                     */
//                    for (Object o : json.getJSONArray("produtos")) {
//                        JSONObject obj = (JSONObject) o;
//
//                        if (conn.count(
//                                "VW_INFOS_PEDIDO_ITEM_DETALHADO",
//                                "PEDIDO_ID = ? and PRODUTO_ID = ? and QTDE = ? and VALOR_TOTAL = ?",
//                                pedidoIdBase,
//                                obj.get("id"),
//                                obj.get("quantidade"),
//                                obj.get("precoTotal")) != 1) {
//                            igual = false;
//                            break;
//
//                        }
//                    }
//
//                    /*
//                     * Caso o pedido chegue aqui como igual a pendência, não é feita a execução da mesma.
//                     */
//                    return !igual;
//                }
//            }
//        }
//
//        return true;
//    }
//
//    /**
//     * Em caso do pedido ser feito em um endereço que foi adicionado apenas na base secundária, é necessário identificar
//     * o endereço cadastrado na base principal, e adicionar o id desse novo endereço ao pedido.
//     *
//     * @param endId - Id do endereço registrado na pendência.
//     * @param conn  - Conexão com a base principal.
//     * @return
//     * @throws Exception
//     */
//    private static int ajustaEnderecoPedidoPendente(int endId, CSPInstrucoesSQLBase conn) throws Exception {
//
//        CSPInstrucoesSQLBase connLocal = CLSCapp.connDb();
//
//        ResultSet result = connLocal.select(sb -> {
//            sb.append("SELECT ");
//            sb.append("    a.LOGRADOURO, ");
//            sb.append("    a.NUMERO, ");
//            sb.append("    coalesce(a.COMPLEMENTO, '') as COMPLEMENTO, ");
//            sb.append("    coalesce(a.REFERENCIA, '') as REFERENCIA, ");
//            sb.append("    coalesce(a.CEP, '') as CEP, ");
//            sb.append("    coalesce(a.LATITUDE, '') as LATITUDE, ");
//            sb.append("    coalesce(a.LONGITUDE, '') as LONGITUDE, ");
//            sb.append("    a.BAIRRO, ");
//            sb.append("    a.CIDADE, ");
//            sb.append("    a.ESTADO_ID, ");
//            sb.append("    a.PAIS_ID ");
//            sb.append("FROM ");
//            sb.append("    VW_INFOS_ENDERECO a ");
//            sb.append("WHERE ");
//            sb.append("    a.ID = ? ");
//
//        }, endId);
//
//        if (result.next()) {
//            String logradouro = result.getString("LOGRADOURO");
//            String numero = result.getString("NUMERO");
//            String complemento = result.getString("COMPLEMENTO");
//            String referencia = result.getString("REFERENCIA");
//            String cep = result.getString("CEP");
//            String latitude = result.getString("LATITUDE");
//            String longitude = result.getString("LONGITUDE");
//            String bairro = result.getString("BAIRRO");
//            String cidade = result.getString("CIDADE");
//            int estadoId = result.getInt("ESTADO_ID");
//            int paisId = result.getInt("PAIS_ID");
//
//
//            ResultSet rs = conn.select(sb -> {
//                        sb.append("SELECT ");
//                        sb.append("    a.ID ");
//                        sb.append("FROM  ");
//                        sb.append("    VW_INFOS_ENDERECO a ");
//                        sb.append("WHERE ");
//                        sb.append("    a.LOGRADOURO = ? ");
//                        sb.append("and ");
//                        sb.append("    a.NUMERO = ? ");
//                        sb.append("and ");
//                        sb.append("    (a.COMPLEMENTO = ? OR a.COMPLEMENTO IS null) ");
//                        sb.append("and ");
//                        sb.append("    (a.REFERENCIA = ? OR a.REFERENCIA IS null) ");
//                        sb.append("and ");
//                        sb.append("    (a.CEP = ? OR a.CEP IS null) ");
//                        sb.append("and ");
//                        sb.append("    (a.LATITUDE = ? OR a.LATITUDE IS null) ");
//                        sb.append("and ");
//                        sb.append("    (a.LONGITUDE = ? OR a.LONGITUDE IS null) ");
//                        sb.append("and ");
//                        sb.append("    a.BAIRRO = ? ");
//                        sb.append("and ");
//                        sb.append("    a.CIDADE =  ? ");
//                        sb.append("and ");
//                        sb.append("    a.ESTADO_ID = ? ");
//                        sb.append("and ");
//                        sb.append("    a.PAIS_ID = ? ");
//                    },
//                    logradouro,
//                    numero,
//                    complemento,
//                    referencia,
//                    cep,
//                    latitude,
//                    longitude,
//                    bairro,
//                    cidade,
//                    estadoId,
//                    paisId
//            );
//
//            if (rs.next()) {
//                return rs.getInt("ID");
//            }
//        }
//
//        return endId;
//    }
}
