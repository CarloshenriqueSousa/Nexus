package Persistencia;

public class PersistenciaException extends RuntimeException {

	public PersistenciaException(String message) {
		super(message);
	}

	public PersistenciaException(String message, Throwable cause) {
		super(message, cause);
	}

	public static PersistenciaException bancoIndisponivel() {
		return new PersistenciaException("Banco de dados indisponível. Verifique PostgreSQL.");
	}
}
