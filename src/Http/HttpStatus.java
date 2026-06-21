package Http;

/**
 * CONCEITO: Enum com campos e construtores
 * Enums podem ter campos, construtores e métodos como classes
 * Cada constante chama o construtor com seus própios valores
 * 
 * ATUALIZAÇÃO SEGURANÇA: Adicionados status HTTP essenciais para
 * respostas de segurança do Vaultra Secure Server.
 */

public enum HttpStatus {
	OK(200, "OK"),
	CREATED(201, "CREATED"),
	NO_CONTENT(204, "NO CONTENT"),
	FOUND(302, "FOUND"),
	BAD_REQUEST(400, "BAD REQUEST"),
	UNAUTHORIZED(401, "UNAUTHORIZED"),
	FORBIDDEN(403, "FORBIDDEN"),
	NOT_FOUND(404, "NOT FOUND"),
	METHOD_NOT_ALLOWED(405, "METHOD NOT ALLOWED"),
	PAYLOAD_TOO_LARGE(413, "PAYLOAD TOO LARGE"),
	TOO_MANY_REQUESTS(429, "TOO MANY REQUESTS"),
	INTERNAL_SERVER_ERROR(500, "INTERNAL SERVER ERROR"),
	SERVICE_UNAVAILABLE(503, "SERVICE UNAVAILABLE");
	
	private final int code;
	private final String message;
	
	HttpStatus(int code, String message) {
		this.code = code;
		this.message = message;
	}
	
	/**
	 * Métodos Getter expoem campos privados controladamente
	 * @return
	 */

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
	
	@Override
	public String toString() {
		return code + "" + message;
	}
	
	
}
