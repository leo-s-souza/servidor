/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.classes;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;

import java.sql.SQLException;
import java.util.LinkedHashMap;

/**
 * Classe utilitária básica do CAPP.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public abstract class CLSCapp {

    /**
     * Caminho da pasta de informações dos dispositivos.
     */
    public final static String PATH_INFOS = "/opt/capp/infos-app";

    /**
     * ID do projeto e-GULA no Firebase. Utilizamos para fazer modificações no banco de dados Cloud Firestore.
     */
    public final static String PATH_FIREBASE_SERVICE_ACCOUNT = "/opt/capp2/private/e-gula-159652b206b4.json";

    /**
     * Chave de utilização de diversas APIs da Google. No momento, as APIs
     * habilitadas dizem respeito, apenas, à funções do Google Maps, mas podemos
     * estende-las para outras funcionalidades.
     */
    public final static String KEY_GOOGLE_API = "AIzaSyDdB8k2l3jdLzpCBQqLR08h0kqPtxT9QuU";

    /**
     * Lista utilizada para que um mesmo usuário não possa fazer uma execução de módulo que já esteja sendo feita.
     * Como um usuário por algum fator enviar dois pedidos iguais ao mesmo tempo, sendo que deveria haver apenas um.
     */
    public static LinkedHashMap<Integer, Object[]> listaModuloPessoa = new LinkedHashMap<>();


    /**
     * Retorna uma nova conexão padrão com a base app.
     *
     * @return Nova conexão com a APP_FOOD.
     */
    public static CSPInstrucoesSQLBase connDb() throws SQLException {
        try {
//            return CLSUtilidadesServidores.getConexaoBase();
            return new CSPInstrucoesSQLBase("localhost", "/opt/capp/bases/APP_FOOD.fdb", "SYSDBA", "cri$$@21", 3050, "UTF8");

        } catch (Exception e) {
            CSPLog.error(CLSCapp.class, "Erro ao tentar obter a classe de manipulação da base APP_FOOD.");
        }

        throw new SQLException();
    }
}
