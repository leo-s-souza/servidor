/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp.rest;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.arquivos.CSPArquivos;
import br.com.egula.capp.classes.CLSException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;

import static br.com.egula.capp.classes.CLSCapp.PATH_INFOS;

/**
 * Classe responsável por fornecer imagens requisitadas.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class ImagensHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) {

        try {
            final CSPArquivos arq = new CSPArquivos(new CSPArquivos(t.getRequestURI().getPath()).getName());
            CSPLog.info(ImagensHandler.class, "download-imagem(" + t.getRemoteAddress().getAddress().getHostAddress() + "):" + arq.getName());

            trataNomeFileAppToServer(arq);
            File file = arq.objFile();

            byte[] bytearray = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(bytearray, 0, bytearray.length);

            t.sendResponseHeaders(200, file.length());
            try (OutputStream os = t.getResponseBody()) {
                os.write(bytearray, 0, bytearray.length);
            }

        } catch (Exception ex) {
            CLSException.register(ex);
        }
    }

    /**
     * Converte o padrão do nome do arquivo usado no app para o caminho usado no
     * server.
     * <p>
     * Exemplo: 141257000161_e_imagem.png =>
     * /opt/capp/infos-app/contratantes/141257000161/enviar-app/imagem.png
     *
     * @param arquivo - Arquivo a ser tratado.
     * @throws Exception - Possíveis erros nos arquivos.
     */
    private static void trataNomeFileAppToServer(CSPArquivos arquivo) throws Exception {
        final String name = arquivo.getName();

        if (name.contains("_e_")) {// /enviar-app/
            arquivo.setPath(
                    PATH_INFOS
                            + "/contratantes/"
                            + extractCnpjFromStringFile(name)
                            + "/enviar-app"
                            + "/"
                            + name.split("_e_")[1]
            );

        } else if (name.startsWith("ci_")) {// /central-imagens/
            arquivo.setPath(
                    PATH_INFOS +
                            "/contratantes/central-imagens"
                            + "/"
                            + (name.replace("ci_", ""))
                            .replace("_", "/")
            );

        } else {
            throw new InvalidAlgorithmParameterException(name + ":nao-reconhecido");
        }

        if (!arquivo.exists() && !arquivo.isFile()) {
            final String ablsBk = arquivo.getAbsolutePath();
            final String ablsNew = ablsBk.replace(
                    name,
                    name.replace("_", "/")
            );
            arquivo.setPath(ablsNew);
            if (!arquivo.isFile()) {
                arquivo.setPath(ablsBk);
            }
        }

    }

    /**
     * Extrai o cnpj do nome d eum arquivo.
     *
     * @param file - Nome do arquivo.
     * @return Retorna o CNPJ presente no nome do arquivo.
     */
    private static String extractCnpjFromStringFile(String file) {

        if (file == null || file.trim().isEmpty()) {
            return null;
        }

        file = file.trim();

        String cnpj = file.replaceAll("[^0123456789]", "");
        if (cnpj.length() == 14) {
            return cnpj;
        }

        try {
            cnpj = new CSPArquivos(file).getFullPath().trim().replaceAll("[^0123456789]", "");
        } catch (Exception e) {
            CLSException.register(e);
        }
        if (cnpj.length() == 14) {
            return cnpj;

        }

        if (file.contains("/")) {
            cnpj = file.split("/")[0].trim();
            if (cnpj.length() == 14) {
                return cnpj;
            }
        }

        if (file.contains("_")) {
            cnpj = file.split("_")[0].trim();
            if (cnpj.length() == 14) {
                return cnpj;
            }
        }

        return null;
    }
}