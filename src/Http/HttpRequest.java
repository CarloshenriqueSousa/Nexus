package Http;

import java.util.*;
import java.io.*;
import Http.HttpMethod;
import Security.User;

/**
 * CONCEITO: Imutabilidade + Fábrica estática 
 * Versão atualizada com Leitura Binária Segura (Zero-Copy para uploads gigantes).
 */
public class HttpRequest {
	
	private User user;

	// LIMITES DE SEGURANÇA DE INFRAESTRUTURA
	private static final int MAX_HEADER_SIZE = 8192;       // 8KB máximo para cabeçalhos
	private static final int MAX_BODY_SIZE_TEXT = 10485760; // 10MB para JSON/texto em memória

	private final HttpMethod metodo;
	private final String caminho;
	private final String versaoHttp;
	private final String ipCliente;
	private final Map<String, String> cabecalhos;
	private final Map<String, String> parametrosQuery;
	private final String corpoTextual;
	
	private final InputStream bodyStream;
	private final long bodyLength;

	private HttpRequest(HttpMethod metodo, String caminho, String versaoHttp, Map<String, String> cabecalhos,
			Map<String, String> parametrosQuery, String corpoTextual, InputStream bodyStream, long bodyLength, String ipCliente) {
		this.metodo = metodo;
		this.caminho = caminho;
		this.versaoHttp = versaoHttp;
		this.cabecalhos = cabecalhos;
		this.parametrosQuery = parametrosQuery;
		this.corpoTextual = corpoTextual;
		this.bodyStream = bodyStream;
		this.bodyLength = bodyLength;
		this.ipCliente = ipCliente;
	}

	public static HttpRequest parse(InputStream entrada) throws IOException {
		return parse(entrada, "desconhecido");
	}

	public static HttpRequest parse(InputStream entrada, String ipCliente) throws IOException {
		// PASSO 1: Ler cabeçalhos em modo binário sem consumir o corpo
		ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
		int lido;
		int contadorHeaders = 0;
		boolean fimCabecalhos = false;
		
		int prev = -1;
		int prev2 = -1;
		int prev3 = -1;

		while ((lido = entrada.read()) != -1) {
			headerBuffer.write(lido);
			contadorHeaders++;
			
			if (contadorHeaders > MAX_HEADER_SIZE) {
				throw new IOException("Ataque DoS Detectado: Cabeçalhos excederam 8KB.");
			}
			
			if (prev3 == '\r' && prev2 == '\n' && prev == '\r' && lido == '\n') {
				fimCabecalhos = true;
				break;
			}
			prev3 = prev2;
			prev2 = prev;
			prev = lido;
		}

		if (!fimCabecalhos) {
			throw new IOException("Requisição HTTP malformada ou conexão encerrada prematuramente.");
		}

		String headersStr = headerBuffer.toString("US-ASCII");
		BufferedReader leitorHeaders = new BufferedReader(new StringReader(headersStr));
		
		String linhaDeRequisicao = leitorHeaders.readLine();
		if (linhaDeRequisicao == null || linhaDeRequisicao.isBlank()) {
			throw new IOException("Requisição vazia recebida");
		}
		
		String[] partes = linhaDeRequisicao.split(" ", 3);
		if (partes.length < 3) {
			throw new IOException("Linha de requisição malformada: " + linhaDeRequisicao);
		}
		
		HttpMethod metodo = HttpMethod.fromString(partes[0]);
		String caminhoCompleto = partes[1];
		String versaoHttp = partes[2];
		
		String caminho;
		Map<String, String> parametrosQuery = new HashMap<>();
		int ponto = caminhoCompleto.indexOf('?');
		if (ponto != -1) {
			caminho = caminhoCompleto.substring(0, ponto);
			parsearQueryString(caminhoCompleto.substring(ponto + 1), parametrosQuery);
		} else {
			caminho = caminhoCompleto;
		}
		
		Map<String, String> cabecalhos = new HashMap<>();
		String linha;
		while ((linha = leitorHeaders.readLine()) != null && !linha.isEmpty()) {
			int doisPontos = linha.indexOf(':');
			if (doisPontos != -1) {
				String nome = linha.substring(0, doisPontos).trim().toLowerCase();
				String valor = linha.substring(doisPontos + 1).trim();
				cabecalhos.put(nome, valor);
			}
		}
		
		long length = 0;
		String tamanhoStr = cabecalhos.get("content-length");
		if (tamanhoStr != null) {
			try {
				length = Long.parseLong(tamanhoStr.trim());
			} catch (NumberFormatException ignored) {}
		}

		String contentType = cabecalhos.getOrDefault("content-type", "").toLowerCase();
		boolean isStream = contentType.contains("application/octet-stream") || contentType.contains("multipart/form-data");
		
		String corpo = "";
		InputStream bodyStream = null;

		if (length > 0) {
			if (isStream) {
				// Deixa no stream para leitura manual
				bodyStream = new BoundedInputStream(entrada, length);
			} else {
				// Carrega na memória se for pequeno (JSON)
				if (length > MAX_BODY_SIZE_TEXT) {
					throw new IOException("Payload Too Large: O corpo JSON/Texto excede o limite máximo de 10MB.");
				}
				byte[] buf = new byte[(int) length];
				int totalLido = 0;
				while (totalLido < length) {
					int read = entrada.read(buf, totalLido, (int) length - totalLido);
					if (read == -1) break;
					totalLido += read;
				}
				corpo = new String(buf, "UTF-8");
			}
		}
		        
		return new HttpRequest(metodo, caminho, versaoHttp, cabecalhos, parametrosQuery, corpo, bodyStream, length, ipCliente);
	}
	
