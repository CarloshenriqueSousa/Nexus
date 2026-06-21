package Security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gerenciador de autenticação JWT (JSON Web Token).
 * Implementa geração e validação de tokens usando HMAC-SHA256,
 * sem dependências externas (Java puro).
 * 
 * Formato JWT: header.payload.signature
 * - Header:    {"alg":"HS256","typ":"JWT"} → Base64URL
 * - Payload:   {"sub":"username","id":1,"cargo":"ADMIN","iat":ts,"exp":ts} → Base64URL
 * - Signature: HMAC-SHA256(header.payload, chaveSecreta) → Base64URL
 */
public class AuthManager {

	private static final String ALGORITMO = "HmacSHA256";
	private static final String HEADER_JWT = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");

	private final byte[] chaveSecreta;
	private final long expiracaoMs;

	/**
	 * @param caminhoChave   caminho do arquivo da chave secreta
	 * @param expiracaoHoras tempo de expiração do token em horas
	 */
	public AuthManager(String caminhoChave, int expiracaoHoras) {
		this.chaveSecreta = carregarOuGerarChave(caminhoChave);
		this.expiracaoMs = expiracaoHoras * 3600L * 1000L;
		System.out.println("[JWT] AuthManager inicializado — expiração: " + expiracaoHoras + "h");
	}

	/**
	 * Gera um token JWT para o usuário autenticado.
	 */
	public String gerarToken(User usuario) {
		long agora = System.currentTimeMillis();
		long expiracao = agora + expiracaoMs;

		String payload = "{" +
			"\"sub\":\"" + escaparJson(usuario.getUsername()) + "\"," +
			"\"id\":" + usuario.getId() + "," +
			"\"cargo\":\"" + usuario.getCargo().name() + "\"," +
			"\"iat\":" + (agora / 1000) + "," +
			"\"exp\":" + (expiracao / 1000) +
			"}";

		String payloadBase64 = base64Url(payload);
		String conteudo = HEADER_JWT + "." + payloadBase64;
		String assinatura = assinar(conteudo);

		return conteudo + "." + assinatura;
	}

	/**
	 * Valida um token JWT e retorna os dados extraídos.
	 * @return TokenInfo com dados do usuário, ou null se inválido/expirado
	 */
	public TokenInfo validarToken(String token) {
		if (token == null || token.isBlank()) return null;

		String[] partes = token.split("\\.");
		if (partes.length != 3) return null;

		// 1. Verificar assinatura HMAC-SHA256
		String conteudo = partes[0] + "." + partes[1];
		String assinaturaEsperada = assinar(conteudo);
		if (!assinaturaEsperada.equals(partes[2])) {
			System.err.println("[JWT] Assinatura inválida detectada — possível adulteração");
			return null;
		}

		// 2. Decodificar payload
		String payloadJson;
		try {
			payloadJson = new String(
				Base64.getUrlDecoder().decode(partes[1]),
				StandardCharsets.UTF_8
			);
		} catch (Exception e) {
			System.err.println("[JWT] Payload malformado: " + e.getMessage());
			return null;
		}

		// 3. Extrair campos do payload
		String username = extrairCampoString(payloadJson, "sub");
		int id = extrairCampoInt(payloadJson, "id");
		String cargo = extrairCampoString(payloadJson, "cargo");
		long exp = extrairCampoLong(payloadJson, "exp");

		if (username == null || cargo == null) return null;

		// 4. Verificar expiração
		long agoraSegundos = System.currentTimeMillis() / 1000;
		if (exp <= agoraSegundos) {
			System.err.println("[JWT] Token expirado para: " + username);
			return null;
		}

		return new TokenInfo(id, username, cargo);
	}

	// ==================== TokenInfo (dados extraídos do JWT) ====================

	/**
	 * Dados extraídos de um token JWT validado com sucesso.
	 */
	public static class TokenInfo {
		private final int id;
		private final String username;
		private final String cargo;

		public TokenInfo(int id, String username, String cargo) {
			this.id = id;
			this.username = username;
			this.cargo = cargo;
		}

		public int getId() { return id; }
		public String getUsername() { return username; }
		public String getCargo() { return cargo; }
	}

	// ==================== Métodos Internos ====================

	/**
	 * Assina dados com HMAC-SHA256 usando a chave secreta.
	 */
	private String assinar(String dados) {
		try {
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(new SecretKeySpec(chaveSecreta, ALGORITMO));
			byte[] hash = mac.doFinal(dados.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException("Erro ao assinar JWT", e);
		}
	}

	/**
	 * Codifica string para Base64URL (sem padding).
	 */
	private static String base64Url(String texto) {
		return Base64.getUrlEncoder().withoutPadding()
				.encodeToString(texto.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Carrega chave secreta do arquivo ou gera uma nova de 256 bits.
	 */
	private byte[] carregarOuGerarChave(String caminhoChave) {
		Path path = Paths.get(caminhoChave);
		try {
			if (Files.exists(path)) {
				String chaveHex = Files.readString(path).trim();
				System.out.println("[JWT] Chave secreta carregada de: " + path.toAbsolutePath());
				return hexParaBytes(chaveHex);
			} else {
				byte[] novaChave = new byte[32]; // 256 bits
				new SecureRandom().nextBytes(novaChave);
				String chaveHex = bytesParaHex(novaChave);

				Files.createDirectories(path.getParent());
				Files.writeString(path, chaveHex);
				System.out.println("[JWT] Nova chave secreta gerada em: " + path.toAbsolutePath());
				return novaChave;
			}
		} catch (IOException e) {
			System.err.println("[JWT] Erro com arquivo de chave, gerando temporária: " + e.getMessage());
			byte[] temporaria = new byte[32];
			new SecureRandom().nextBytes(temporaria);
			return temporaria;
		}
	}

	private static String bytesParaHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static byte[] hexParaBytes(String hex) {
		int len = hex.length();
		byte[] bytes = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
					+ Character.digit(hex.charAt(i + 1), 16));
		}
		return bytes;
	}

	private static String escaparJson(String s) {
		return s.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r");
	}

	// ==================== Mini-parser JSON ====================

	private static String extrairCampoString(String json, String campo) {
		String busca = "\"" + campo + "\":\"";
		int inicio = json.indexOf(busca);
		if (inicio == -1) return null;
		inicio += busca.length();
		int fim = json.indexOf("\"", inicio);
		if (fim == -1) return null;
		return json.substring(inicio, fim);
	}

	private static int extrairCampoInt(String json, String campo) {
		String busca = "\"" + campo + "\":";
		int inicio = json.indexOf(busca);
		if (inicio == -1) return -1;
		inicio += busca.length();
		StringBuilder sb = new StringBuilder();
		for (int i = inicio; i < json.length(); i++) {
			char c = json.charAt(i);
			if (Character.isDigit(c) || c == '-') sb.append(c);
			else break;
		}
		try { return Integer.parseInt(sb.toString()); }
		catch (NumberFormatException e) { return -1; }
	}

	private static long extrairCampoLong(String json, String campo) {
		String busca = "\"" + campo + "\":";
		int inicio = json.indexOf(busca);
		if (inicio == -1) return -1;
		inicio += busca.length();
		StringBuilder sb = new StringBuilder();
		for (int i = inicio; i < json.length(); i++) {
			char c = json.charAt(i);
			if (Character.isDigit(c) || c == '-') sb.append(c);
			else break;
		}
		try { return Long.parseLong(sb.toString()); }
		catch (NumberFormatException e) { return -1; }
	}
}
