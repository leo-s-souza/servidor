/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.classes;

import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Blob;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ExportaBonusFirabaseBase {

    public static void processaBonusFirebaseBase() throws Exception {

//        CSPInstrucoesSQLBase conn = new CSPInstrucoesSQLBase("localhost", "/Users/casa/teste/logs_capp/APP_FOOD.fdb", "SYSDBA", "masterkey", 3050, "UTF8");

        CSPInstrucoesSQLBase conn = new CSPInstrucoesSQLBase("localhost", "/opt/capp/bases/APP_FOOD.fdb", "SYSDBA", "cri$$@21", 3050, "UTF8");
//        executaFinalizaçãoBase(conn);

        Firestore database = FirestoreClient.getFirestore();

        // Create a reference to the cities collection
        CollectionReference usuarios = database.collection("usuario");
        // Create a query against the collection.
        Query query = usuarios.limit(10000000).orderBy("id");//whereLessThan("id", 10000000).orderBy("id");

        // retrieve  query results asynchronously using query.get()
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            System.out.println(document.getLong("id") + " - Bônus = " + document.getDouble("bonusLivre"));

            Double bonus = (document.getDouble("bonusLivre") != null) ? document.getDouble("bonusLivre") : 0;

            conn.insertComposto("USUARIO_BONUS", new HashMap<String, Object>() {
                {
                    put("USUARIO_ID", document.getLong("id"));
                    put("QUANT_BONUS", bonus);
                }
            });
        }

        executaFinalizacaoBase(conn);
    }

    private static void executaFinalizacaoBase(CSPInstrucoesSQLBase conn) throws Exception {

        CSPInstrucoesSQLBase conn2 = new CSPInstrucoesSQLBase("localhost", "/opt/capp/bases/APP_FOOD.fdb", "SYSDBA", "cri$$@21", 3050, "UTF8");

        ResultSet result = conn.select(sb -> {
            sb.append("SELECT ");
            sb.append("    a.ID, ");
            sb.append("    a.TOTAL_BONUS_LIVRE, ");
            sb.append("    coalesce(ua.QUANT_BONUS, -1) as QUANT_BONUS ");
            sb.append("FROM ");
            sb.append("    VW_INFOS_USUARIO a ");
            sb.append("left join ");
            sb.append("    USUARIO_BONUS ua ON a.ID = ua.USUARIO_ID ");
        });

        while (result.next()) {
            if(result.getDouble("QUANT_BONUS") == -1){
                conn2.insertComposto("USUARIO_BONUS", new HashMap<String, Object>() {
                    {
                        put("USUARIO_ID", result.getInt("ID"));
                        put("QUANT_BONUS", result.getDouble("TOTAL_BONUS_LIVRE"));
                    }
                });
            }
        }
    }
}
