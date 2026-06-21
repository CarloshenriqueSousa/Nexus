package Http;

import java.util.*;
import java.io.*;
import Http.HttpMethod;
import Security.User;

/**
 * CONCEITO: Imutabilidade + Fábrica estática 
 * Versão atualizada com Proteção de Buffer e Limites Anti-DoS corporativos.
 */
public class HttpRequest {
	
	private User user;

	// LIMITES DE SEGURANÇA DE INFRAESTRUTURA
	private static final int MAX_HEADER_SIZE = 8192;       // 8KB máximo para cabeçalhos (Padrão Apache/Nginx)
	private static final int MAX_BODY_SIZE = 10485760;     // 10MB máximo para uploads/corpo no workspace

	private final HttpMethod metodo;
	private final String caminho;
	private final String versaoHttp;
	private final String ipCliente;
	private final Map<String, String> cabecalhos;
	private final Map<String, String> parametrosQuery;
	private final String corpo;

	private HttpRequest(HttpMethod metodo, String caminho, String versaoHttp, Map<String, String> cabecalhos,
			Map<String, String> parametrosQuery, String corpo, String ipCliente) {
		this.metodo = metodo;
		this.caminho = caminho;
		this.versaoHttp = versaoHttp;
		this.cabecalhos = cabecalhos;
		this.parametrosQuery = parametrosQuery;
		this.corpo = corpo;
		this.ipCliente = ipCliente;
	}

	public static HttpRequest parse(InputStream entrada) throws IOException {
		return parse(entrada, "desconhecido");
	}

	public static HttpRequest parse(InputStream entrada, String ipCliente) throws IOException {
		BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada));
		
		/**
		 * PASSO 1: Linha de requisição
		 */
		String linhaDeRequisicao = leitor.readLine();
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
		
		/**
		 * PASSO 2: Separar caminho dos parâmetros de Query
		 */
		String caminho;
		Map<String, String> parametrosQuery = new HashMap<>();
		 
		int ponto = caminhoCompleto.indexOf('?');
		if (ponto != -1) {
			caminho = caminhoCompleto.substring(0, ponto);
			parsearQueryString(caminhoCompleto.substring(ponto + 1), parametrosQuery);
		} else {
			caminho = caminhoCompleto;
		}
		 
		/**
		 * PASSO 3: Leitura dos cabeçalhos com Trava Anti-DoS
		 */
		Map<String, String> cabecalhos = new HashMap<>();
		String linha;
		int totalHeaderBytes = 0;

		while ((linha = leitor.readLine()) != null && !linha.isEmpty()) {
			// Defesa contra estouro de memória por cabeçalhos gigantes
			totalHeaderBytes += linha.length();
			if (totalHeaderBytes > MAX_HEADER_SIZE) {
				throw new IOException("Ataque DoS Detectado: Tamanho dos cabeçalhos excedeu o limite seguro de 8KB.");
			}

			int doisPontos = linha.indexOf(':');
			if (doisPontos != -1) {
				String nome = linha.substring(0, doisPontos).trim().toLowerCase();
				String valor = linha.substring(doisPontos + 1).trim();
				cabecalhos.put(nome, valor);
			}
		}
		 
		/**
		 * PASSO 4: Ler corpo com Trava de Alocação de Memória (Anti-OOM)
		 */
		String corpo = "";
		String tamanhoStr = cabecalhos.get("content-length");
		if (tamanhoStr != null) {
			try {
				int tamanho = Integer.parseInt(tamanhoStr.trim());
				
				// Defesa: Impede a alocação de buffers gigantescos na heap do Java
				if (tamanho > MAX_BODY_SIZE) {
					throw new IOException("Payload Too Large: O corpo da requisição excede o limite máximo de 10MB.");
				}

				if (tamanho > 0) {
					char[] buffer = new char[tamanho];
					int lidos = leitor.read(buffer, 0, tamanho);
					if (lidos > 0) {
						corpo = new String(buffer, 0, lidos);
					}
				}
			} catch (NumberFormatException e) {
				// Content-Length inválido → ignora o corpo de forma segura
			}
		}
		        
		return new HttpRequest(metodo, caminho, versaoHttp, cabecalhos, parametrosQuery, corpo, ipCliente);
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
		if (cookies.isBlank()) {
			return "";
		}

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

	public String getCorpo() { return corpo; }

	/**
	 * Parseia o corpo da requisição como dados de formulário (application/x-www-form-urlencoded).
	 * Reutiliza o parser de query string existente.
	 */
	public Map<String, String> getFormData() {
		Map<String, String> form = new HashMap<>();
		if (corpo != null && !corpo.isBlank()) {
			parsearQueryString(corpo, form);
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

	public void setUser(User user) {
		this.user = user;
	}
	
	public User getUser() {
		return user;
	}
}