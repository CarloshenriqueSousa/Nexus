package Security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utilitário para hash e verificação de senhas.
 * Usa SHA-256 com salt aleatório de 128 bits.
 */
public final class SenhaUtil {

	private static final int SALT_TAMANHO = 16; // 16 bytes = 128 bits

	private SenhaUtil() {}

	/**
	 * Gera um salt aleatório criptograficamente seguro.
	 * @return salt em hexadecimal (32 caracteres)
	 */
	public static String gerarSalt() {
		byte[] salt = new byte[SALT_TAMANHO];
		new SecureRandom().nextBytes(salt);
		return bytesParaHex(salt);
	}

	/**
	 * Gera hash SHA-256 da senha concatenada com salt.
	 * @param senha a senha em texto puro
	 * @param salt o salt em hexadecimal
	 * @return hash em hexadecimal (64 caracteres)
	 */
	public static String hashear(String senha, String salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String entrada = salt + senha;
			byte[] hash = digest.digest(entrada.getBytes(StandardCharsets.UTF_8));
			return bytesParaHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 não disponível na JVM", e);
		}
	}

	/**
	 * Verifica se a senha corresponde ao hash+salt armazenado.
	 */
	public static boolean verificar(String senha, String hashArmazenado, String salt) {
		String hashCalculado = hashear(senha, salt);
		return hashCalculado.equals(hashArmazenado);
	}

	/**
	 * Converte array de bytes para string hexadecimal.
	 */
	private static String bytesParaHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
