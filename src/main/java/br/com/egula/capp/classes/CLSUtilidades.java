/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.classes;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.arquivos.CSPArquivos;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.egula.capp.model.Distancia;
import br.com.egula.capp.model.Endereco;
import br.com.egula.capp.model.Horario;
import br.com.egula.capp.model.Usuario;
import br.com.egula.capp.model.garcom.Garcom;
import br.com.egula.capp.rest.AdicionaEnderecoHandler;
import br.com.egula.capp.rest.BackupBasePrincipal;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixElementStatus;
import com.google.maps.model.GeocodingResult;
import com.sun.net.httpserver.HttpExchange;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static br.com.egula.capp.classes.CLSCapp.KEY_GOOGLE_API;

/**
 * Classe utilitária de uso geral.
 *
 * @author Matheus Felipe Amelco
 * @since 1.3
 */
public class CLSUtilidades {
    /**
     * Retorna um objeto do usuário pelo seu ID.
     *
     * @param id   ID usuário.
     * @param conn Conexão com a base.
     */
    public synchronized static Usuario getUsuario(int id, CSPInstrucoesSQLBase conn) throws Exception {
        // Dados gerais do usuário
        ResultSet select = conn.selectOneRow((StringBuilder sb) -> {
            sb.append("SELECT ");
            sb.append("  r.CPF, ");
            sb.append("  r.NOME, ");
            sb.append("  r.SOBRENOME, ");
            sb.append("  r.EMAIL, ");
            sb.append(" r.TELEFONE_NUMERO, ");
            sb.append(" r.TIPO_USUARIO, ");
            sb.append("  r.TOTAL_BONUS_BLOQUEADO, ");
            sb.append(" r.TOTAL_BONUS_LIVRE ");
            sb.append("FROM ");
            sb.append("  VW_INFOS_USUARIO r ");
            sb.append("WHERE ");
            sb.append("  r.ID = ? ");
        }, id);

        if (select == null) {
            return null;
        }

        final String cpf = select.getString("CPF");
        final String nome = select.getString("NOME");
        final String sobrenome = select.getString("SOBRENOME");
        final String email = select.getString("EMAIL");
        final String telefoneNumero = select.getString("TELEFONE_NUMERO");
        final int tipoUsuario = select.getInt("TIPO_USUARIO");
        final double bonusBloqueado = select.getDouble("TOTAL_BONUS_BLOQUEADO");
        final double bonusLivre = select.getDouble("TOTAL_BONUS_LIVRE");

        // Endereços
        final ArrayList<Endereco> e = new ArrayList<>();

        select = conn.select((StringBuilder sb) -> {
            sb.append("SELECT ");
            sb.append("  r.ID, ");
            sb.append("  r.LOGRADOURO, ");
            sb.append("  r.NUMERO, ");
            sb.append("  r.COMPLEMENTO, ");
            sb.append(" r.REFERENCIA, ");
            sb.append(" r.CEP, ");
            sb.append("  r.LATITUDE, ");
            sb.append("  r.LONGITUDE, ");
            sb.append("  r.BAIRRO, ");
            sb.append("  r.CIDADE, ");
            sb.append("  r.ESTADO, ");
            sb.append("  r.PAIS ");
            sb.append("FROM ");
            sb.append("  VW_INFOS_USUARIO_ENDERECO r ");
            sb.append("WHERE ");
            sb.append("  r.USUARIO_ID = ? ");
        }, id);

        while (select.next()) {
            e.add(new Endereco(
                    select.getInt("ID"),
                    select.getString("LOGRADOURO"),
                    select.getString("NUMERO"),
                    select.getString("COMPLEMENTO"),
                    select.getString("REFERENCIA"),
                    select.getString("BAIRRO"),
                    select.getString("CIDADE"),
                    select.getString("ESTADO"),
                    select.getString("PAIS"),
                    select.getString("CEP"),
                    select.getDouble("LATITUDE"),
                    select.getDouble("LONGITUDE")
            ));
        }

        // Distâncias de endereços
        List<Distancia> distancias = new ArrayList<>();

        select = conn.select((StringBuilder sb) -> {
            sb.append("SELECT ");
            sb.append("    d.ID, ");
            sb.append("    d.ORIGEM_ENDERECO_ID, ");
            sb.append("    d.DESTINO_ENDERECO_ID, ");
            sb.append("    d.DISTANCIA, ");
            sb.append("    d.TEMPO ");
            sb.append("FROM ");
            sb.append("    DISTANCIA_ENDERECO d ");
            sb.append("JOIN ");
            sb.append("    VW_INFOS_USUARIO_ENDERECO e ");
            sb.append("        ON e.ID = d.DESTINO_ENDERECO_ID ");
            sb.append("WHERE ");
            sb.append("  e.USUARIO_ID = ? ");
        }, id);

        while (select.next()) {
            distancias.add(
                    new Distancia(
                            select.getInt("ID"),
                            select.getInt("ORIGEM_ENDERECO_ID"),
                            select.getInt("DESTINO_ENDERECO_ID"),
                            select.getDouble("DISTANCIA"),
                            select.getLong("TEMPO")
                    )
            );
        }

        return new Usuario(id, nome, sobrenome, telefoneNumero, email, cpf, tipoUsuario, bonusLivre, bonusBloqueado, e, distancias);

    }

