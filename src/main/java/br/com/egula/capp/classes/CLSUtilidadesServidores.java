/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.classes;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.exceptions.CSPException;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangRede;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.Query;
import com.google.firebase.cloud.FirestoreClient;

import java.sql.SQLException;
import java.util.Arrays;

//import static br.com.egula.capp.rest.BackupBasePrincipal.startBackup;

public class CLSUtilidadesServidores {

//    /**
//     * A primeira posição do array deve sempre ser o host do servidor principal
//     */
//    private static String[] hosts;
//    private static boolean servidorPrincipal;
//
//    private static ListenerRegistration registration;
//
//    /**
//     * Inicia as informações principais para a interação entre servidores.
//     */
//    public static void iniciaInfoServidores() {
//        Firestore database = FirestoreClient.getFirestore();
//        Query query = database.collection("servidores").whereEqualTo("ativo", true);
//
//        registration = query.addSnapshotListener((value, error) -> {
//
//            if(error != null){
//                CSPLog.error(CLSUtilidadesServidores.class, error.getMessage());
//            }
//
//            if(value != null){
//                for (DocumentSnapshot document : value.getDocuments()) {
//                    hosts = document.get("servers").toString().replace("[", "").replace("]", "").split(", ");
//                    CSPLog.info("Lista de servers - " + Arrays.toString(hosts));
//
//                    //Inicia processo de backup automático da base APP_FOOD.
////                    startBackup();
//
//                }
//            }
//
//            try {
//                servidorPrincipal = hosts[0].equals(CSPUtilidadesLangRede.getExternalIp());
//
////                if(!servidorPrincipal){
////                    CLSPendencias.resolvePendencias();
////                }
//            } catch (Exception e) {
//                CSPException.register(e);
//                stopLiestener();
//            }
//
//            stopLiestener();
//        });
//    }
//
//    private static void stopLiestener(){
//        registration.remove();
//    }
//
//    /**
//     * Retorna a conexão com a base.
//     *
//     * Primeiro é feito a tentativa de conexão com a base principal.
//     *
//     * Caso não seja possivel é iniciada a conexão com a base local.
//     *
//     * @return
//     * @throws SQLException
//     */
//    public static CSPInstrucoesSQLBase getConexaoBase() throws Exception {
//
//        String host = (isServidorPrincipal()) ? "localhost" : hosts[0];
//
//        CSPInstrucoesSQLBase conn = new CSPInstrucoesSQLBase(host, "/opt/capp/bases/APP_FOOD.fdb", "SYSDBA", "cri$$@21", 3050, "UTF8");
//
//        return conn;
//    }
//
//    public static String[] getHosts() {
//        return hosts;
//    }
//
//    public static String getHostPrincipal(){
//        return hosts[0];
//    }
//
//    public static boolean isServidorPrincipal() {
//        return servidorPrincipal;
//    }
}
