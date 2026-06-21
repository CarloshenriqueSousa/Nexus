package Http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import Data.JsonBuilder;
import Security.SecurityHeaders;

public class HttpResponse {

	private HttpStatus status = HttpStatus.OK;

	private final Map<String, String> cabecalhos = new LinkedHashMap<>();

	private byte[] corpo = new byte[0];

	public HttpResponse() {
		cabecalhos.put("Server", "JavaHTTP/1.0");
		cabecalhos.put("Connection", "close");
		SecurityHeaders.aplicar(cabecalhos);
	}

	public HttpResponse status(HttpStatus status) {
		this.status = status;
		return this;
	}

	public HttpResponse corpo(String conteudo, String tipoConteudo) {
		this.corpo = conteudo.getBytes(StandardCharsets.UTF_8);
		cabecalhos.put("Content-Type", tipoConteudo);
		cabecalhos.put("Content-Length", String.valueOf(this.corpo.length));
		return this;
	}

	public HttpResponse corpo(byte[] conteudo, String tipoConteudo) {
		this.corpo = conteudo;
		cabecalhos.put("Content-Type", tipoConteudo);
		cabecalhos.put("Content-Length", String.valueOf(conteudo.length));
		return this;
	}

	public HttpResponse json(String json) {
		return corpo(json, "application/json; charset=utf-8");
	}

	public HttpResponse html(String html) {
		return corpo(html, "text/html; charset=utf-8");
	}

	public HttpResponse texto(String texto) {
		return corpo(texto, "text/plain; charset=utf-8");
	}

	public static HttpResponse ok() {
		return new HttpResponse().status(HttpStatus.OK);
	}

	public static HttpResponse ok(String conteudo) {
		return ok().texto(conteudo);
	}

	public static HttpResponse naoEncontrado() {
		return new HttpResponse()
				.status(HttpStatus.NOT_FOUND)
				.json(JsonBuilder.erro("Recurso não encontrado"));
	}

	public static HttpResponse requisicaoInvalida(String msg) {
		return new HttpResponse()
				.status(HttpStatus.BAD_REQUEST)
				.json(JsonBuilder.erro(msg));
	}

	public static HttpResponse erroInterno(String msg) {
		return new HttpResponse()
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.json(JsonBuilder.erro(msg));
	}

	public static HttpResponse servicoIndisponivel(String msg) {
		return new HttpResponse()
				.status(HttpStatus.SERVICE_UNAVAILABLE)
				.json(JsonBuilder.erro(msg));
	}

	/**
	 * Cria resposta de redirecionamento HTTP 302.
	 * Pode ser encadeada com .cookie() para setar cookies durante o redirect.
	 */
	public static HttpResponse redirect(String url) {
		return new HttpResponse()
				.status(HttpStatus.FOUND)
				.cabecalho("Location", url);
	}

	public HttpResponse cabecalho(String nome, String valor) {
		cabecalhos.put(nome, valor);
		return this;
	}

	public HttpResponse cookie(String nome, String valor, int maxAgeSegundos) {
		cabecalhos.put("Set-Cookie", nome + "=" + valor
				+ "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + maxAgeSegundos);
		return this;
	}

	public HttpResponse limparCookie(String nome) {
		cabecalhos.put("Set-Cookie", nome + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
		return this;
	}

	public void escreverEm(OutputStream saida) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("HTTP/1.1 ")
				.append(status.getCode())
				.append(' ')
				.append(status.getMessage())
				.append("\r\n");

		for (Map.Entry<String, String> entrada : cabecalhos.entrySet()) {
			sb.append(entrada.getKey())
					.append(": ")
					.append(entrada.getValue())
					.append("\r\n");
		}

		sb.append("\r\n");
		saida.write(sb.toString().getBytes(StandardCharsets.UTF_8));

		if (corpo.length > 0) {
			saida.write(corpo);
		}

		saida.flush();
	}
}