    /**
     * Retorna um objeto do Garçom pelo seu ID.
     *
     * @param id   ID do Garçom.
     * @param conn Conexão com a base.
     */
    public static Garcom getGarcom(int id, CSPInstrucoesSQLBase conn) throws Exception {
        List<Integer> lojas = new ArrayList<>();

        ResultSet select = conn.selectOneRow((StringBuilder sb) -> {
            sb.append("SELECT ");
            sb.append("	r.CPF, ");
            sb.append("	r.NOME, ");
            sb.append("	r.SOBRENOME, ");
            sb.append("	r.EMAIL ");
            sb.append("FROM ");
            sb.append("	GARCOM r ");
            sb.append("WHERE ");
            sb.append("	r.ID = ? ");
        }, id);

        if (select == null) {
            return null;
        }

        final String cpf = select.getString("CPF");
        final String nome = select.getString("NOME");
        final String sobrenome = select.getString("SOBRENOME");
        final String email = select.getString("EMAIL");

        select = conn.select((StringBuilder sb) -> {
            sb.append("SELECT ");
            sb.append("	a.LOJA_ID ");
            sb.append("FROM ");
            sb.append("	LOJA_GARCOM a ");
            sb.append("WHERE ");
            sb.append("	a.GARCOM_ID = ? ");
        }, id);

        while (select.next()) {
            lojas.add(select.getInt("LOJA_ID"));
        }

        return new Garcom(id, cpf, nome, sobrenome, email, lojas);
    }

    /**
     * Obtém as coordenadas geográficas do endereço a partir do Google Maps API.
     *
     * @param endereco - Endereço que terá os pontos geográficos localizado.
     * @return - Retorna um objeto de Endereço com latitude e longitude definidos caso haja resultado na pesquisa.
     */
    public static Endereco getLatLngEndereco(Endereco endereco) {
        try {
            CSPLog.info(AdicionaEnderecoHandler.class, "Obtendo coordnadas geográficas do endereço: " + endereco.getEnderecoParaConsulta());
            final GeoApiContext context = new GeoApiContext.Builder().apiKey(KEY_GOOGLE_API).build();
            final GeocodingResult[] results = GeocodingApi.geocode(context, endereco.getEnderecoParaConsulta()).await();

            if (results != null && results.length > 0 && results[0] != null) {
                CSPLog.info(AdicionaEnderecoHandler.class, "Coordenadas obtidas: " + results[0].geometry.location.lat + ", " + results[0].geometry.location.lng);
                endereco.setLatitude(results[0].geometry.location.lat);
                endereco.setLongitude(results[0].geometry.location.lng);
            }

        } catch (Exception e) {
            CLSException.register(e);
        }

        return endereco;
    }

    /**
     * Obtem a distância e o tempo de viagem entre dois endereços através da menor rota possível. Primeiro consultamos
     * se ja existe a informação na base, em caos contrário, requisitamos o Google Maps Distance Matrix API.
     *
     * @param conn    - Conexão com a base de dados.
     * @param context - Context da API do Google Maps.
     * @param origem  - Endereço de origem.
     * @param destino - Endereço de destino.
     * @return - Classe Distancia que contém informações sobre os endereços, além da distância e tempo da rota.
     */
    public static Distancia getDistanciaPorRota(CSPInstrucoesSQLBase conn, GeoApiContext context, Endereco origem, Endereco destino) throws InterruptedException, ApiException, IOException, SQLException {
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
                origem.getId(),
                destino.getId()
        );

