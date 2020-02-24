/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.classes;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.utilidades.CSPUtilidadesLangRede;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static br.com.egula.capp.classes.CLSCapp.connDb;

/**
 * Classe utilitária para disparo de notificações via FCM.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class CLSNotificacao {
    /**
     * Registra na base e dispara notificações via FCM para usuários
     * específicos.
     *
     * @param notificacao Notificacao - Notificacao que deve ser gerada.◊
     * @param idUsuario   Long - Id do usuário que deve ser notificado! (Pode ser
     *                    null em caso de notificações de monitoramento).
     * @param parametros  Object... - Parametros da procedure da notificação. EM
     *                    ORDEM!
     */
    public synchronized static void registraNotificacaoUsuariosFCM(Notificacao notificacao, Long idUsuario, Object... parametros) {

        try {
            final CSPInstrucoesSQLBase connNotificacoesUser = connDb();

            if (notificacao.isMonitoramento()) {
                ArrayList<Long> idUsers = new ArrayList<>();

                ResultSet rs = connNotificacoesUser.select("SELECT r.USUARIO_ID FROM VW_USUARIO_MONITORAMENTO r");

                while (rs.next()) {
                    idUsers.add(rs.getLong("USUARIO_ID"));
                }

                for (Long idUser : idUsers) {
                    rs = connNotificacoesUser.select((StringBuilder sb) -> {
                        sb.append("SELECT ");
                        sb.append("     ID, ");
                        sb.append("     CONTEUDO, ");
                        sb.append("     TITULO, ");
                        sb.append("     TIPO, ");
                        sb.append("     RESP, ");
                        if (notificacao.equals(Notificacao.B1) || notificacao.equals(Notificacao.B3)) {
                            sb.append("     BL, ");
                            sb.append("     BB, ");
                        }
                        sb.append("     TOKEN ");
                        sb.append("FROM ");
                        sb.append(notificacao.getProcedure());
                        sb.append("(");
                        sb.append(String.valueOf(idUser));
                        for (int i = 0; i < parametros.length; i++) {
                            sb.append(", ?");
                        }
                        sb.append(")");
                    }, parametros);

                    if (rs.next()) {
                        if (rs.getString("TOKEN") != null) {
                            try {
                                String conteudoMs = rs.getString("CONTEUDO") +
                                        "\nCapp: " +
                                        CSPUtilidadesLangRede.getExternalIp();

                                CSPLog.info(CLSNotificacao.class, "controle-fcm(" + rs.getLong("ID") + ")...");

                                CLSNotificacao.disparaFCMMonitoramento(
                                        rs.getLong("ID"),
                                        rs.getString("TITULO"),
                                        conteudoMs.replace("<n>", "\n"),
                                        rs.getInt("TIPO"),
                                        rs.getString("RESP"),
                                        notificacao.equals(Notificacao.B1) || notificacao.equals(Notificacao.B3) ? rs.getDouble("BL") : 0,
                                        notificacao.equals(Notificacao.B1) || notificacao.equals(Notificacao.B3) ? rs.getDouble("BB") : 0,
                                        rs.getString("TOKEN"));
                                CSPLog.info(CLSNotificacao.class, "controle-fcm(" + rs.getLong("ID") + ")...OK");

                            } catch (SQLException e) {
                                CSPLog.error(CLSNotificacao.class, "controle-fcm(" + rs.getLong("ID") + ")...NO");
                                CLSException.register(e);
                            }
                        }
                    }
                }

            } else if (idUsuario != null && idUsuario > 0) {
                ResultSet rs = connNotificacoesUser.select((StringBuilder sb) -> {
                    sb.append("SELECT ");
                    sb.append("     ID, ");
                    sb.append("     CONTEUDO, ");
                    sb.append("     TITULO, ");
                    sb.append("     TIPO, ");
                    sb.append("     RESP, ");
                    if (notificacao.equals(Notificacao.B1) || notificacao.equals(Notificacao.B3)) {
                        sb.append("     BL, ");
                        sb.append("     BB, ");
                    }
                    sb.append("     TOKEN ");
                    sb.append("FROM ");
                    sb.append(notificacao.getProcedure());
                    sb.append("(");
                    sb.append(String.valueOf(idUsuario));
                    for (int i = 0; i < parametros.length; i++) {
                        sb.append(",?");
                    }
                    sb.append(")");
                }, parametros);

                if (rs.next()) {
                    if (rs.getString("TOKEN") != null) {
                        try {

                            CSPLog.info(CLSNotificacao.class, "controle-fcm(" + rs.getLong("ID") + ")...");

                            CLSNotificacao.disparaFCMMonitoramento(
                                    rs.getLong("ID"),
                                    rs.getString("TITULO"),
                                    rs.getString("CONTEUDO").replace("<n>", "\n"),
                                    rs.getInt("TIPO"),
                                    rs.getString("RESP"),
                                    notificacao.equals(Notificacao.B1) || notificacao.equals(Notificacao.B3) ? rs.getDouble("BL") : 0,
                                    notificacao.equals(Notificacao.B1) || notificacao.equals(Notificacao.B3) ? rs.getDouble("BB") : 0,
                                    rs.getString("TOKEN"));

                            CSPLog.info(CLSNotificacao.class, "controle-fcm(" + rs.getLong("ID") + ")...OK");

                        } catch (SQLException e) {
                            CSPLog.error(CLSNotificacao.class, "controle-fcm(" + rs.getLong("ID") + ")...NO");
                            CLSException.register(e);
                        }
                    }
                }
            }

            connNotificacoesUser.close();

        } catch (Exception e) {
            CLSException.register(e);
        }
    }

    /**
     * Envia a notificação para o FCM.
     *
     * @param idNotificacao          Long - ID da notificação.
     * @param tituloNotificacao      String - Título da notificação.
     * @param conteudoNotificacao    String - Conteúdo da notificação.
     * @param tipoNotificacao        int - Tipo da notificação.
     * @param responsavelNotificacao String - CNPJ da loja responsável pela notificação.
     * @param bonusLivre
     * @param bonusBloqueado
     * @param tokenDispositivos      String - Token FCM dos dispositivos que deverão
     *                               receber a notificação.
     */
    private static void disparaFCMMonitoramento(long idNotificacao, String tituloNotificacao, String conteudoNotificacao, int tipoNotificacao, String responsavelNotificacao, double bonusLivre, double bonusBloqueado, String... tokenDispositivos) {
        if (tokenDispositivos != null && tokenDispositivos.length > 0 && idNotificacao > 0) {

            HttpURLConnection conn = null;
            OutputStreamWriter wr = null;
            BufferedReader br = null;
            InputStreamReader isr = null;

            final String AUTH_KEY = "AAAAkCQHJ6E:APA91bE4cn--dz1U6rSiIaRc_wDM4Uos4RLl07sbu-JPXEVa4vMLSoGWYr1u0rU5U_2PCMLl_g2XNSNyG0VECuUrwWpwXEbU-MqyTZqMegKtxpWER3MRLVtB1bCOfe8s9kcn1Q6ObMkVDWMojdeBM4a3RU_kqfIWnA";
            final String URL = "https://fcm.googleapis.com/fcm/send";

            try {
                conn = (HttpURLConnection) new URL(URL).openConnection();
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "key=" + AUTH_KEY);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject json = new JSONObject();
                json.put("registration_ids", tokenDispositivos);
                json.put("priority", "high");
                json.put("data", new JSONObject() {
                    {
                        put("id", String.valueOf(idNotificacao));
                        put("titulo", tituloNotificacao);
                        put("conteudo", conteudoNotificacao);
                        put("tipo", String.valueOf(tipoNotificacao));
                        put("resp", responsavelNotificacao);

                        if (bonusLivre != 0 || bonusBloqueado != 0) {
                            put("bl", bonusLivre);
                            put("bb", bonusBloqueado);
                        }
                    }
                });

                //Envia o request.
                CSPLog.info(json.toString());
                wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(json.toString());
                wr.flush();

                //Lê a resposta.
                isr = new InputStreamReader(conn.getInputStream());
                br = new BufferedReader(isr);

                CSPLog.info("Output Server FCM.... \n");

                String output;
                while ((output = br.readLine()) != null) {
                    CSPLog.info(output);
                }

                CSPLog.info("Notificacao enviada com sucesso ao FCM");

            } catch (IOException | JSONException e) {
                CLSException.register(e);
                CSPLog.error("problema ao enviar notificacao via FCM, consulte o log");

                //Garante que tudo será fechado!
            } finally {
                try {
                    if (conn != null) {
                        conn.disconnect();
                    }
                    if (wr != null) {
                        wr.close();
                    }

                    if (br != null) {
                        br.close();
                    }

                    if (isr != null) {
                        isr.close();
                    }
                } catch (IOException e) {
                    CLSException.register(e);
                }
            }
        }
    }

    public enum Notificacao {
        B1("PR_REGISTRA_NOTI_B_1", false),
        B2("PR_REGISTRA_NOTI_B_2", false),
        B3("PR_REGISTRA_NOTI_B_3", false),
        C1("PR_REGISTRA_NOTI_C_1", false),
        P1("PR_REGISTRA_NOTI_P_1", false),
        P2("PR_REGISTRA_NOTI_P_2", true),
        P3("PR_REGISTRA_NOTI_P_3", true),
        P4("PR_REGISTRA_NOTI_P_4", true),
        P5("PR_REGISTRA_NOTI_P_5", false),
        P6("PR_REGISTRA_NOTI_P_6", false),
        START("PR_REGISTRA_NOTI_START", true);

        private final String procedure;
        private final boolean isMonitoramento;

        /**
         * @param procedure
         * @param isMonitoramento
         */
        Notificacao(String procedure, boolean isMonitoramento) {
            this.procedure = procedure;
            this.isMonitoramento = isMonitoramento;
        }

        /**
         * @return
         */
        public boolean isMonitoramento() {
            return this.isMonitoramento;
        }

        /**
         * @return
         */
        public String getProcedure() {
            return this.procedure;
        }
    }
}
