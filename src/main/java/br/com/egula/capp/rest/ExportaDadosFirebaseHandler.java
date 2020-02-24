/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.FrmModuloPaiBase;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.egula.capp.ModuloRest;
import br.com.egula.capp.classes.CLSCapp;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.classes.CLSUtilidades;
import br.com.egula.capp.model.*;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Classe responsável por realizar o login de usuários do Egula.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class ExportaDadosFirebaseHandler implements HttpHandler {

    private static final String HOST_REST_IMAGENS = "http://177.54.11.198:22250/imagens/";

    private Firestore database;
    private int lojaId;
    private CSPInstrucoesSQLBase conn;

    private int countProdutos;

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();

            CSPLog.info(ModuloRest.class, "exporta-dados(" + hostAddress + ")");

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
                JSONObject json = new JSONObject(sb.toString());

                if (json.has("loja") && !json.isNull("loja")) {

                    this.database = FirestoreClient.getFirestore();
                    this.lojaId = json.getInt("loja");
                    this.conn = CLSCapp.connDb();

                    CSPLog.info(ExportaDadosFirebaseHandler.class, "Iniciando exportação de dados da loja #" + lojaId);

                    // Fazemos em outra Thread para liberar o cliente que enviou o request HTTP o mais rápido possível.
                    FrmModuloPaiBase.executor((executor) -> {
                        try {
                            WriteBatch batch = database.batch();
                            exportaLoja(batch);
//                            batch.commit();

                            CSPLog.info(ExportaDadosFirebaseHandler.class, "Exportação de dados da loja #" + lojaId + " finalizada!");

                            executor.shutdown();
                        } catch (SQLException e) {
                            CSPLog.info("exporta-dados interrompido: SQL Exception.");
                            CLSException.register(e);

                            executor.shutdown();
                        }
                    },0,1);

                    ModuloRest.writeResponse(httpExchange, 200, "OK");
                }

            } else {
                CSPLog.info("exporta-dados interrompido: conteúdo do POST está vazio.");
                ModuloRest.writeResponse(httpExchange, 400, "Requisição inválida");
            }

        } catch (IOException | SQLException e) {
            CLSException.register(e);
            ModuloRest.writeResponse(httpExchange, 500, "Internal Server Error");
        }
    }

    /**
     * Exporta todas as informações importante da loja para o Firebase.
     */
    private void exportaLoja(WriteBatch batch) throws SQLException {

        // Contadores
        int countLoja = 0;
        int countPagamento = 0;
        int countEntrega = 0;
        int countEntregaDistancia = 0;
        int countGrupos = 0;
        countProdutos = 0;

        Loja loja = preparaLoja();

        if (loja != null) {

            // Loja
            {
                DocumentReference docRef = this.database.collection("loja").document(String.valueOf(lojaId));
                batch.set(docRef, loja);

                Map<String, Object> dados = new HashMap<>();
                Map<String, Object> dados2 = new HashMap<>();
                Endereco endereco = loja.getEndereco();

                dados.put("logradouro", endereco.getLogradouro());
                dados.put("numero", endereco.getNumero());
                dados.put("complemento", endereco.getComplemento());
                dados.put("referencia", endereco.getReferencia());
                dados.put("bairro", endereco.getBairro());
                dados.put("cidade", endereco.getCidade());
                dados.put("estado", endereco.getEstado());
                dados.put("pais", endereco.getPais());
                dados.put("cep", endereco.getCep());
                dados.put("latitude", endereco.getLatitude());
                dados.put("longitude", endereco.getLongitude());

                dados2.put("endereco", dados);

                batch.update(this.database.collection("loja").document(String.valueOf(lojaId)), dados2);

                countLoja++;
            }


            // Pagamento
            {
                CollectionReference colRef = this.database.collection("loja").document(String.valueOf(lojaId)).collection("pagamento");
                detelaColecao(colRef, batch);

                for (Pagamento pagamento : preparaPagamento()) {
                    DocumentReference docRef = colRef.document(String.valueOf(pagamento.getId()));
                    batch.set(docRef, pagamento);

                    countPagamento++;
                }
                batch.commit();
            }

            // Entrega
            {
                batch = database.batch();
                CollectionReference colRef = this.database.collection("loja").document(String.valueOf(lojaId)).collection("entrega");
                detelaColecao(colRef, batch);

                for (Entrega entrega : preparaEntrega()) {
                    DocumentReference docRef = colRef.document(String.valueOf(entrega.getId()));
                    batch.set(docRef, entrega);

                    countEntrega++;
                }
                batch.commit();
            }

            // Entrega Distância
            {
                batch = database.batch();
                CollectionReference colRef = this.database.collection("loja").document(String.valueOf(lojaId)).collection("distanciaEntrega");
                detelaColecao(colRef, batch);

                for (EntregaDistancia entregaDistancia : preparaEntregaDistancia()) {
                    DocumentReference docRef = colRef.document(String.valueOf(entregaDistancia.getId()));
                    batch.set(docRef, entregaDistancia);

                    countEntregaDistancia++;
                }
                batch.commit();
            }

            // Grupos e Subgrupos
            {
                batch = database.batch();
                CollectionReference colRef = this.database.collection("loja").document(String.valueOf(lojaId)).collection("grupo");
                detelaColecao(colRef, batch);

                for (Grupo grupo : preparaGrupos()) {
                    DocumentReference docRef = colRef.document(String.valueOf(grupo.getId()));
                    batch.set(docRef, grupo);

                    countGrupos++;
                }
                batch.commit();
            }

            // Produtos
            {
                batch = database.batch();
                exportaProdutos(batch);
            }

            int total = +countLoja + countEntrega + countPagamento + countEntregaDistancia + countGrupos + countProdutos;
            CSPLog.info(ExportaDadosFirebaseHandler.class, "Quantidade de gravações para o documento de Loja: " + countLoja);
            CSPLog.info(ExportaDadosFirebaseHandler.class, "Quantidade de gravações para os documentos de Tipo de Pagamento: " + countPagamento);
            CSPLog.info(ExportaDadosFirebaseHandler.class, "Quantidade de gravações para os documentos de Tipo de Entrega: " + countEntrega);
            CSPLog.info(ExportaDadosFirebaseHandler.class, "Quantidade de gravações para o documento de Entrega Distância: " + countEntregaDistancia);
            CSPLog.info(ExportaDadosFirebaseHandler.class, "Quantidade de gravações para os documentos de Grupos: " + countGrupos);
            CSPLog.info(ExportaDadosFirebaseHandler.class, "Quantidade de gravações para os documentos de Produtos: " + countProdutos);
            CSPLog.info(ExportaDadosFirebaseHandler.class, "QUANTIDADE DE GRAVAÇÕES TOTAIS PARA EXPORTAÇÃO DA LOJA: " + total);
        }
    }

    /**
     * Obtém as informações necessários da loja a partir da APP_FOOD.
     *
     * @return Objeto da loja requisitada.
     */
    private Loja preparaLoja() throws SQLException {
        ResultSet rs = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.NOME, ");
            sb.append("    a.CNPJ, ");
            sb.append("    a.IMAGEM, ");
            sb.append("    a.LOJA_STATUS, ");
            sb.append("    a.ENDERECO_ID, ");
            sb.append("    a.LOGRADOURO, ");
            sb.append("    a.NUMERO, ");
            sb.append("    a.COMPLEMENTO, ");
            sb.append("    a.REFERENCIA, ");
            sb.append("    a.BAIRRO, ");
            sb.append("    a.CIDADE, ");
            sb.append("    a.ESTADO, ");
            sb.append("    a.PAIS, ");
            sb.append("    a.CEP, ");
            sb.append("    a.LATITUDE, ");
            sb.append("    a.LONGITUDE ");
            sb.append("FROM ");
            sb.append("    VW_INFOS_LOJA_FIREBASE_NOVO a ");
            sb.append("WHERE ");
            sb.append("     a.ID = ?;");
        }, lojaId);

        if (rs.next()) {
            // Endereço
            int enderecoId = rs.getInt("ENDERECO_ID");
            String logradouro = rs.getString("LOGRADOURO");
            String numero = rs.getString("NUMERO");
            String complemento = rs.getString("COMPLEMENTO");
            String referencia = rs.getString("REFERENCIA");
            String bairro = rs.getString("BAIRRO");
            String cidade = rs.getString("CIDADE");
            String estado = rs.getString("ESTADO");
            String pais = rs.getString("PAIS");
            String cep = rs.getString("CEP");
            double lat = rs.getDouble("LATITUDE");
            double lng = rs.getDouble("LONGITUDE");

            // Loja
            String nome = rs.getString("NOME");
            String cnpj = rs.getString("CNPJ");
            String imagem = rs.getString("IMAGEM") != null ? HOST_REST_IMAGENS + rs.getString("IMAGEM") : null;
            int tipo = rs.getInt("LOJA_STATUS");

            List<Horario> horarios = new ArrayList<>();

            ResultSet resultSet = conn.select(sb -> {
                sb.append("SELECT ");
                sb.append("    a.ID, ");
                sb.append("    a.DIA_SEMANA, ");
                sb.append("    a.HORA_INICIO, ");
                sb.append("    a.HORA_FIM, ");
                sb.append("    a.VALOR_ACRESCIMO, ");
                sb.append("    a.TIPO_ATENDIMENTO, ");
                sb.append("    a.DATA_EXATA ");
                sb.append("FROM ");
                sb.append("    HORARIO_ATENDIMENTO a ");
                sb.append("WHERE ");
                sb.append("     a.LOJA_ID = ? ");
                sb.append("     AND a.APP_STATUS <> 0;");
            }, lojaId);

            while (resultSet.next()) {
                horarios.add(
                        new Horario(
                                resultSet.getInt("ID"),
                                resultSet.getInt("DIA_SEMANA"),
                                resultSet.getTime("HORA_INICIO"),
                                resultSet.getTime("HORA_FIM"),
                                resultSet.getDouble("VALOR_ACRESCIMO"),
                                resultSet.getInt("TIPO_ATENDIMENTO"),
                                resultSet.getDate("DATA_EXATA")
                        )
                );
            }

            Map<String, Boolean> garcons = new HashMap<>();
            resultSet = conn.select(sb -> {
                sb.append("SELECT ");
                sb.append("    a.GARCOM_ID ");
                sb.append("FROM ");
                sb.append("    LOJA_GARCOM a ");
                sb.append("WHERE ");
                sb.append("     a.LOJA_ID = ?;");
            }, lojaId);

            while (resultSet.next()) {
                garcons.put(resultSet.getString("GARCOM_ID"), true);
            }

            Endereco endereco = new Endereco(enderecoId, logradouro, numero, complemento, referencia, bairro, cidade, estado, pais, cep, lat, lng);

            if (endereco.getLatitude() == 0 || endereco.getLongitude() == 0) {
                endereco = CLSUtilidades.getLatLngEndereco(endereco);

                conn.execute(
                        "update ENDERECO a set a.LATITUDE = ?, a.LONGITUDE = ? where a.ID = ?",
                        endereco.getLatitude(),
                        endereco.getLongitude(),
                        endereco.getId()
                );
            }

            return new Loja(lojaId, nome, cnpj, imagem, tipo, endereco, horarios, garcons);
        }

        return null;
    }

    /**
     * Obtém as informações de pagamento da loja requisitada a partir da APP_FOOD.
     *
     * @return Array com todos os pagamentos aplicáveis.
     */
    private List<Pagamento> preparaPagamento() throws SQLException {
        List<Pagamento> pagamentos = new ArrayList<>();

        ResultSet rs = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.ID, ");
            sb.append("    a.DESCRICAO as NOME, ");
            sb.append("    a.IS_COM_TROCO as TROCO, ");
            sb.append("    a.IS_BONUS as BONUS ");
            sb.append("FROM ");
            sb.append("    FORMA_PAGAMENTO a ");
            sb.append("WHERE ");
            sb.append("    a.LOJA_ID = ? ");
            sb.append("    AND a.APP_STATUS <> 0;");
        }, lojaId);


        while (rs.next()) {
            // Adiciona o pagamento ao array.
            pagamentos.add(new Pagamento(rs.getInt("ID"), rs.getString("NOME"), rs.getInt("TROCO") == 1, rs.getInt("BONUS") == 1));
        }

        return pagamentos;
    }

    /**
     * Obtém as informações de entrega da loja requisitada a partir da APP_FOOD.
     *
     * @return Array com todas as entregas aplicáveis.
     */
    private List<Entrega> preparaEntrega() throws SQLException {
        List<Entrega> entregas = new ArrayList<>();

        ResultSet rs = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.ID, ");
            sb.append("    a.DESCRICAO as NOME, ");
            sb.append("    a.APP_IS_LOCAL as IS_LOCAL ");
            sb.append("FROM ");
            sb.append("    FORMA_ENTREGA a ");
            sb.append("WHERE ");
            sb.append("    a.LOJA_ID = ? ");
            sb.append("    AND a.APP_STATUS <> 0;");
        }, lojaId);

        while (rs.next()) {
            // Adiciona a entrega ao array.
            entregas.add(new Entrega(rs.getInt("ID"), rs.getString("NOME"), 0.0, rs.getInt("IS_LOCAL") == 1));
        }

        return entregas;
    }

    /**
     * Obtém as informações de distância de entrega da loja requisitada a partir da APP_FOOD.
     *
     * @return Array com todas as distâncias de entrega aplicáveis.
     */
    private List<EntregaDistancia> preparaEntregaDistancia() throws SQLException {
        List<EntregaDistancia> entregaDistancias = new ArrayList<>();

        ResultSet rs = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.ID, ");
            sb.append("    a.FORMA_ENTREGA_ID as ENTREGA, ");
            sb.append("    a.DISTANCIA_MIN, ");
            sb.append("    a.DISTANCIA_MAX, ");
            sb.append("    a.VALOR ");
            sb.append("FROM ");
            sb.append("    DISTANCIA_ENTREGA a ");
            sb.append("WHERE ");
            sb.append("    a.LOJA_ID = ?;");
        }, lojaId);

        while (rs.next()) {
            // Adiciona a distancia entrega ao array.
            entregaDistancias.add(new EntregaDistancia(rs.getInt("ID"), rs.getInt("ENTREGA"), rs.getDouble("DISTANCIA_MIN"), rs.getDouble("DISTANCIA_MAX"), rs.getDouble("VALOR")));
        }

        return entregaDistancias;
    }

    /**
     * Obtém as informações de grupos e subgrupos da loja requisitada a partir da APP_FOOD.
     *
     * @return Array com todos os grupos e subgrupos da loja.
     */
    private List<Grupo> preparaGrupos() throws SQLException {
        List<Grupo> grupos = new ArrayList<>();
        LinkedHashSet<Integer> gruposPais = new LinkedHashSet<>();

        ResultSet rs = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.ID, ");
            sb.append("    ad.APP_PATH as IMAGEM, ");
            sb.append("    a.GRUPO_PRODUTO_ID, ");
            sb.append("    a.DESCRICAO AS NOME ");
            sb.append("FROM ");
            sb.append("    GRUPO_PRODUTO a ");
            sb.append("LEFT JOIN ");
            sb.append("    ARQUIVO_DISPONIVEL ad ");
            sb.append("    ON ad.ID = a.ARQUIVO_DISPONIVEL_ID ");
            sb.append("WHERE ");
            sb.append("    a.LOJA_ID = ? ");
            sb.append("    AND a.APP_STATUS <> 0;");
        }, lojaId);

        while (rs.next()) {
            int grupoId = rs.getInt("ID");
            String nome = rs.getString("NOME");
            String imagem = rs.getString("IMAGEM") != null ? HOST_REST_IMAGENS + rs.getString("IMAGEM") : "";
            int grupoPai = rs.getInt("GRUPO_PRODUTO_ID");
            boolean isPizza = nome.trim().toLowerCase().contains("pizza") || nome.trim().toLowerCase().contains("pizzas") || nome.trim().toLowerCase().contains("calzone");

            if (grupoPai > 0) {
                gruposPais.add(grupoPai);
            }

            grupos.add(new Grupo(grupoId, nome, imagem, isPizza, false, grupoPai));
        }

        // Verificamos se o grupo tem algum subgrupo, se tiver, marcamos a variavel "subgrupos" como verdadeira.
        for (Integer id : gruposPais) {
            for (Grupo grupo : grupos) {
                if (id == grupo.getId()) {
                    grupo.setSubgrupos(true);
                    break;
                }
            }
        }

        return grupos;
    }

    /**
     *
     */
    private void exportaProdutos(WriteBatch batch) throws SQLException {
        List<Produto> produtos = new ArrayList<>();

        // Usar LinkedHashSet porque não podemos repetir os IDs.
        LinkedHashSet<Integer> grupoIds = new LinkedHashSet<>();

        ResultSet rs = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.ID, ");
            sb.append("    a.DESCRICAO AS NOME, ");
            sb.append("    a.PRECO_VENDA AS VALOR, ");
            sb.append("    a.GRUPO_PRODUTO_ID AS GRUPO_ID, ");
            sb.append("    ad.APP_PATH as IMAGEM, ");
            sb.append("    case ");
            sb.append("       when (a.QUANT_INGR_CORTESIA IS NULL) then 0 ");
            sb.append("       when (a.QUANT_INGR_CORTESIA IS NOT NULL) then a.QUANT_INGR_CORTESIA ");
            sb.append("    end as QUANT_INGR_CORTESIA ");
            sb.append("FROM ");
            sb.append("    PRODUTO a ");
            sb.append("LEFT JOIN ");
            sb.append("    ARQUIVO_DISPONIVEL ad ");
            sb.append("    ON ad.ID = a.ARQUIVO_DISPONIVEL_ID ");
            sb.append("LEFT JOIN ");
            sb.append("    GRUPO_PRODUTO g ");
            sb.append("    ON g.ID = a.GRUPO_PRODUTO_ID ");
            sb.append("WHERE ");
            sb.append("    a.LOJA_ID = ? ");
            sb.append("    AND a.APP_STATUS <> 0 ");
            sb.append("    AND g.APP_STATUS <> 0;");
        }, lojaId);

        while (rs.next()) {
            String imagem = rs.getString("IMAGEM") != null ? HOST_REST_IMAGENS + rs.getString("IMAGEM") : null;
            int grupoId = rs.getInt("GRUPO_ID");

            grupoIds.add(grupoId);

            produtos.add(
                    new Produto(
                            rs.getInt("ID"),
                            rs.getString("NOME"),
                            imagem,
                            rs.getDouble("VALOR"),
                            grupoId,
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new ArrayList<>(),
                            new ArrayList<>(),
                            rs.getInt("QUANT_INGR_CORTESIA")
                    )
            );
        }


        List<Tamanho> tamanhos = new ArrayList<>();
        rs = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.TAMANHO_PRODUTO_ID AS ID, ");
            sb.append("    t.DESCRICAO AS NOME, ");
            sb.append("    a.PRECO_VENDA AS VALOR, ");
            sb.append("    a.LIMITE_OPCIONAIS, ");
            sb.append("    a.LIMITE_SABORES, ");
            sb.append("    a.NUMERO_FATIAS, ");
            sb.append("    a.TAMANHO_MASSA, ");
            sb.append("    a.PRODUTO_ID ");
            sb.append("FROM ");
            sb.append("    TAMANHO_PRODUTO_DISPONIVEL a ");
            sb.append("JOIN ");
            sb.append("    TAMANHO_PRODUTO t ");
            sb.append("    ON t.ID = a.TAMANHO_PRODUTO_ID ");
            sb.append("WHERE ");
            sb.append("    t.LOJA_ID = ? ");
            sb.append("    AND a.APP_STATUS <> 0;");
        }, lojaId);

        while (rs.next()) {
            tamanhos.add(
                    new Tamanho(
                            rs.getInt("ID"),
                            rs.getString("NOME"),
                            rs.getDouble("VALOR"),
                            rs.getInt("LIMITE_OPCIONAIS"),
                            rs.getInt("LIMITE_SABORES"),
                            rs.getInt("NUMERO_FATIAS"),
                            rs.getDouble("TAMANHO_MASSA"),
                            rs.getInt("PRODUTO_ID")
                    )
            );
        }

        List<OpcionalItem> opcionalItems = new ArrayList<>();
        rs = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.OPCIONAL_PRODUTO_ID AS ID, ");
            sb.append("    o.DESCRICAO AS NOME, ");
            sb.append("    ad.APP_PATH AS IMAGEM, ");
            sb.append("    a.ACRESCIMO, ");
            sb.append("    a.APP_TELA, ");
            sb.append("    a.APP_IS_DEFAULT, ");
            sb.append("    a.PRODUTO_ID ");
            sb.append("FROM ");
            sb.append("    OPCIONAL_PRODUTO_DISPONIVEL a ");
            sb.append("JOIN ");
            sb.append("    OPCIONAL_PRODUTO o ");
            sb.append("    ON o.ID = a.OPCIONAL_PRODUTO_ID ");
            sb.append("LEFT JOIN ");
            sb.append("    ARQUIVO_DISPONIVEL ad ");
            sb.append("    ON ad.ID = a.ARQUIVO_DISPONIVEL_ID ");
            sb.append("WHERE ");
            sb.append("    o.LOJA_ID = ? ");
            sb.append("    AND a.APP_STATUS <> 0;");
        }, lojaId);

        while (rs.next()) {
            String imagem = rs.getString("IMAGEM") != null ? HOST_REST_IMAGENS + rs.getString("IMAGEM") : null;

            opcionalItems.add(
                    new OpcionalItem(
                            rs.getInt("ID"),
                            rs.getString("NOME"),
                            imagem,
                            rs.getDouble("ACRESCIMO"),
                            new ArrayList<>(),
                            rs.getInt("APP_IS_DEFAULT") == 1,
                            rs.getInt("APP_TELA"),
                            rs.getInt("PRODUTO_ID")
                    )
            );
        }

        // Opcional Preço
        rs = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.TPD_TAMANHO_ID AS ID, ");
            sb.append("    a.VALOR_OPCIONAL AS VALOR, ");
            sb.append("    a.OPD_OPCIONAL_ID ");
            sb.append("FROM ");
            sb.append("    OP_PROD_VALOR_TAMANHO_PROD a ");
            sb.append("JOIN ");
            sb.append("    OPCIONAL_PRODUTO s ");
            sb.append("    ON s.ID = a.OPD_OPCIONAL_ID ");
            sb.append("WHERE ");
            sb.append("    s.LOJA_ID = ?;");
        }, lojaId);

        while (rs.next()) {
            for (OpcionalItem opcItem : opcionalItems) {
                if (rs.getInt("OPD_OPCIONAL_ID") == opcItem.getId()) {

                    opcItem.getValorEspecial().add(
                            new ValorEspecial(
                                    rs.getInt("ID"),
                                    rs.getDouble("VALOR")
                            )
                    );
                }
            }
        }

        // Sabores
        List<Sabor> sabores = new ArrayList<>();
        {
            // Sabor
            rs = conn.select(sb -> {
                sb.append("SELECT ");
                sb.append("    a.SABOR_ID AS ID, ");
                sb.append("    s.DESCRICAO AS NOME, ");
                sb.append("    a.PRODUTO_ID ");
                sb.append("FROM ");
                sb.append("    SABOR_PRODUTO_DISPONIVEL a ");
                sb.append("JOIN ");
                sb.append("    SABOR s ");
                sb.append("    ON s.ID = a.SABOR_ID ");
                sb.append("WHERE ");
                sb.append("    s.LOJA_ID = ? ");
                sb.append("    AND a.APP_STATUS <> 0;");
            }, lojaId);

            while (rs.next()) {
                sabores.add(
                        new Sabor(rs.getInt("ID"), rs.getString("NOME"), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), rs.getInt("PRODUTO_ID"))
                );
            }

            // Sabor Preço
            rs = conn.select(sb -> {
                sb.append("SELECT ");
                sb.append("    a.TAMANHO_PRODUTO_ID AS ID, ");
                sb.append("    a.VALOR_SABOR AS VALOR, ");
                sb.append("    a.SABOR_ID ");
                sb.append("FROM ");
                sb.append("    TPD_SPD_VALOR a ");
                sb.append("JOIN ");
                sb.append("    SABOR s ");
                sb.append("    ON s.ID = a.SABOR_ID ");
                sb.append("WHERE ");
                sb.append("    s.LOJA_ID = ?;");
            }, lojaId);

            while (rs.next()) {
                for (Sabor sabor : sabores) {
                    if (rs.getInt("SABOR_ID") == sabor.getId()) {

                        sabor.getValorEspecial().add(
                                new ValorEspecial(
                                        rs.getInt("ID"),
                                        rs.getDouble("VALOR")
                                )
                        );
                    }
                }
            }

            // Sabor Ingredientes
            rs = conn.select(sb -> {
                sb.append("SELECT ");
                sb.append("    a.INGREDIENTE_ID, ");
                sb.append("    a.SPD_SABOR_ID, ");
                sb.append("    p.DESCRICAO AS NOME, ");
                sb.append("    a.VALOR_ACRESCIMO AS VALOR, ");
                sb.append("    a.APP_STATUS_INGREDIENTE AS STATUS ");
                sb.append("FROM ");
                sb.append("    SABOR_PRODUTO_INGREDIENTE a ");
                sb.append("JOIN ");
                sb.append("    PRODUTO p ");
                sb.append("    ON p.ID = a.INGREDIENTE_ID ");
                sb.append("WHERE ");
                sb.append("    p.LOJA_ID = ? ");
                sb.append("    AND a.APP_STATUS <> 0;");
            }, lojaId);

            while (rs.next()) {
                for (Sabor sabor : sabores) {
                    if (rs.getInt("SPD_SABOR_ID") == sabor.getId()) {

                        int status = rs.getInt("STATUS");

                        if (status == 3) {
                            sabor.getIngredientesAdicionais().add(
                                    new Ingrediente(
                                            rs.getInt("INGREDIENTE_ID"),
                                            rs.getString("NOME"),
                                            rs.getDouble("VALOR"),
                                            0,
                                            false,
                                            0,
                                            0
                                    )
                            );

                        } else {
                            sabor.getIngredientesBase().add(
                                    new Ingrediente(
                                            rs.getInt("INGREDIENTE_ID"),
                                            rs.getString("NOME"),
                                            rs.getDouble("VALOR"),
                                            0,
                                            status == 2,
                                            0,
                                            0
                                    )
                            );
                        }
                    }
                }
            }
        }

        // Ingredientes
        List<Ingrediente> ingredientesBase = new ArrayList<>();
        List<Ingrediente> ingredientesAdicionais = new ArrayList<>();
        rs = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.PRODUTO_ID, ");
            sb.append("    a.INGREDIENTE_ID, ");
            sb.append("    p.DESCRICAO AS NOME, ");
            sb.append("    a.VALOR_ACRESCIMO AS VALOR, ");
            sb.append("    a.APP_STATUS_INGREDIENTE AS STATUS, ");
            sb.append("    a.INGREDIENTE_SUBSTITUICAO_ID ");
            sb.append("FROM ");
            sb.append("    PRODUTO_INGREDIENTE a ");
            sb.append("JOIN ");
            sb.append("    PRODUTO p ");
            sb.append("    ON p.ID = a.INGREDIENTE_ID ");
            sb.append("WHERE ");
            sb.append("    p.LOJA_ID = ? ");
            sb.append("    AND a.APP_STATUS <> 0;");
        }, lojaId);

        while (rs.next()) {
            int status = rs.getInt("STATUS");

            if (status == 3) {
                ingredientesAdicionais.add(
                        new Ingrediente(
                                rs.getInt("INGREDIENTE_ID"),
                                rs.getString("NOME"),
                                rs.getDouble("VALOR"),
                                0,
                                false,
                                rs.getInt("PRODUTO_ID"),
                                0
                        )
                );

            } else {
                ingredientesBase.add(
                        new Ingrediente(
                                rs.getInt("INGREDIENTE_ID"),
                                rs.getString("NOME"),
                                rs.getDouble("VALOR"),
                                rs.getInt("INGREDIENTE_SUBSTITUICAO_ID"),
                                status == 2 || status == 4,
                                rs.getInt("PRODUTO_ID"),
                                0
                        )
                );
            }
        }


        for (Integer grupoId : grupoIds) {
            CollectionReference colRef = this.database.collection("loja").document(String.valueOf(lojaId)).collection("grupo").document(String.valueOf(grupoId)).collection("produto");
            detelaColecao(colRef, batch);

            for (Produto produto : produtos) {

                // Tamanhos
                {
                    List<Tamanho> tamanhoProduto = new ArrayList<>();
                    for (Tamanho tamanho : tamanhos) {
                        if (tamanho.produto() == produto.getId()) {
                            tamanhoProduto.add(tamanho);
                        }
                    }

                    produto.setTamanhos(tamanhoProduto);
                }

                // Opcionais
                {
                    List<Opcional> opcionais = new ArrayList<>();
                    List<OpcionalItem> opcionalItemsProdutoTela1 = new ArrayList<>();
                    List<OpcionalItem> opcionalItemsProdutoTela2 = new ArrayList<>();
                    boolean isMassa = false;

                    for (OpcionalItem item : opcionalItems) {
                        if (item.produto() == produto.getId()) {
                            switch (item.appTela()) {
                                case 1:
                                    opcionalItemsProdutoTela1.add(item);
                                    // Essa verificação não é boa, eu sei...
                                    isMassa = item.getNome().trim().equalsIgnoreCase("fina") || item.getNome().trim().equalsIgnoreCase("tradicional");
                                    break;

                                case 2:
                                    opcionalItemsProdutoTela2.add(item);
                                    break;
                            }
                        }
                    }

                    if (!opcionalItemsProdutoTela1.isEmpty()) {
                        opcionais.add(new Opcional(1, "Massa", isMassa, opcionalItemsProdutoTela1));
                    }

                    if (!opcionalItemsProdutoTela2.isEmpty()) {
                        opcionais.add(new Opcional(2, "Borda", false, opcionalItemsProdutoTela2));
                    }

                    produto.setOpcionais(opcionais);
                }

                // Sabores + Sabores Especiais
                {
                    List<Sabor> saboresProduto = new ArrayList<>();
                    for (Sabor sabor : sabores) {
                        if (sabor.produto() == produto.getId()) {
                            saboresProduto.add(sabor);
                        }
                    }

                    produto.setSabores(saboresProduto);
                }

                if (grupoId == produto.grupo()) {
                    countProdutos++;
                    DocumentReference docRef = colRef.document(String.valueOf(produto.getId()));
                    batch.set(docRef, produto);
                }

                // Ingredientes
                {
                    List<Ingrediente> ingredientesBaseProduto = new ArrayList<>();
                    List<Ingrediente> ingredientesAdicionaisProduto = new ArrayList<>();

                    for (Ingrediente ingredienteBase : ingredientesBase) {
                        if (ingredienteBase.produto() == produto.getId()) {
                            ingredientesBaseProduto.add(ingredienteBase);
                        }
                    }

                    for (Ingrediente ingredienteAdicional : ingredientesAdicionais) {
                        if (ingredienteAdicional.produto() == produto.getId()) {
                            ingredientesAdicionaisProduto.add(ingredienteAdicional);
                        }
                    }

                    produto.setIngredientesBase(ingredientesBaseProduto);
                    produto.setIngredientesAdicionais(ingredientesAdicionaisProduto);
                }

                if (grupoId == produto.grupo()) {
                    countProdutos++;
                    DocumentReference docRef = colRef.document(String.valueOf(produto.getId()));
                    batch.set(docRef, produto);
                }
            }
            batch.commit();
            batch = database.batch();
        }
    }


    /**
     * Adiciona ao batch o delete de todos os documentos da coleção passada por parâmetro.
     *
     * @param collection Coleção que será deletada. (Os documentos dela serão deletados,
     *                   consequentemente, a coleção será removida automaticamente)
     * @param batch      Batch para execução das exclusões. É interessante fazer qualquer outra execução nos
     *                   mesmos documentos/coleção pelo mesmo batch para evitar problemas com concorrencia.
     */
    private void detelaColecao(CollectionReference collection, WriteBatch batch) {
        try {
            ApiFuture<QuerySnapshot> future = collection.get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (DocumentSnapshot document : documents) {
                batch.delete(document.getReference());
            }

        } catch (ExecutionException | InterruptedException e) {
            CSPLog.error(ExportaDadosFirebaseHandler.class, "Erro ao realizar a leitura. Motivo: " + e.getMessage());
            CLSException.register(e);
        }
    }
}