        if (rs.next()) {
            return new Distancia(rs.getInt("ID"), origem.getId(), destino.getId(), rs.getDouble("DISTANCIA"), rs.getLong("TEMPO"));
        }

        String[] ori = new String[1];
        String[] dest = new String[1];

        double origemLatitude = origem.getLatitude();
        double origemLongitude = origem.getLongitude();
        double destinoLatitude = destino.getLatitude();
        double destinoLongitude = destino.getLongitude();

        if (origemLatitude != 0 && origemLongitude != 0 && destinoLatitude != 0 && destinoLongitude != 0) {
            ori[0] = origemLatitude + "," + origemLongitude;
            dest[0] = destinoLatitude + "," + destinoLongitude;

        } else {
            ori[0] = origem.getEnderecoParaConsulta();
            dest[0] = destino.getEnderecoParaConsulta();
        }

        DistanceMatrix results = DistanceMatrixApi.getDistanceMatrix(context, ori, dest).await();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        CSPLog.info(CLSUtilidades.class, gson.toJson(results.rows));

        DistanceMatrixElement element = results.rows[0].elements[0];

        if (element.status == DistanceMatrixElementStatus.OK) {
            double quilometros = ((double) element.distance.inMeters) / 1000;
            long minutos = element.duration.inSeconds / 60;

            rs = conn.select((sb -> {
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
                    origem.getId(),
                    destino.getId(),
                    quilometros,
                    minutos
            );

            if (rs.next()) {
                return new Distancia(rs.getInt("ID"), origem.getId(), destino.getId(), quilometros, minutos);
            }
        }

        return null;
    }

    /**
     * Retorna a distância entre o novo endereço cadastrado e a loja em que a compra está sendo feita.
     *
     * @param idLoja
     * @param enderecoUsuario
     * @param conn
     * @return
     * @throws Exception
     */
    public static Distancia retornaDistanciaEnderecoLoja(int idLoja, Endereco enderecoUsuario, CSPInstrucoesSQLBase conn) throws Exception {

        ResultSet rs = conn.select((StringBuilder sb) -> {
            sb.append("  SELECT ");
            sb.append("      a.ID, ");
            sb.append("      a.CNPJ, ");
            sb.append("      a.NOME, ");
            sb.append("      le.STATUS_ENDERECO,");
            sb.append("      e.ID AS ID_ENDERECO, ");
            sb.append("      e.LOGRADOURO,");
            sb.append("      e.NUMERO,");
            sb.append("      e.COMPLEMENTO,");
            sb.append("      e.REFERENCIA,");
            sb.append("      e.BAIRRO,");
            sb.append("      e.CIDADE,");
            sb.append("      e.ESTADO,");
            sb.append("      e.PAIS,");
            sb.append("      e.CEP,");
            sb.append("      e.LATITUDE,");
            sb.append("      e.LONGITUDE");
            sb.append("  FROM ");
            sb.append("      LOJA a ");
            sb.append("  join");
            sb.append("      LOJA_ENDERECO le ON le.LOJA_ID = a.ID");
            sb.append("  join");
            sb.append("      VW_INFOS_ENDERECO e ON e.ID = le.ENDERECO_ID");
            sb.append("  WHERE");
            sb.append("      a.ID = ?");
            sb.append("  and");
            sb.append("      le.STATUS_ENDERECO = 1");
        }, idLoja);

        if (rs.next()) {
            Endereco endLoja = new Endereco(
                    rs.getInt("ID_ENDERECO"),
                    rs.getString("LOGRADOURO"),
                    rs.getString("NUMERO"),
                    rs.getString("COMPLEMENTO"),
                    rs.getString("REFERENCIA"),
                    rs.getString("BAIRRO"),
                    rs.getString("CIDADE"),
                    rs.getString("ESTADO"),
                    rs.getString("PAIS"),
                    rs.getString("CEP"),
                    rs.getDouble("LATITUDE"),
                    rs.getDouble("LONGITUDE")
            );

            final GeoApiContext context = new GeoApiContext.Builder().apiKey(KEY_GOOGLE_API).build();

            Distancia distancia = CLSUtilidades.getDistanciaPorRota(conn, context, endLoja, enderecoUsuario);

            return distancia;
        }

        return null;
    }

    /**
     * Método faz a atualização de bônus do usuário no firebase.
     * <p>
     * Caso o documento já tenha os campos dentro de usuário é apenas feito o update do bônus livre.
     * <p>
     * Caso o documento não tenha os campos, é adicionado os valores de id de usuário e bônus livre.
     *
     * @param refUsuario
     * @param usuarioId
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void atualizaBonusUsuarioFirebird(DocumentReference refUsuario, long usuarioId, CSPInstrucoesSQLBase conn) throws Exception {

        double bonusLivre = conn.selectOneRow("SELECT p.TOTAL_BONUS_LIVRE FROM PR_RETORNA_BONUS_USUARIO (?) p",
                usuarioId
        ).getDouble("TOTAL_BONUS_LIVRE");

        // asynchronously retrieve the document
        ApiFuture<DocumentSnapshot> future = refUsuario.get();

        // block on response
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            refUsuario.update("bonusLivre", bonusLivre);
        } else {


            refUsuario.set(new LinkedHashMap<String, Object>() {
                {
                    put("id", usuarioId);
                    put("bonusLivre", bonusLivre);
                }
            });
        }
    }

    /**
     * Método faz a atualização de bônus do usuário no firebase.
     * <p>
     * Caso o documento já tenha os campos dentro de usuário é apenas feito o update do bônus livre.
     * <p>
     * Caso o documento não tenha os campos, é adicionado os valores de id de usuário e bônus livre.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void atualizaHorariosLojaFirebird(long lojaId, ArrayList<HashMap<String, Object>> horarios) throws Exception {

        Firestore database = FirestoreClient.getFirestore();
        DocumentReference refLoja = database.collection("loja").document(String.valueOf(lojaId));

        // asynchronously retrieve the document
        ApiFuture<DocumentSnapshot> future = refLoja.get();

        // block on response
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            refLoja.update("horarios", horarios);
        }
    }

    /**
     * Retorna a quantidade de segundos até a hora desejada.
     *
     * @param hora - Hora para qual a diferença de tempo deve ser calculada.
     *
     * @return
     *      [0] - Quantidade de segundos até a hora selecionada (Integer).
     *      [1] - Quantidade legivel de tempo até a hora especificada (Integer).
     */
    public static Object[] getTempoAteHora(int hora) {

        if (hora > 24 || hora < 1) {
            return new Integer[0];
        }

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = 60 - calendar.get(Calendar.MINUTE);

        hour = hour < hora ? hora - hour : hora - hour + 24;

        hour = minute == 60 ? hour : hour - 1;
        minute = minute == 60 ? 0 : minute;

        Object[] resposta = new Object[2];

        String tempoLegivel = String.valueOf(hour) + ":" + String.valueOf(minute);

        resposta[0] = (((hour * 60) * 60) + (minute * 60));
        resposta[1] = tempoLegivel;

        return resposta;
    }


    /**
     * Método utilizado para receber arquivos em uma requisição http.
     *
     * @param he - Requisição http
     * @param nomeArquivo - Nome completo que o arquivo receberá
     * @param caminho - Caminho de onde o arquivo será gravado
     * @return
     *      null - Arquivo não recebido
     *      CSPArquivos - Arquivo gravado conforme especificações
     */
    public static CSPArquivos recebeArquivo(HttpExchange he, String nomeArquivo, String caminho) {
        try {

            CSPLog.info("Recebendo arquivo");

            CSPArquivos diretorio = new CSPArquivos(caminho);
            if (!diretorio.exists()) {
                diretorio.mkdirs();
            }

            CSPArquivos ar = new CSPArquivos(caminho + nomeArquivo);

            CSPLog.info("(file:" + ar.getAbsolutePath() + ")...");

            DataInputStream entrada = new DataInputStream(he.getRequestBody());
            FileOutputStream sarq = new FileOutputStream(caminho + nomeArquivo);

            byte[] br = new byte[512];
            int leitura = entrada.read(br);

            while (leitura != -1) {
                if (leitura != -2) {
                    sarq.write(br, 0, leitura);
                }
                leitura = entrada.read(br);
            }
            entrada.close();
            sarq.close();

            CSPLog.info("(size:" + ar.length() + ";file:" + ar.getAbsolutePath() + ")...OK");

            CSPLog.info("...OK");

            return ar;
        } catch (Exception ex) {
            CSPLog.error("Erro na transferência do arquivo");
            CLSException.register(ex);
            return null;
        }
    }
}