	private static void parsearQueryString(String query, Map<String, String> params) {
		String[] pares = query.split("&");
		for (String par: pares) {
			String[] kv = par.split("=", 2);
			if (kv.length == 2) {
				params.put(decodificarUrl(kv[0]), decodificarUrl(kv[1]));
			} else if (kv.length == 1 && !kv[0].isBlank()) {
				params.put(decodificarUrl(kv[0]), "");
			}
		}
	}
	
	private static String decodificarUrl(String s) {
		return s.replace("+", " ")
				.replace("%20", " ")
				.replace("%3A", ":")
				.replace("%2F", "/");
	}

	public HttpMethod getMetodo() { return metodo; }
	public String getCaminho() { return caminho; }
	public String getVersaoHttp() { return versaoHttp; }
	public String getIpCliente() { return ipCliente; }

	public String getCabecalhos(String nome) {
		return cabecalhos.getOrDefault(nome.toLowerCase(), "");
	}

	public String getCookie(String nome) {
		String cookies = getCabecalhos("cookie");
		if (cookies.isBlank()) return "";

		for (String parte : cookies.split(";")) {
			String[] kv = parte.trim().split("=", 2);
			if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(nome)) {
				return kv[1].trim();
			}
		}
		return "";
	}

	public String getParametrosQuery(String nome) {
		return parametrosQuery.getOrDefault(nome, "");
	}
	
	public Map<String, String> getParametrosQuery() {
		return Collections.unmodifiableMap(parametrosQuery);
	}

	public String getCorpo() { return corpoTextual; }
	
	public InputStream getBodyStream() { return bodyStream; }
	public long getBodyLength() { return bodyLength; }

	public Map<String, String> getFormData() {
		Map<String, String> form = new HashMap<>();
		if (corpoTextual != null && !corpoTextual.isBlank()) {
			parsearQueryString(corpoTextual, form);
		}
		return form;
	}
	
	@Override
	public String toString() {
		return metodo + " " + caminho + " [" + versaoHttp + "] ";
	}
	
	private final Map<String, String> parametrosPath = new HashMap<>();

	public String getParametroPath(String nome) {
		return parametrosPath.getOrDefault(nome, "");
	}

	public void setParametroPath(String nome, String valor) {
		parametrosPath.put(nome, valor);
	}

	public void setUser(User user) { this.user = user; }
	public User getUser() { return user; }

	// Classe utilitária para limitar a leitura do InputStream ao Content-Length
	public static class BoundedInputStream extends InputStream {
		private final InputStream in;
		private long restantes;

		public BoundedInputStream(InputStream in, long size) {
			this.in = in;
			this.restantes = size;
		}

		@Override
		public int read() throws IOException {
			if (restantes <= 0) return -1;
			int res = in.read();
			if (res != -1) restantes--;
			return res;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (restantes <= 0) return -1;
			int bytesLidos = in.read(b, off, (int) Math.min(len, restantes));
			if (bytesLidos != -1) restantes -= bytesLidos;
			return bytesLidos;
		}
	}
}