/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.model;

import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangJson;
import br.com.egula.capp.classes.CLSException;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Model de dispositivos.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class Dispositivo {

    /*
    Constantes para identificar o sistema do dispositivo.
     */
    public static final int UNKNOWN = 0;
    public static final int ANDROID = 0;
    public static final int IOS = 1;

    private final long id;
    private final String idHardware;
    private final Date dataCadastro;
    private final String appVersao;
    private final int codVersao;
    private final String modelo;
    private final String fabricante;
    private final String sdkVersao;
    private final int so;
    private final String soVersao;
    private final String telefone;
    private final String operadora;
    private final String appTokenFcm;


    public Dispositivo(long id, String idHardware, Date dataCadastro, int codVersao, String appVersao,  String modelo, String fabricante, String sdkVersao, int so, String soVersao, String telefone, String operadora, String appTokenFcm) {
        this.id = id;
        this.idHardware = idHardware;
        this.dataCadastro = dataCadastro;
        this.appVersao = appVersao;
        this.codVersao = codVersao;
        this.modelo = modelo;
        this.fabricante = fabricante;
        this.sdkVersao = sdkVersao;
        this.so = so;
        this.soVersao = soVersao;
        this.telefone = telefone;
        this.operadora = operadora;
        this.appTokenFcm = appTokenFcm;
    }

    public Dispositivo(long id, String idHardware, int so, int codVersao) {
        this(id, idHardware, null, codVersao, null ,null, null, null, so, null, null, null, null);
    }

    public long getId() {
        return id;
    }

    public String getIdHardware() {
        return idHardware;
    }

    public Date getDataCadastro() {
        return dataCadastro;
    }

    public int getCodVersao() {
        return codVersao;
    }

    public String getAppVersao() {
        return appVersao;
    }

    public String getModelo() {
        return modelo;
    }

    public String getFabricante() {
        return fabricante;
    }

    public String getSdkVersao() {
        return sdkVersao;
    }

    public int getSo() {
        return so;
    }

    public String getSoVersao() {
        return soVersao;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getOperadora() {
        return operadora;
    }

    public String getAppTokenFcm() {
        return appTokenFcm;
    }

    /**
     * Retorna um objeto do dispositivo pelo seu id de hardware
     *
     * @param idHardware - ID do hardware do dispositivo.
     * @param conn       - Conexão com a base de dados.
     * @return - Objeto Dispositivo.
     */
    public static Dispositivo getDispositivo(String idHardware, CSPInstrucoesSQLBase conn) {
        try {

            if (idHardware == null || idHardware.trim().isEmpty() || idHardware.trim().equals("?")) {
                return null;
            }

            idHardware = idHardware.toLowerCase();

            final ResultSet select;
            select = conn.selectOneRow((StringBuilder sb) -> {
                sb.append("SELECT ");
                sb.append("    r.ID, ");
                sb.append("    r.CODIGO_VERSAO, ");
                sb.append("    r.APP_VERSAO, ");
                sb.append("    r.APP_TOKEN_FCM, ");
                sb.append("    r.HORARIO_CADASTRO, ");
                sb.append("    r.MODELO, ");
                sb.append("    r.FABRICANTE, ");
                sb.append("    r.SO, ");
                sb.append("    r.SO_VERSAO, ");
                sb.append("    r.SDK_VERSAO, ");
                sb.append("    r.TELEFONE_NUMERO, ");
                sb.append("    r.TELEFONE_OPERADORA ");
                sb.append("FROM ");
                sb.append("    VW_INFOS_DISPOSITIVO r ");
                sb.append("INNER join PR_RETORNA_CADASTRO_DISPOSITIVO(?) p ");
                sb.append("  on p.ID = r.ID ");

            }, idHardware);

            if (select != null) {
                return new Dispositivo(
                        select.getLong("ID"),
                        idHardware,
                        select.getDate("HORARIO_CADASTRO"),
                        (select.getString("CODIGO_VERSAO") != null) ? select.getInt("CODIGO_VERSAO") : -1 ,
                        select.getString("APP_VERSAO"),
                        select.getString("MODELO"),
                        select.getString("FABRICANTE"),
                        select.getString("SDK_VERSAO"),
                        select.getInt("SO"),
                        select.getString("SO_VERSAO"),
                        select.getString("TELEFONE_NUMERO"),
                        select.getString("TELEFONE_OPERADORA"),
                        select.getString("APP_TOKEN_FCM")
                );
            }

        } catch (SQLException e) {
            CLSException.register(e);
        }

        return null;
    }

    /**
     * Captura e registra na base a versão do app do dispositivo
     *
     * @param dev    - Objeto do Dispositivo.
     * @param input  - JSON de entrada dos dados.
     * @param hostIP - Host da conexão feita pelo dispositivo.
     * @param conn   - Conexão com a base de dados.
     * @throws java.lang.Exception
     */
    public static void registraAppVersao(Dispositivo dev, JSONObject input, String hostIP, CSPInstrucoesSQLBase conn) throws Exception {

        final String versaoApp = CSPUtilidadesLangJson.getFromJson(input, "app_versao", (String) null);
        final int versaoCodigo = CSPUtilidadesLangJson.getFromJson(input, "cod_versao", -1);

        if (versaoApp == null || versaoCodigo == -1) {
            return;
        }

        conn.execute("EXECUTE PROCEDURE PR_REGISTRA_APP_VERSAO (?, ?, ?, ?);",
                dev.getId(),
                hostIP,
                versaoCodigo,
                versaoApp
        );

    }

    /**
     * Captura e registra na base o token fcm do app do dispositivo
     *
     * @param dev    - Objeto do Dispositivo.
     * @param input  - JSON de entrada dos dados.
     * @param hostIP - Host da conexão feita pelo dispositivo.
     * @param conn   - Conexão com a base de dados.
     * @throws java.lang.Exception
     */
    public static void registraAppTokenFcm(Dispositivo dev, JSONObject input, String hostIP, CSPInstrucoesSQLBase conn) throws Exception {

        String token = CSPUtilidadesLangJson.getFromJson(input, "APP_TOKEN_FCM_DEV", (String) null);

        if (token == null) {
            return;
        }

        /*
         * Algumas versões do fcm estão retornando um json no lugar
         * do token(string).
         *
         * Para corrigir isso precisamos avaliar se é um json e caso for coletar
         * o token dentro dele
         */
        if (CSPUtilidadesLangJson.isJson(token)) {
            token = CSPUtilidadesLangJson.getFromJson(new JSONObject(token), "token", (String) null);

            if (token == null) {
                return;
            }
        }

        conn.execute(
                "EXECUTE PROCEDURE PR_REGISTRA_APP_TOKEN_FCM (?, ?,?);",
                dev.getId(),
                hostIP,
                token
        );
    }

    /**
     * Captura e registra na base as informações gerais sobre o dispositivo
     *
     * @param dev   - Objeto do Dispositivo.
     * @param input - JSON de entrada dos dados.
     * @param conn  - Conexão com a base de dados.
     * @throws java.lang.Exception
     */
    public static void registraInfosDev(Dispositivo dev, JSONObject input, CSPInstrucoesSQLBase conn) throws Exception {

        conn.execute(
                "EXECUTE PROCEDURE PR_UP_DEV_INFOS_EXTRA (?, ?, ?, ?, ?, ?, ?)",
                dev.getId(),
                CSPUtilidadesLangJson.getFromJson(input, "MODEL_DEV", (String) null),
                CSPUtilidadesLangJson.getFromJson(input, "MANUFACTURER_DEV", (String) null),
                CSPUtilidadesLangJson.getFromJson(input, "TELEFONE_DEV", (String) null),
                CSPUtilidadesLangJson.getFromJson(input, "OPERADORA_DEV", (String) null),
                CSPUtilidadesLangJson.getFromJson(input, "SO_VERSION_DEV", (String) null),
                CSPUtilidadesLangJson.getFromJson(input, "SDK_DEV", (String) null)
        );
    }
}