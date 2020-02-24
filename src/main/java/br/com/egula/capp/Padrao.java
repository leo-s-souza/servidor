/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.FrmModuloPaiBase;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLang;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.classes.CLSPendencias;
import br.com.egula.capp.classes.CLSUtilidades;
import br.com.egula.capp.classes.CLSUtilidadesServidores;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Locale;

import static br.com.egula.capp.classes.CLSCapp.PATH_FIREBASE_SERVICE_ACCOUNT;

/**
 * Classe de execução principal do servidor.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class Padrao {

    /**
     * Método main.
     *
     * @param args Argumentos da linha de comando.
     */
    public static void main(String[] args) {
        new Padrao();
    }

    /**
     * Classe Padrão de execução do CAPP.
     */
    private Padrao() {
        try {

            // Inicia os Logs.
            CSPLog.startLog(Padrao.class);

            // Define o padrão de números monetários usado no Brasil.
            {
                NumberFormat n = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
                n.setMinimumFractionDigits(2);
                n.setMaximumFractionDigits(2);
                CSPUtilidadesLang.setDefaultCurrencyConf(n);
            }

            CSPInstrucoesSQLBase.setEnableAlteracaoGold(false);
            CSPInstrucoesSQLBase.setEnableGenerateSqlLogs(false);
            CSPInstrucoesSQLBase.setInfosBase(CSPInstrucoesSQLBase.Bases.BASE_APP, "35.199.104.29", "/opt/capp/bases/APP_FOOD.fdb", "SYSDBA", "cri$$@21", 3050, "UTF8");

            // Inicialização do SDK do Firebase
            {
                File file = new File(PATH_FIREBASE_SERVICE_ACCOUNT);
                InputStream serviceAccount = new FileInputStream(file);
                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);
            }

            // Rest
            FrmModuloPaiBase.runModuleCapp(ModuloRest.class);

            agendaRestartAutomatico();

        } catch (Exception ex) {
            CLSException.register(ex);
        }
    }

    /**
     * Agenda o processo de restart automático do capp as 17:00 horas e as 05:00 horas
     */
    private void agendaRestartAutomatico() {

        Object[] tempoAte5 = CLSUtilidades.getTempoAteHora(5);
        Object[] tempoAte17 = CLSUtilidades.getTempoAteHora(17);

        Object[] tempoAteExecucao = ((Integer) tempoAte5[0] < (Integer) tempoAte17[0]) ? tempoAte5 : tempoAte17;

        CSPLog.info("Restart do capp agendado para daqui a " + tempoAteExecucao[1]);

        FrmModuloPaiBase.executor((executor) -> {
            System.exit(-1);
            executor.shutdown();
        }, (Integer) tempoAteExecucao[0], 10);
    }
}