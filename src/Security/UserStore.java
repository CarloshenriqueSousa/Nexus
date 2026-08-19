package Security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import Persistencia.*;

/**
 * Armazena e gerencia usuários no PostgreSQL via GerenciadorEntidade.
 * Mantém compatibilidade com a API original.
 */
public class UserStore {

	public UserStore(String caminhoArquivo) {
		ConexaoDB.validarDisponibilidade();
		try {
			// 1. Inicializar as tabelas do banco de dados para segurança
			GerenciadorEntidade.inicializarTabela(User.class);
			GerenciadorEntidade.inicializarTabela(Permissao.class);

			// 2. Rodar a migração do arquivo users.dat para PostgreSQL
			MigradorDados.executar();

			// 3. Se nenhum usuário existir, criar os tenants e usuários padrão do SaaS
			List<User> todos = listarTodos();
			if (todos.isEmpty()) {
				System.out.println("[UserStore] Banco de dados vazio. Criando estrutura SaaS padrão...");
				criarSaaSEstruturaPadrao();
			}
		} catch (Exception e) {
			throw new PersistenciaException("Falha ao inicializar o banco de dados para UserStore.", e);
		}
	}

	private void criarSaaSEstruturaPadrao() {
		try {
			// 1. Criar Tenants
			Model.Tenant systemTenant = new Model.Tenant(1, "Sistema Principal", "system.local");
			systemTenant.setPlano("ENTERPRISE");
			GerenciadorEntidade.salvar(systemTenant);

			Model.Tenant alfaTenant = new Model.Tenant(2, "Empresa Alfa", "alfa.com");
			alfaTenant.setPlano("ENTERPRISE");
			GerenciadorEntidade.salvar(alfaTenant);

			Model.Tenant carlosTenant = new Model.Tenant(3, "Plano Individual - Carlos", "carlos.com");
			carlosTenant.setPlano("INDIVIDUAL");
			GerenciadorEntidade.salvar(carlosTenant);

			Model.Tenant joaoTenant = new Model.Tenant(4, "Plano Individual - João", "joao.com");
			joaoTenant.setPlano("INDIVIDUAL");
			GerenciadorEntidade.salvar(joaoTenant);

			// 2. Criar Usuários
			// Admin Sistema (Tenant 1)
			String salt1 = SenhaUtil.gerarSalt();
			String hash1 = SenhaUtil.hashear("admin123", salt1);
			User admin = new User(1, "admin", hash1, salt1, Role.ADMIN);
			admin.setDeveTrocarSenha(true);
			admin.setTenantId(1);
			GerenciadorEntidade.salvar(admin);
			criarSalvarPermissao(admin.getId(), "/*", true, true, true, true);

			// Admin Alfa (Tenant 2)
			String salt2 = SenhaUtil.gerarSalt();
			String hash2 = SenhaUtil.hashear("alfa123", salt2);
			User adminAlfa = new User(2, "admin_alfa", hash2, salt2, Role.ADMIN);
			adminAlfa.setTenantId(2);
			GerenciadorEntidade.salvar(adminAlfa);
			criarSalvarPermissao(adminAlfa.getId(), "/*", true, true, true, true);

			// Modelador Alfa (Tenant 2)
			String salt3 = SenhaUtil.gerarSalt();
			String hash3 = SenhaUtil.hashear("alfa123", salt3);
			User modAlfa = new User(3, "modelador_alfa", hash3, salt3, Role.MODELADOR);
			modAlfa.setTenantId(2);
			GerenciadorEntidade.salvar(modAlfa);
			for (Permissao p : obterPermissoesPadrao(modAlfa)) {
				p.setUsuarioId(modAlfa.getId());
				GerenciadorEntidade.salvar(p);
			}

			// Carlos (Tenant 3)
			String salt4 = SenhaUtil.gerarSalt();
			String hash4 = SenhaUtil.hashear("carlos123", salt4);
			User carlos = new User(4, "carlos", hash4, salt4, Role.MODELADOR);
			carlos.setTenantId(3);
			GerenciadorEntidade.salvar(carlos);
			for (Permissao p : obterPermissoesPadrao(carlos)) {
				p.setUsuarioId(carlos.getId());
				GerenciadorEntidade.salvar(p);
			}

			// João (Tenant 4)
			String salt5 = SenhaUtil.gerarSalt();
			String hash5 = SenhaUtil.hashear("joao123", salt5);
			User joao = new User(5, "joao", hash5, salt5, Role.MODELADOR);
			joao.setTenantId(4);
			GerenciadorEntidade.salvar(joao);
			for (Permissao p : obterPermissoesPadrao(joao)) {
				p.setUsuarioId(joao.getId());
				GerenciadorEntidade.salvar(p);
			}

			// Sincronizar as sequences do PostgreSQL
			GerenciadorEntidade.executarDdl("SELECT setval(pg_get_serial_sequence('tenants', 'id'), COALESCE(max(id), 1)) FROM tenants;");
			GerenciadorEntidade.executarDdl("SELECT setval(pg_get_serial_sequence('usuarios', 'id'), COALESCE(max(id), 1)) FROM usuarios;");
			GerenciadorEntidade.executarDdl("SELECT setval(pg_get_serial_sequence('permissoes', 'id'), COALESCE(max(id), 1)) FROM permissoes;");

			System.out.println("╔══════════════════════════════════════════════╗");
			System.out.println("║  SaaS Multi-Tenant inicializado com sucesso!║");
			System.out.println("║  Admin Sistema: admin / admin123            ║");
			System.out.println("║  Admin Empresa Alfa: admin_alfa / alfa123    ║");
			System.out.println("║  Carlos (Individual): carlos / carlos123    ║");
			System.out.println("║  João (Individual): joao / joao123          ║");
			System.out.println("╚══════════════════════════════════════════════╝");
		} catch (Exception e) {
			System.err.println("[UserStore] Erro ao criar dados SaaS padrão: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void criarSalvarPermissao(int userId, String caminho, boolean ver, boolean editar, boolean deletar, boolean criar) {
		Permissao perm = new Permissao(caminho, ver, editar, deletar, criar);
		perm.setUsuarioId(userId);
		GerenciadorEntidade.salvar(perm);
	}

	// ==================== Consultas ====================

	/** Busca usuário por username (case-insensitive) */
	public synchronized Optional<User> buscarPorUsername(String username) {
		List<User> encontrados = GerenciadorEntidade.buscarPor(User.class, "username", username);
		if (encontrados.isEmpty()) {
			return Optional.empty();
		}
		User user = encontrados.get(0);
		carregarPermissoes(user);
		return Optional.of(user);
	}

	/** Busca usuário por ID */
	public synchronized Optional<User> buscarPorId(int id) {
		Optional<User> opt = GerenciadorEntidade.buscarPorId(User.class, id);
		opt.ifPresent(this::carregarPermissoes);
		return opt;
	}

	/** Lista todos os usuários */
	public synchronized List<User> listarTodos() {
		List<User> todos = GerenciadorEntidade.buscarTodos(User.class);
		for (User user : todos) {
			carregarPermissoes(user);
		}
		return Collections.unmodifiableList(todos);
	}

	private void carregarPermissoes(User user) {
		user.limparPermissoes();
		List<Permissao> perms = GerenciadorEntidade.buscarPor(Permissao.class, "usuarioId", user.getId());
		for (Permissao perm : perms) {
			user.adicionarPermissao(perm);
		}
	}

	// ==================== CRUD ====================

	/**
	 * Cria novo usuário com permissões padrão do cargo.
	 * @return o usuário criado, ou null se username já existe
	 */
	public synchronized User criarUsuario(String username, String senha, Role cargo) {
		return criarUsuario(username, senha, cargo, 1);
	}

	public synchronized User criarUsuario(String username, String senha, Role cargo, int tenantId) {
		if (buscarPorUsername(username).isPresent()) {
			return null;
		}

		String salt = SenhaUtil.gerarSalt();
		String hash = SenhaUtil.hashear(senha, salt);
		User novoUser = new User(0, username, hash, salt, cargo);
		novoUser.setTenantId(tenantId);

		// Salva usuário no banco (gera ID)
		GerenciadorEntidade.salvar(novoUser);

		// Atribui e salva as permissões padrão
		List<Permissao> permsPadrao = obterPermissoesPadrao(novoUser);
		for (Permissao p : permsPadrao) {
			p.setUsuarioId(novoUser.getId());
			GerenciadorEntidade.salvar(p);
			novoUser.adicionarPermissao(p);
		}

		System.out.println("[UserStore] Usuário criado no banco: " + username + " (cargo: " + cargo.getNome() + ", tenant: " + tenantId + ")");
		return novoUser;
	}

	/**
	 * Remove usuário por ID. Não permite remover o admin principal (ID=1).
	 */
	public synchronized boolean removerUsuario(int id) {
		if (id == 1) return false;
		boolean removido = GerenciadorEntidade.remover(User.class, id);
		if (removido) {
			System.out.println("[UserStore] Usuário ID " + id + " removido do banco.");
		}
		return removido;
	}

	/**
	 * Atualiza permissões de um usuário.
	 */
	public synchronized boolean atualizarPermissoes(int id, List<Permissao> novasPermissoes) {
		Optional<User> opt = buscarPorId(id);
		if (opt.isEmpty()) return false;

		// Deletar permissões antigas
		GerenciadorEntidade.executarDelete("permissoes", "usuario_id", id);

		// Salvar as novas
		for (Permissao p : novasPermissoes) {
			p.setUsuarioId(id);
			GerenciadorEntidade.salvar(p);
		}
		
		System.out.println("[UserStore] Permissões atualizadas no banco para ID: " + id);
		return true;
	}

	/**
	 * Atualiza cargo de um usuário.
	 */
	public synchronized boolean atualizarCargo(int id, Role novoCargo) {
		Optional<User> opt = buscarPorId(id);
		if (opt.isEmpty()) return false;

		User user = opt.get();
		user.setCargo(novoCargo);
		GerenciadorEntidade.salvar(user);
		System.out.println("[UserStore] Cargo do ID " + id + " alterado para: " + novoCargo.getNome());
		return true;
	}

	/**
	 * Atualiza senha de um usuário.
	 */
	public synchronized boolean atualizarSenha(int id, String novaSenha) {
		Optional<User> opt = buscarPorId(id);
		if (opt.isEmpty()) return false;

		User user = opt.get();
		user.setSenha(novaSenha);
		user.setDeveTrocarSenha(false);
		GerenciadorEntidade.salvar(user);
		System.out.println("[UserStore] Senha alterada no banco para ID: " + id);
		return true;
	}

	// ==================== Permissões Padrão por Cargo ====================

	private List<Permissao> obterPermissoesPadrao(User user) {
		List<Permissao> perms = new ArrayList<>();
		switch (user.getCargo()) {
			case ADMIN -> {
				perms.add(Permissao.total("/*"));
			}
			case MODELADOR -> {
				perms.add(Permissao.verEditar("/workspace/modelagem/*"));
				perms.add(Permissao.somenteVer("/workspace/docs/*"));
				perms.add(Permissao.somenteVer("/workspace/*"));
				perms.add(Permissao.total("/api/projetos/*"));
				perms.add(Permissao.total("/workspace/projeto/*"));
			}
			case ARQUITETO -> {
				perms.add(Permissao.verEditar("/workspace/arquitetura/*"));
				perms.add(Permissao.somenteVer("/workspace/docs/*"));
				perms.add(Permissao.somenteVer("/workspace/*"));
				perms.add(Permissao.total("/api/projetos/*"));
				perms.add(Permissao.total("/workspace/projeto/*"));
			}
			case DESENVOLVEDOR -> {
				perms.add(Permissao.verEditar("/workspace/src/*"));
				perms.add(Permissao.verEditar("/workspace/docs/*"));
				perms.add(Permissao.somenteVer("/workspace/*"));
				perms.add(Permissao.total("/api/projetos/*"));
				perms.add(Permissao.total("/workspace/projeto/*"));
			}
			case VISUALIZADOR -> {
				perms.add(Permissao.somenteVer("/workspace/*"));
				perms.add(Permissao.somenteVer("/api/projetos/*"));
				perms.add(Permissao.somenteVer("/workspace/projeto/*"));
			}
		}
		return perms;
	}
}
