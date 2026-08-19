package Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import Handler.Handler;
import Http.HttpRequest;
import Http.HttpResponse;

/**
 * Serve arquivos estáticos de um diretório base (ex.: public/Workspace).
 * Opcionalmente remove um prefixo de URL (ex.: /workspace/) antes de resolver o path.
 */
public class StaticFilesHandler implements Handler {

	private final Path diretorioBase;
	private final String prefixoUrl;

	public StaticFilesHandler(String diretorio) {
		this(diretorio, null);
	}

	public StaticFilesHandler(String diretorio, String prefixoUrl) {
		this.diretorioBase = Paths.get(diretorio).toAbsolutePath().normalize();
		this.prefixoUrl = prefixoUrl;
	}

	public HttpResponse servir(HttpRequest requisicao) {
		String caminho = requisicao.getCaminho();
		if (caminho.equals("/")) {
			caminho = "/index.html";
		}

		String subpath = caminho;
		if (prefixoUrl != null && !prefixoUrl.isEmpty()) {
			if (caminho.equals(prefixoUrl.substring(0, prefixoUrl.length() - 1))) {
				return HttpResponse.naoEncontrado();
			}
			if (caminho.startsWith(prefixoUrl)) {
				subpath = caminho.substring(prefixoUrl.length());
			} else if (prefixoUrl.endsWith("/") && caminho.startsWith(prefixoUrl.substring(0, prefixoUrl.length() - 1))) {
				String prefixoSemBarra = prefixoUrl.substring(0, prefixoUrl.length() - 1);
				subpath = caminho.substring(prefixoSemBarra.length());
				if (subpath.startsWith("/")) {
					subpath = subpath.substring(1);
				}
			} else {
				return HttpResponse.naoEncontrado();
			}
		} else if (caminho.startsWith("/")) {
			subpath = caminho.substring(1);
		}

		Path arquivo = diretorioBase.resolve(subpath).normalize();

		if (!arquivo.startsWith(diretorioBase) || !Files.isRegularFile(arquivo)) {
			if (!caminho.startsWith("/api/")) {
				Path indexHtml = diretorioBase.resolve("index.html").normalize();
				if (Files.isRegularFile(indexHtml)) {
					try {
						byte[] conteudo = Files.readAllBytes(indexHtml);
						return HttpResponse.ok().corpo(conteudo, "text/html; charset=utf-8");
					} catch (IOException ignored) {}
				}
			}
			return HttpResponse.naoEncontrado();
		}

		try {
			byte[] conteudo = Files.readAllBytes(arquivo);
			String tipo = MimeTypes.porCaminho(arquivo.toString());
			return HttpResponse.ok().corpo(conteudo, tipo);
		} catch (IOException e) {
			return HttpResponse.erroInterno("Erro ao ler arquivo estático.");
		}
	}

	@Override
	public HttpResponse handle(HttpRequest request) {
		return servir(request);
	}
}
