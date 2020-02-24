/*
 * Copyright (c) 2018. Casa da Automação Ltda.
 * Todos os direitos reservados.
 */

package br.com.egula.capp;

import br.com.casaautomacao.casagold.classes.CSPLog;
import br.com.casaautomacao.casagold.classes.FrmModuloPaiBase;
import br.com.egula.capp.classes.CLSException;
import br.com.egula.capp.rest.*;
import br.com.egula.capp.rest.garcom.LoginGarcomHandler;
import br.com.egula.capp.rest.garcom.LogoffForcadoGarcomHandler;
import br.com.egula.capp.rest.garcom.LogoffGarcomHandler;
import br.com.egula.capp.rest.garcom.PedidoGarcomHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Servidor RESTful para comunicação de outras aplicações.
 *
 * @author Matheus Felipe Amelco
 * @since 1.0
 */
public class ModuloRest extends FrmModuloPaiBase {

    @Override
    protected void run() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(22250), 0);

        // Efetua login dos usuários. (POST)
        server.createContext("/login", new LoginHandler());

        // Efetua login de migração dos usuários. (POST)
        server.createContext("/login-migracao", new LoginMigracaoHandler());

        // Efetua logoff dos usuários. (POST)
        server.createContext("/logoff", new LogoffHandler());

        // Força o logoff da conta em todos os dispositivos. (POST)
        server.createContext("/forca-logoff", new LogoffForcadoHandler());

        // Faz a verificação do login para liberar o dispositivo a fazer o pedido.
        server.createContext("/verifica-login", new VerificaLoginHandler());

        // Adiciona um novo usuário. (POST)
        server.createContext("/adiciona-usuario", new AdicionaUsuarioHandler());

        // Altera os dados cadastrais de um usuário. (POST)
        server.createContext("/altera-usuario", new AlteraUsuarioHandler());

        // Adiciona um novo endereço para um usuário. (POST)
        server.createContext("/adiciona-endereco", new AdicionaEnderecoHandler());

        // Adiciona um novo endereço para um usuário. (POST)
        server.createContext("/adiciona-endereco-novo", new AdicionaEnderecoNovoHandler());

        // Calcula a distância entre os endereços do usuário e da loja (POST)
        server.createContext("/calcula-distancias-enderecos", new EnderecoDistanciaHandler());

        // Remove um endereço relacionado ao usuário. (POST)
        server.createContext("/remove-endereco", new RemoveEnderecoHandler());

        // Recupera a senha d eum usuário enviando-o um email. (POST)
        server.createContext("/recuperar-senha", new RecuperarSenhaHandler());

        // Confirmação de recebimento de notificações. (POST)
        server.createContext("/notificacao-recebida", new NotificacaoRecebidaHandler());

        // Efetua pedido (POST)
        server.createContext("/efetua-pedido", new PedidoHandler());

        // Exporta os dados da base para o Firebase (POST)
        server.createContext("/exporta-dados", new ExportaDadosFirebaseHandler());

        // Confirmação do pedido web (GET)
        server.createContext("/loja/confirma-pedido-web", new ConfirmaPedidoHandler());

        // Cancelamento do pedido web (GET)
        server.createContext("/loja/cancela-pedido-web", new CancelaPedidoHandler());

        // Imagens via HTTP para o Android. (GET)
        server.createContext("/imagens", new ImagensHandler());

        // Imagens via HTTP para o Android. (GET)
        server.createContext("/pagamentos", new PagamentoHandler());

        // Efetua o login do garçom. (POST)
        server.createContext("/garcom/login", new LoginGarcomHandler());

        // Efetua o logoff do garçom. (POST)
        server.createContext("/garcom/logoff", new LogoffGarcomHandler());

        // Efetua o logoff forçado do garçom. (POST)
        server.createContext("/garcom/forca-logoff", new LogoffForcadoGarcomHandler());

        // Efetua pedido garçom. (POST)
        server.createContext("/garcom/efetua-pedido", new PedidoGarcomHandler());

        // Cadastro de token APNS. (POST)
        server.createContext("/registra-token-apns", new RegistraTokenApnsHandler());

        server.createContext("/requisita-horarios-loja", new RequisitaHorariosLojaHandler());

        server.createContext("/ajusta-horarios-loja", new AjustaHorariosLojaHandler());

        server.createContext("/procura-produtos", new ProcuraProdutosHandler());

        // Registra nova imagem. (POST)
        server.createContext("/nova-imagem", new NovaImagemHandler());

        server.setExecutor(Executors.newCachedThreadPool());
//        server.setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100)));
//        server.setExecutor(null);

        server.start();
    }

    /**
     * Lê o conteúdo da requisição.
     *
     * @param httpExchange - Handler da conexão.
     */
    public static String readPostContent(HttpExchange httpExchange) throws IOException {
        StringBuilder sb = new StringBuilder();

        String inputLine;

        BufferedReader in = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()));
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }

        in.close();

        return sb.toString().trim();
    }

    /**
     * Trata o link HTTP para remover os parâmetros e adicioná-los em um mapa.
     *
     * @param query - Link da requisição.
     * @return - Mapa com os parametros.
     */
    public static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();

        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }

    /**
     * Escreve a resposta da requisição.
     *
     * @param httpExchange - Handler da conexão.
     * @param mensagem     - Mesangem de status da resposta HTTP.
     * @param statusCode   - Código de status da resposta HTTP.
     */
    public static void writeResponse(HttpExchange httpExchange, int statusCode, String mensagem) {
        String hostAddress = httpExchange.getRemoteAddress().getAddress().getHostAddress();
        CSPLog.info(ModuloRest.class, "response(" + hostAddress + "): code=" + statusCode + " length=" + mensagem.length() + " msg=" + mensagem);

        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");

        OutputStream os = null;
        try {
            byte[] bytes = mensagem.getBytes();
            httpExchange.sendResponseHeaders(statusCode, bytes.length);
            os = httpExchange.getResponseBody();
            os.write(bytes);
            os.flush();

        } catch (IOException e) {
            CLSException.register(e);

        } finally {

            if (os != null) {
                try {
                    os.close();

                    CSPLog.info(ModuloRest.class, "close(" + hostAddress + ")");

                } catch (IOException e) {
                    CLSException.register(e);
                }
            }
        }
    }
}