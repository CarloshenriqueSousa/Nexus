package Security;

import java.util.Map;

/**
 * Adiciona cabeçalhos HTTP de segurança às respostas.
 * Protege contra XSS, clickjacking, MIME sniffing e outros ataques.
 */
public final class SecurityHeaders {

	private SecurityHeaders() {}

	/**
	 * Aplica cabeçalhos de segurança ao mapa de cabeçalhos da resposta.
	 */
	public static void aplicar(Map<String, String> cabecalhos) {
		cabecalhos.put("X-Content-Type-Options", "nosniff");
		cabecalhos.put("X-Frame-Options", "DENY");
		cabecalhos.put("X-XSS-Protection", "1; mode=block");
		cabecalhos.put("Referrer-Policy", "strict-origin-when-cross-origin");
		cabecalhos.put("Cache-Control", "no-store, no-cache, must-revalidate");
		cabecalhos.put("Pragma", "no-cache");
	}
}
