package Data;

import java.util.*;

public class MimeTypes {

	/**
	 * CONCEITO: Map
	 * Map<Chave, Valor> associa cada chave a um valor único
	 * Hashap é a implemedntação mais comun: acesso em 0(1)
	 * 
	 * CONCEITO: Generics (<String, String>)
	 * Os tipos entre <> informam ao compilador o que o Map vai armazenar
	 * evitando erros em produção
	 */
	
	private static final Map<String, String> TIPOS = new HashMap<>();
	
	/**
	 * CONCEITO: Bloco Inicializador estáticop
	 * Executa uma vez quando a classe é carregada na Jvm
	 * util para inicializar estrutureas de dados complexas
	 */
	
	static {
		TIPOS.put("html", "text/html; charset=utf-8");
        TIPOS.put("css",  "text/css; charset=utf-8");
        TIPOS.put("js",   "application/javascript");
        TIPOS.put("json", "application/json; charset=utf-8");
        TIPOS.put("txt",  "text/plain; charset=utf-8");
        TIPOS.put("png",  "image/png");
        TIPOS.put("jpg",  "image/jpeg");
        TIPOS.put("jpeg", "image/jpeg");
        TIPOS.put("gif",  "image/gif");
        TIPOS.put("svg",  "image/svg+xml");
        TIPOS.put("ico",  "image/x-icon");
        TIPOS.put("pdf",  "application/pdf");
	}
	
	private MimeTypes() {}
	
	public static String porExtensao(String extensao) {
		return TIPOS.getOrDefault(extensao.toLowerCase(), "application/octet-stream");
	}
	
	public static String porCaminho(String caminho) {
		int ponto = caminho.lastIndexOf('.');
		if (ponto == -1 || ponto == caminho.length() - 1) {
			return "application/octet-stream";
		}
		return porExtensao(caminho.substring(ponto + 1));
	}
}
