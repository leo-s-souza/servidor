/*
 * Copyright (c) 2019. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.FrmModuloPaiBase;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.classes.CLSUtilidadesServidores;
import com.jcraft.jsch.*;

import java.io.*;
import java.util.*;

public class BackupBasePrincipal {

//    private static boolean threadIniciada = false;
//
//    /**
//     * Da inicio ao processo de backup da base APP_FOOD do servidor principal
//     *
//     * @throws Exception
//     */
//    public static void startBackup() {
//        if(CLSUtilidadesServidores.isServidorPrincipal()){
//            if(!threadIniciada){
//                executaProcessoBackup(CLSUtilidadesServidores.getHosts()[1]);
//            }
//        }
//    }
//
//    /**
//     * Faz a conexão com o servidor que tem a base e efetua a cópia nos horarios 12:00 e 00:00
//     */
//    private static void executaProcessoBackup(String host){
//
//        int threadDelay = getSecondsUntilTarget(12);
//
//        //Variavel utilizada para definir em quanto tempo a thread deve ser executada.
//        threadDelay = (threadDelay <= 43200) ? threadDelay : getSecondsUntilTarget(24);
//
//        FrmModuloPaiBase.executor((executor) -> {
//
//            CSPLog.info(BackupBasePrincipal.class, "Inicia o processo de cópia da base principal no ip " + host);
//
//            try {
//                String remoteA = "/opt/capp/bases/backup/APP_FOOD.fdb";
//                String local = "/opt/capp/bases/";
//                String fileName = "APP_FOOD.fdb";
//
//                String user = "casa";
//                int port = 53133;
//
//                String keyFilePath = "/home/casaautomacao_oficial/.ssh/id_rsa";
//                String keyPassword = "cri$$@2001";
//
//                Session session = createSession(user, host, port, keyFilePath, keyPassword);
//
//                if(session != null){
//                    copyLocalToRemote(session, local, remoteA, fileName);
//                }
//            } catch (Exception e) {
//                CLSException.register(e);
//            }
//
//            CSPLog.info(BackupBasePrincipal.class, "Finaliza o processo de cópia da base principal no ip " + host);
//
//        }, threadDelay, 43200);
//
//        threadIniciada = true;
//    }
//
//    /**
//     * Retorna a quantidade de segundos até a hora desejada para a execução da thread.
//     *
//     * @param targetHour
//     * @return
//     */
//    private static int getSecondsUntilTarget(int targetHour) {
//        Calendar calendar = Calendar.getInstance();
//        int hour = calendar.get(Calendar.HOUR_OF_DAY);
//        int minute = 60 - calendar.get(Calendar.MINUTE);
//
//        hour = hour < targetHour ? targetHour - hour : targetHour - hour + 24;
//
//        hour = minute == 60 ? hour : hour - 1;
//        minute = minute == 60 ? 0 : minute;
//
//        CSPLog.info(BackupBasePrincipal.class, "Backup da base principal vai ser feito em " + hour + " horas e " + minute + " minutos");
//
//        return (((hour * 60) * 60) + (minute * 60));
//    }
//
//    /**
//     * Cria uma sessão ssh entre a máquina com o executável e o host recebido por parametro.
//     *
//     * Código original em: https://medium.com/@ldclakmal/scp-with-java-b7b7dbcdbc85
//     *
//     * @param user - usuário do host externo
//     * @param host - endereço do host externo
//     * @param port - porta para comunicação do host externo
//     * @param keyFilePath - caminho do arquivo chave (id_rsa).
//     * @param keyPassword - password do arquivo chave.
//     * @return - retorna a seção
//     */
//    private static Session createSession(String user, String host, int port, String keyFilePath, String keyPassword) {
//        try {
//            JSch jsch = new JSch();
//
//            if (keyFilePath != null) {
//                if (keyPassword != null) {
//                    jsch.addIdentity(keyFilePath, keyPassword);
//                } else {
//                    jsch.addIdentity(keyFilePath);
//                }
//            }
//
//            Properties config = new java.util.Properties();
//            config.put("StrictHostKeyChecking", "no");
//
//            Session session = jsch.getSession(user, host, port);
//            session.setConfig(config);
//            session.connect();
//
//            return session;
//        } catch (JSchException e) {
//            CLSException.register(e);
//            return null;
//        }
//    }
//
//    /**
//     * Copia um arquivo de um host externo para a máquina atual.
//     *
//     * Código original em: https://medium.com/@ldclakmal/scp-with-java-b7b7dbcdbc85
//     *
//     * @param session - sessão ssh criada em createSession(String user, String host, int port, String keyFilePath, String keyPassword)
//     * @param from - De onde o arquivo vai ser copiado.
//     * @param to - Para onde o arquivo vai ser copiado.
//     * @param fileName - Nome do arquivo que vai ser copiado.
//     * @throws JSchException
//     * @throws IOException
//     */
//    private static void copyRemoteToLocal(Session session, String from, String to, String fileName) throws JSchException, IOException {
//        from = from + File.separator + fileName;
//        String prefix = null;
//
//        if (new File(to).isDirectory()) {
//            prefix = to + File.separator;
//        }
//
//        // exec 'scp -f rfile' remotely
//        String command = "scp -f " + from;
//        Channel channel = session.openChannel("exec");
//        ((ChannelExec) channel).setCommand(command);
//
//        // get I/O streams for remote scp
//        OutputStream out = channel.getOutputStream();
//        InputStream in = channel.getInputStream();
//
//        channel.connect();
//
//        byte[] buf = new byte[1024];
//
//        // send '\0'
//        buf[0] = 0;
//        out.write(buf, 0, 1);
//        out.flush();
//
//        while (true) {
//            int c = checkAck(in);
//            if (c != 'C') {
//                break;
//            }
//
//            // read '0644 '
//            in.read(buf, 0, 5);
//
//            long filesize = 0L;
//            while (true) {
//                if (in.read(buf, 0, 1) < 0) {
//                    // error
//                    break;
//                }
//                if (buf[0] == ' ') break;
//                filesize = filesize * 10L + (long) (buf[0] - '0');
//            }
//
//            String file = null;
//            for (int i = 0; ; i++) {
//                in.read(buf, i, 1);
//                if (buf[i] == (byte) 0x0a) {
//                    file = new String(buf, 0, i);
//                    break;
//                }
//            }
//
//            CSPLog.info(BackupBasePrincipal.class, "file-size=" + filesize + ", file=" + file);
//
//            // send '\0'
//            buf[0] = 0;
//            out.write(buf, 0, 1);
//            out.flush();
//
//            // read a content of lfile
//            FileOutputStream fos = new FileOutputStream(prefix == null ? to : prefix + file);
//            int foo;
//            while (true) {
//                if (buf.length < filesize) foo = buf.length;
//                else foo = (int) filesize;
//                foo = in.read(buf, 0, foo);
//                if (foo < 0) {
//                    // error
//                    break;
//                }
//                fos.write(buf, 0, foo);
//                filesize -= foo;
//                if (filesize == 0L) break;
//            }
//
//            if (checkAck(in) != 0) {
//                System.exit(0);
//            }
//
//            // send '\0'
//            buf[0] = 0;
//            out.write(buf, 0, 1);
//            out.flush();
//
//            try {
//                fos.close();
//            } catch (Exception ex) {
//                CLSException.register(ex);
//            }
//        }
//
//        channel.disconnect();
//        session.disconnect();
//    }
//
//    /**
//     * Copia um arquivo da máquina atual para um um host externo.
//     *
//     * Código original em: https://medium.com/@ldclakmal/scp-with-java-b7b7dbcdbc85
//     *
//     * @param session - sessão ssh criada em createSession(String user, String host, int port, String keyFilePath, String keyPassword)
//     * @param from - De onde o arquivo vai ser copiado.
//     * @param to - Para onde o arquivo vai ser copiado.
//     * @param fileName - Nome do arquivo que vai ser copiado.
//     * @throws JSchException
//     * @throws IOException
//     */
//    private static void copyLocalToRemote(Session session, String from, String to, String fileName) throws JSchException, IOException {
//
//        // exec 'scp -t rfile' remotely
//        String command = "scp " + "-p" + " -t " + to;
//        Channel channel = session.openChannel("exec");
//        ((ChannelExec) channel).setCommand(command);
//
//        try {
//            from = from + File.separator + fileName;
//
//            // get I/O streams for remote scp
//            OutputStream out = channel.getOutputStream();
//            InputStream in = channel.getInputStream();
//
//            channel.connect();
//
//            if (checkAck(in) != 0) {
//                System.exit(0);
//            }
//
//            File _lfile = new File(from);
//
//            command = "T" + (_lfile.lastModified() / 1000) + " 0";
//            // The access time should be sent here,
//            // but it is not accessible with JavaAPI ;-<
//            command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
//            out.write(command.getBytes());
//            out.flush();
//
//            if (checkAck(in) != 0) {
//                System.exit(0);
//            }
//
//            // send "C0644 filesize filename", where filename should not include '/'
//            long filesize = _lfile.length();
//            command = "C0644 " + filesize + " ";
//            if (from.lastIndexOf('/') > 0) {
//                command += from.substring(from.lastIndexOf('/') + 1);
//            } else {
//                command += from;
//            }
//
//            command += "\n";
//            out.write(command.getBytes());
//            out.flush();
//
//            if (checkAck(in) != 0) {
//                System.exit(0);
//            }
//
//            // send a content of lfile
//            FileInputStream fis = new FileInputStream(from);
//            byte[] buf = new byte[1024];
//            while (true) {
//                int len = fis.read(buf, 0, buf.length);
//                if (len <= 0) break;
//                out.write(buf, 0, len); //out.flush();
//            }
//
//            // send '\0'
//            buf[0] = 0;
//            out.write(buf, 0, 1);
//            out.flush();
//
//            if (checkAck(in) != 0) {
//                System.exit(0);
//            }
//            out.close();
//
//                fis.close();
//
//            channel.disconnect();
//            session.disconnect();
//        } catch (Exception ex) {
//
//            channel.disconnect();
//            session.disconnect();
//
//            CLSException.register(ex);
//        }
//    }
//
//    /**
//     * Checa a integridade do arquivo em inputStream.
//     * @param in - InputStream do arquivo.
//     * @return
//     * @throws IOException
//     */
//    public static int checkAck(InputStream in) {
//        try {
//
//            int b = in.read();
//            // b may be 0 for success,
//            //          1 for error,
//            //          2 for fatal error,
//            //         -1
//            if (b == 0) return b;
//            if (b == -1) return b;
//
//            if (b == 1 || b == 2) {
//                StringBuffer sb = new StringBuffer();
//                int c;
//                do {
//                    c = in.read();
//                    sb.append((char) c);
//                }
//                while (c != '\n');
//                if (b == 1) { // error
//                    CSPLog.error(BackupBasePrincipal.class, sb.toString());
//                }
//                if (b == 2) { // fatal error
//                    CSPLog.error(BackupBasePrincipal.class, sb.toString());
//                }
//            }
//
//            return b;
//
//        } catch (IOException ex){
//
//            CLSException.register(ex);
//
//            return -1;
//        }
//    }
}
