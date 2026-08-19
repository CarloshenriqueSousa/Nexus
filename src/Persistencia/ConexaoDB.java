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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConexaoDB {
	private static BlockingQueue<Connection> pool;
	private static String url;
	private static String user;
	private static String password;
	private static int poolSize;
	private static volatile boolean disponivel = false;

	// Teto de conexões: rastreia conexões criadas fora do pool (overflow)
	private static final AtomicInteger conexoesExternas = new AtomicInteger(0);
	private static int maxConexoesTotal; // poolSize * 2 = teto absoluto

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
		} else {
			// Verificar se ao menos as variáveis de ambiente estão definidas
			boolean temEnvDB = System.getenv("DB_URL") != null && !System.getenv("DB_URL").isBlank();
			if (!temEnvDB) {
				System.err.println();
				System.err.println("╔══════════════════════════════════════════════════════════════════╗");
				System.err.println("║  ⚠  AVISO: security.properties NÃO ENCONTRADO                 ║");
				System.err.println("╠══════════════════════════════════════════════════════════════════╣");
				System.err.println("║  Nenhum arquivo de configuração foi encontrado em:              ║");
				System.err.println("║    - src/config/security.properties                             ║");
				System.err.println("║    - config/security.properties                                 ║");
				System.err.println("║                                                                  ║");
				System.err.println("║  E nenhuma variável de ambiente DB_URL está definida.            ║");
				System.err.println("║                                                                  ║");
				System.err.println("║  Copie o arquivo security.properties.example e ajuste:           ║");
				System.err.println("║    cp src/config/security.properties.example \\                   ║");
				System.err.println("║       src/config/security.properties                             ║");
				System.err.println("║                                                                  ║");
				System.err.println("║  Usando valores padrão (localhost:5432/nexus_db).                ║");
				System.err.println("╚══════════════════════════════════════════════════════════════════╝");
				System.err.println();
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
		maxConexoesTotal = poolSize * 2; // Teto: dobro do pool base

		pool = new LinkedBlockingQueue<>(poolSize);
		disponivel = false;

		try {
			Class.forName("org.postgresql.Driver");

			int maxRetries = 10;
			int delayMs = 2000;
			boolean conectado = false;
			SQLException ultimaEx = null;

			for (int tentativa = 1; tentativa <= maxRetries; tentativa++) {
				try {
					// Testa criar uma conexão para ver se o banco está pronto
					Connection teste = criarNovaConexao();
					teste.close();
					
					// Preenche o pool
					for (int i = 0; i < poolSize; i++) {
						pool.add(criarNovaConexao());
					}
					conectado = true;
					disponivel = true;
					System.out.println("[ConexaoDB] Pool de conexões inicializado com " + poolSize + " conexões na tentativa " + tentativa + ".");
					break;
				} catch (SQLException e) {
					ultimaEx = e;
					System.err.println("[ConexaoDB] Tentativa " + tentativa + " de conexão falhou. Banco pode estar iniciando...");
					try {
						Thread.sleep(delayMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}

			if (!conectado) {
				System.err.println("[ConexaoDB] Não foi possível conectar ao banco após " + maxRetries + " tentativas.");
				if (ultimaEx != null) {
					System.err.println("[ConexaoDB] Último erro: " + ultimaEx.getMessage());
				}
			}
		} catch (ClassNotFoundException e) {
			System.err.println("[ConexaoDB] Driver do PostgreSQL não encontrado no classpath!");
			e.printStackTrace();
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

	private static final ThreadLocal<Connection> conexaoThreadLocal = new ThreadLocal<>();

	public static Connection obterConexao() throws SQLException {
		validarDisponibilidade();

		Connection connTx = conexaoThreadLocal.get();
		if (connTx != null && !connTx.isClosed()) {
			return connTx; // Reutiliza a conexão da transação atual
		}

		// Tentar obter do pool (non-blocking)
		Connection conn = pool.poll();
		if (conn != null) {
			if (conn.isClosed() || !conn.isValid(2)) {
				try { conn.close(); } catch (Exception ignored) {}
				conn = criarNovaConexao();
			}
			return conn;
		}

		// Pool vazio: verificar se podemos criar uma conexão extra (respeitando o teto)
		if (conexoesExternas.get() < maxConexoesTotal - poolSize) {
			conexoesExternas.incrementAndGet();
			try {
				return criarNovaConexao();
			} catch (SQLException e) {
				conexoesExternas.decrementAndGet();
				throw e;
			}
		}

		// Teto atingido: aguardar com timeout de 3 segundos
		try {
			conn = pool.poll(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SQLException("[ConexaoDB] Thread interrompida ao aguardar conexão.");
		}

		if (conn == null) {
			throw new SQLException("[ConexaoDB] Pool esgotado. Todas as " + maxConexoesTotal + " conexões em uso. Tente novamente.");
		}

		if (conn.isClosed() || !conn.isValid(2)) {
			try { conn.close(); } catch (Exception ignored) {}
			conn = criarNovaConexao();
		}
		return conn;
	}

	public static void liberarConexao(Connection conn) {
		if (conn == null || !disponivel) return;

		// Não devolve a conexão ao pool se ela fizer parte de uma transação ativa
		if (conexaoThreadLocal.get() == conn) {
			return;
		}

		// Se o pool está cheio, fechar a conexão extra e decrementar o contador
		if (!pool.offer(conn)) {
			try { conn.close(); } catch (SQLException ignored) {}
			conexoesExternas.decrementAndGet();
			return;
		}
	}

	private static void devolverAoPool(Connection conn) {
		try {
			if (conn.isClosed()) {
				// Conexão morta: recriar apenas se cabe no pool
				Connection nova = criarNovaConexao();
				if (!pool.offer(nova)) {
					nova.close();
					conexoesExternas.decrementAndGet();
				}
			} else {
				if (!pool.offer(conn)) {
					conn.close();
					conexoesExternas.decrementAndGet();
				}
			}
		} catch (SQLException e) {
			System.err.println("[ConexaoDB] Erro ao liberar ou recriar conexão: " + e.getMessage());
			conexoesExternas.decrementAndGet();
		}
	}

	public static void iniciarTransacao() throws SQLException {
		Connection conn = obterConexao();
		conn.setAutoCommit(false);
		conexaoThreadLocal.set(conn);
	}

	public static void comitarTransacao() throws SQLException {
		Connection conn = conexaoThreadLocal.get();
		if (conn != null) {
			try {
				conn.commit();
				conn.setAutoCommit(true);
			} finally {
				conexaoThreadLocal.remove();
				devolverAoPool(conn);
			}
		}
	}

	public static void rollbackTransacao() {
		Connection conn = conexaoThreadLocal.get();
		if (conn != null) {
			try {
				conn.rollback();
				conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			} finally {
				conexaoThreadLocal.remove();
				devolverAoPool(conn);
			}
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
