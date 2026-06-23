package Persistencia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConexaoDB {
	private static BlockingQueue<Connection> pool;
	private static String url;
	private static String user;
	private static String password;
	private static int poolSize;
	private static volatile boolean disponivel = false;

	static {
		inicializar();
	}

	private static synchronized void inicializar() {
		Properties props = new Properties();
		Path path = Paths.get("src/config/security.properties");
		if (!Files.exists(path)) {
			path = Paths.get("config/security.properties");
		}
		if (Files.exists(path)) {
			try (InputStream in = Files.newInputStream(path)) {
				props.load(in);
			} catch (IOException e) {
				System.err.println("[ConexaoDB] Erro ao carregar security.properties: " + e.getMessage());
			}
		}

		url = System.getenv("DB_URL");
		if (url == null || url.isBlank()) {
			url = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/nexus_db");
		}

		user = System.getenv("DB_USER");
		if (user == null || user.isBlank()) {
			user = props.getProperty("db.user", "postgres");
		}

		password = System.getenv("DB_PASSWORD");
		if (password == null || password.isBlank()) {
			password = props.getProperty("db.password", "postgres");
		}

		String poolSizeStr = System.getenv("DB_POOL_SIZE");
		if (poolSizeStr == null || poolSizeStr.isBlank()) {
			poolSizeStr = props.getProperty("db.pool.size", "5");
		}
		poolSize = Integer.parseInt(poolSizeStr);

		pool = new LinkedBlockingQueue<>(poolSize);
		disponivel = false;

		try {
			Class.forName("org.postgresql.Driver");

			for (int i = 0; i < poolSize; i++) {
				pool.add(criarNovaConexao());
			}
			disponivel = true;
			System.out.println("[ConexaoDB] Pool de conexões inicializado com " + poolSize + " conexões.");
		} catch (ClassNotFoundException e) {
			System.err.println("[ConexaoDB] Driver do PostgreSQL não encontrado no classpath!");
			e.printStackTrace();
		} catch (SQLException e) {
			System.err.println("[ConexaoDB] Erro ao inicializar conexões: " + e.getMessage());
			pool.clear();
		}
	}

	public static boolean isDisponivel() {
		return disponivel;
	}

	public static String getUrl() {
		return url;
	}

	public static void validarDisponibilidade() {
		if (!disponivel) {
			throw PersistenciaException.bancoIndisponivel();
		}
	}

	public static void imprimirInstrucoesSetup() {
		System.err.println();
		System.err.println("╔══════════════════════════════════════════════════════════════╗");
		System.err.println("║  PostgreSQL obrigatório — banco indisponível                ║");
		System.err.println("╠══════════════════════════════════════════════════════════════╣");
		System.err.println("║  1. Instale e inicie o PostgreSQL                           ║");
		System.err.println("║  2. Crie o banco: CREATE DATABASE nexus_db;                  ║");
		System.err.println("║  3. Ajuste src/config/security.properties se necessário     ║");
		System.err.println("║     URL atual: " + padRight(url, 44) + "║");
		System.err.println("╚══════════════════════════════════════════════════════════════╝");
		System.err.println();
	}

	private static String padRight(String s, int width) {
		if (s.length() >= width) {
			return s.substring(0, width);
		}
		return s + " ".repeat(width - s.length());
	}

	private static Connection criarNovaConexao() throws SQLException {
		return DriverManager.getConnection(url, user, password);
	}

	public static Connection obterConexao() throws SQLException {
		validarDisponibilidade();

		Connection conn = pool.poll();
		if (conn == null) {
			return criarNovaConexao();
		}
		if (conn.isClosed() || !conn.isValid(2)) {
			try {
				conn.close();
			} catch (Exception ignored) {}
			conn = criarNovaConexao();
		}
		return conn;
	}

	public static void liberarConexao(Connection conn) {
		if (conn == null || !disponivel) return;
		try {
			if (conn.isClosed()) {
				pool.offer(criarNovaConexao());
			} else {
				pool.offer(conn);
			}
		} catch (SQLException e) {
			System.err.println("[ConexaoDB] Erro ao liberar ou recriar conexão: " + e.getMessage());
		}
	}

	public static synchronized void fecharPool() {
		if (pool == null) return;
		for (Connection conn : pool) {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				System.err.println("[ConexaoDB] Erro ao fechar conexão do pool: " + e.getMessage());
			}
		}
		pool.clear();
		disponivel = false;
		System.out.println("[ConexaoDB] Pool de conexões encerrado.");
	}
}
