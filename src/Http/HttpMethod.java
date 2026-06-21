package Http;


/**
 * CONCEITO: Enum
 * Um Enum é igual a um tipo especial de classe com um conjunto fixo de constantes
 * é mais seguro que usar Strings ou Inteiros
 */
public enum HttpMethod {

	GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH;
	
	/**
	 * CONCEITO: método estático
	 * Pertence à classe, não é uma instancia
	 */
	
	public static HttpMethod fromString(String method) {
		try {
			return valueOf(method.toUpperCase());
		} catch (IllegalArgumentException e){
			return GET;
		}
	}
}
