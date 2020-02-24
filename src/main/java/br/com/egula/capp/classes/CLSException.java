/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.classes;

import br.com.casaautomacao.casagold.classes.CSPLog;
import org.firebirdsql.jdbc.FBSQLException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesSO.LINE_SEPARATOR;

/**
 * Classe responsável por registar no Log todas as exceptions geradas pelo sistema e passadas para essa classe.
 * As exceptions aparecem formatadas no Log.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class CLSException {

    /**
     * Registra uma Exception
     *
     * @param ex Exception ocorrida.
     */
    public static void register(Throwable ex) {
        if (ex.getClass().equals(FBSQLException.class)) {
            CSPLog.error("Problem with last SQl executed. " + trataException(ex));

        } else {
            CSPLog.error("Erro (exception): " + trataException(ex));
        }
    }

    /**
     * Formata a exception para exibição no log.
     *
     * @param ex Exception ocorrida.
     * @return Retorna uma string com a Exception formatada.
     */
    private static String trataException(Throwable ex) {
        String exM = LINE_SEPARATOR;
        exM += "        Message: " + ex.getMessage() + LINE_SEPARATOR;
        exM += "        Localized Message: " + ex.getLocalizedMessage() + LINE_SEPARATOR;
        exM += "        Trace:" + LINE_SEPARATOR;
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        exM += "		" + errors.toString().replace("	", "		");
        return exM;
    }
}