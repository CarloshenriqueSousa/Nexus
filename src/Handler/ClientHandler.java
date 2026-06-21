package Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import Http.HttpRequest;
import Http.HttpResponse;
import Router.Router;

/**
 * Processa uma conexão HTTP em uma thread do pool.
 * Fluxo: socket -> parse da requisição -> router -> resposta -> socket.
 */
public class ClientHandler implements Runnable {

	private final Socket socket;
	private final Router router;

	public ClientHandler(Socket socket, Router router) {
		this.socket = socket;
		this.router = router;
	}

	@Override
	public void run() {
		try (socket;
				InputStream entrada = socket.getInputStream();
				OutputStream saida = socket.getOutputStream()) {

			String ipCliente = socket.getInetAddress() != null
					? socket.getInetAddress().getHostAddress()
					: "desconhecido";
			HttpRequest requisicao = HttpRequest.parse(entrada, ipCliente);
			HttpResponse resposta = router.despachar(requisicao);
			resposta.escreverEm(saida);

		} catch (IOException e) {
			System.err.println("Erro ao processar cliente: " + e.getMessage());
		}
	}
}
