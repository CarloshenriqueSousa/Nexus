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

			// 3. Se nenhum usuário existir, criar o admin padrão no banco
			List<User> todos = listarTodos();
			if (todos.isEmpty()) {
				System.out.println("[UserStore] Banco de dados vazio. Criando admin padrão...");
				criarAdminPadrao();
			}
		} catch (Exception e) {
			throw new PersistenciaException("Falha ao inicializar o banco de dados para UserStore.", e);
		}
	}

	private void criarAdminPadrao() {
		String salt = SenhaUtil.gerarSalt();
		String hash = SenhaUtil.hashear("admin123", salt);
		User admin = new User(1, "admin", hash, salt, Role.ADMIN);
		admin.setDeveTrocarSenha(true);
		
		// Salvar o usuário para gerar o ID
		GerenciadorEntidade.salvar(admin);

		// Criar e salvar permissão
		Permissao perm = Permissao.total("/*");
		perm.setUsuarioId(admin.getId());
		GerenciadorEntidade.salvar(perm);
		
		admin.adicionarPermissao(perm);

		System.out.println("╔══════════════════════════════════════════════╗");
		System.out.println("║  Admin padrão criado no Banco de Dados!     ║");
		System.out.println("║  Usuário: admin                             ║");
		System.out.println("║  Senha:   admin123                          ║");
		System.out.println("║  ⚠ TROQUE A SENHA NO PRIMEIRO LOGIN!       ║");
		System.out.println("╚══════════════════════════════════════════════╝");
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
		if (buscarPorUsername(username).isPresent()) {
			return null;
		}

		String salt = SenhaUtil.gerarSalt();
		String hash = SenhaUtil.hashear(senha, salt);
		User novoUser = new User(0, username, hash, salt, cargo);

		// Salva usuário no banco (gera ID)
		GerenciadorEntidade.salvar(novoUser);

		// Atribui e salva as permissões padrão
		List<Permissao> permsPadrao = obterPermissoesPadrao(novoUser);
		for (Permissao p : permsPadrao) {
			p.setUsuarioId(novoUser.getId());
			GerenciadorEntidade.salvar(p);
			novoUser.adicionarPermissao(p);
		}

		System.out.println("[UserStore] Usuário criado no banco: " + username + " (cargo: " + cargo.getNome() + ")");
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
		GerenciadorEntidade.executarDdl("DELETE FROM permissoes WHERE usuario_id = " + id);

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
