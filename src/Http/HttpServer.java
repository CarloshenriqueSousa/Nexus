package Http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Data.StaticFilesHandler;
import Router.Router;
import Handler.ClientHandler;

/**
 * ServerSocket
 * É feito o reconhecimento em uma porta TCP aguardando conexões.
 * Quando um cliente se conecta, accept() retorna um socket
 * para comunicação exclusiva com aquele cliente.
 * 
 * Fluxo:
 * ServerSocket(porta) -> bind automático na porta
 * ServerSocket.aceept() -> bloqueia até um cliente conectar
 * Socket cliente = accept() -> conexão estabelecida
 * cliente.getInputStream() | getOutputStream() -> troca de dados
 */

public class HttpServer {

	private final int porta;
	private final Router router;
	
	/**
	 * ExecutorService - Pool de threads
	 * Criar um thread nova para cada requisição não é valido.
	 * Um poll reultiliza threads: mantem N threads vivas distribuindo as tarefas entre elas
	 * 
	 *  newFixedThreadPool(10) mantém exatamente10 threads ativas.
	 *  mais de 10 requisições simultâneas ficam em fila
	 */
	
	private final ExecutorService poolDeThreads;
	
	private ServerSocket serverSocket;
	
	/**
	 * Volatiie
	 * 'Volatiie' força leitura/escrita sempre na memoria princiapl,
	 * garantindo que todas as threads enxergam o mesmo valor.
	 * 
	 * sem o volatile, uma thread poderia ler um valor em cache
	 * enquanto outra já o modificou na memoria
	 */
	
	private volatile boolean rodando = false;
	
	public HttpServer(int porta, Router router) {
		this.porta = porta;
		this.router = router;
		this.poolDeThreads = Executors.newFixedThreadPool(10);
	}
	
	/**
	 * Method reference (handler::servir)
	 * É equivalente à lambda: req -> handler.servir(req)
	 * Syntactic sugar para passar um método como Handler
	 */
	public HttpServer configurarArquivosEstaticos(String diretorio) {
        StaticFilesHandler handler = new StaticFilesHandler(diretorio);
        router.fallback(handler::servir);
        return this;
    }
	
	public void iniciar() throws IOException {
		serverSocket = new ServerSocket();
		serverSocket.setReuseAddress(true);
		serverSocket.bind(new java.net.InetSocketAddress(porta));
		
		rodando = true;
		
		System.out.println("=".repeat(50));
        System.out.println("  Servidor HTTP iniciado!");
        System.out.println("  URL: http://localhost:" + porta);
        System.out.println("  Threads no pool: 10");
        System.out.println("=".repeat(50));
        
        while (rodando) {
            try {
                Socket socketCliente = serverSocket.accept();
                ClientHandler tarefa = new ClientHandler(socketCliente, router);
                poolDeThreads.execute(tarefa);
            } catch (IOException e) {
                if (rodando) {
                    System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                }
            }
        }
	}
}
