/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.classes;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.bancodados.CSPInstrucoesSQLBase;
import br.com.casaautomacao.casagold.classes.exceptions.CSPException;
import br.com.egula.capp.model.Notificacao;
import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.auth.ApnsSigningKey;
import com.turo.pushy.apns.util.ApnsPayloadBuilder;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import com.turo.pushy.apns.util.TokenUtil;
import com.turo.pushy.apns.util.concurrent.PushNotificationFuture;

import java.io.File;
import java.sql.ResultSet;
import java.util.concurrent.ExecutionException;

public class CLSNotificacaoApns {

    /**
     *
     * @param notificacao
     * @param idHardware
     * @param conn
     * @throws Exception
     */
    public static void pushNotificationAppApns(Notificacao notificacao, String idHardware, CSPInstrucoesSQLBase conn) throws Exception {

        if(!idHardware.startsWith("io")){
            return;
        }

        ResultSet rs = conn.select((sb -> {
            sb.append("SELECT ");
            sb.append("    a.TOKEN ");
            sb.append("FROM ");
            sb.append("    VW_DISPOSITIVO_IOS_APNS_TOKEN a ");
            sb.append("WHERE ");
            sb.append("    a.ID_HARDWARE = ?");
        }), idHardware);

        if(rs.next()){

            final ApnsClient apnsClient = new ApnsClientBuilder()
                    .setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
//                    .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File("/opt/capp2/AuthKey_2SKXYT5K9H.p8"),
                            "57Q4MX88DB", "2SKXYT5K9H"))
                    .build();

            final SimpleApnsPushNotification pushNotification;

            {
                final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
                payloadBuilder.setAlertTitle(notificacao.getTitulo());
                payloadBuilder.setAlertBody(notificacao.getMensagem());
                payloadBuilder.setSound("default");

                final String payload = payloadBuilder.buildWithDefaultMaximumLength();
                final String token = TokenUtil.sanitizeTokenString(rs.getString("TOKEN"));

                pushNotification = new SimpleApnsPushNotification(token, "brcomegulaios", payload);
            }

            final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
                    sendNotificationFuture = apnsClient.sendNotification(pushNotification);

            try {
                final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse =
                        sendNotificationFuture.get();

                if (pushNotificationResponse.isAccepted()) {
                    CSPLog.info("Push notification accepted by APNs gateway.");
                } else {
                    CSPLog.info("Notification rejected by the APNs gateway: " +
                            pushNotificationResponse.getRejectionReason());

                    if (pushNotificationResponse.getTokenInvalidationTimestamp() != null) {
                        CSPLog.info("\t…and the token is invalid as of " +
                                pushNotificationResponse.getTokenInvalidationTimestamp());
                    }
                }
            } catch (final ExecutionException e) {
                CSPLog.info("Failed to send push notification.");
                CSPException.register(e);
            }
        }

    }
}
