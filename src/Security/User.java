package Security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import Persistencia.*;

/**
 * Representa um usuário do sistema.
 * Cada usuário possui um cargo (Role) e permissões granulares
 * que definem exatamente quais pastas/recursos pode acessar.
 */
@Entidade(tabela = "usuarios")
public class User {

	@Id
	@Coluna(nome = "id", tipo = "SERIAL")
	private int id;

	@Coluna(nome = "username", tipo = "VARCHAR(100)", unico = true, naoNulo = true)
	private String username;

	@Coluna(nome = "senha_hash", tipo = "VARCHAR(128)", naoNulo = true)
	private String senhaHash;

	@Coluna(nome = "salt", tipo = "VARCHAR(64)", naoNulo = true)
	private String salt;

	@Coluna(nome = "cargo", tipo = "VARCHAR(50)", naoNulo = true)
	private Role cargo;

	@Coluna(nome = "ativo", tipo = "BOOLEAN", padrao = "true")
	private boolean ativo;

	@Coluna(nome = "deve_trocar_senha", tipo = "BOOLEAN", padrao = "false")
	private boolean deveTrocarSenha;

	@Coluna(nome = "tenant_id", tipo = "INT", naoNulo = true, padrao = "1")
	private int tenantId;

	private List<Permissao> permissoes;

	public User() {
		this.permissoes = new ArrayList<>();
		this.ativo = true;
		this.deveTrocarSenha = false;
		this.tenantId = 1;
	}

	public User(int id, String username, String senhaHash, String salt, Role cargo) {
		this.id = id;
		this.username = username;
		this.senhaHash = senhaHash;
		this.salt = salt;
		this.cargo = cargo;
		this.ativo = true;
		this.deveTrocarSenha = false;
		this.permissoes = new ArrayList<>();
		this.tenantId = 1;
	}

	// ==================== Getters ====================

	public int getId() { return id; }
	public String getUsername() { return username; }
	public String getSenhaHash() { return senhaHash; }
	public String getSalt() { return salt; }
	public Role getCargo() { return cargo; }
	public boolean isAtivo() { return ativo; }
	public boolean isDeveTrocarSenha() { return deveTrocarSenha; }
	public int getTenantId() { return tenantId; }
	public List<Permissao> getPermissoes() { return Collections.unmodifiableList(permissoes); }

	// ==================== Setters ====================

	public void setCargo(Role cargo) { this.cargo = cargo; }
	public void setAtivo(boolean ativo) { this.ativo = ativo; }
	public void setDeveTrocarSenha(boolean deveTrocarSenha) { this.deveTrocarSenha = deveTrocarSenha; }
	public void setTenantId(int tenantId) { this.tenantId = tenantId; }

	/**
	 * Atualiza a senha gerando novo salt e hash.
	 */
	public void setSenha(String novaSenha) {
		this.salt = SenhaUtil.gerarSalt();
		this.senhaHash = SenhaUtil.hashear(novaSenha, this.salt);
	}

	/**
	 * Verifica se a senha fornecida corresponde ao hash armazenado.
	 */
	public boolean verificarSenha(String senha) {
		return SenhaUtil.verificar(senha, this.senhaHash, this.salt);
	}

	// ==================== Permissões ====================

	public void adicionarPermissao(Permissao permissao) {
		permissoes.add(permissao);
	}

	public void limparPermissoes() {
		permissoes.clear();
	}

	/** Verifica se o usuário pode VISUALIZAR determinado caminho */
	public boolean podeVer(String caminho) {
		if (cargo == Role.ADMIN) return true;
		return permissoes.stream()
				.anyMatch(p -> p.cobreCaminho(caminho) && p.isPodeVer());
	}

	/** Verifica se o usuário pode EDITAR determinado caminho */
	public boolean podeEditar(String caminho) {
		if (cargo == Role.ADMIN) return true;
		return permissoes.stream()
				.anyMatch(p -> p.cobreCaminho(caminho) && p.isPodeEditar());
	}

	/** Verifica se o usuário pode DELETAR determinado caminho */
	public boolean podeDeletar(String caminho) {
		if (cargo == Role.ADMIN) return true;
		return permissoes.stream()
				.anyMatch(p -> p.cobreCaminho(caminho) && p.isPodeDeletar());
	}

	/** Verifica se o usuário pode CRIAR em determinado caminho */
	public boolean podeCriar(String caminho) {
		if (cargo == Role.ADMIN) return true;
		return permissoes.stream()
				.anyMatch(p -> p.cobreCaminho(caminho) && p.isPodeCriar());
	}

	// ==================== Serialização ====================

	/**
	 * Serializa para formato de armazenamento.
	 * Formato: id|username|senhaHash|salt|cargo|ativo|deveTrocarSenha|perm1;perm2;...
	 */
	public String serializar() {
		StringBuilder sb = new StringBuilder();
		sb.append(id).append("|");
		sb.append(username).append("|");
		sb.append(senhaHash).append("|");
		sb.append(salt).append("|");
		sb.append(cargo.name()).append("|");
		sb.append(ativo).append("|");
		sb.append(deveTrocarSenha).append("|");
		sb.append(tenantId).append("|");

		for (int i = 0; i < permissoes.size(); i++) {
			if (i > 0) sb.append(";");
			sb.append(permissoes.get(i).serializar());
		}
		return sb.toString();
	}

	/**
	 * Deserializa do formato de armazenamento.
	 */
	public static User deserializar(String linha) {
		String[] partes = linha.split("\\|", 8);
		if (partes.length < 7) {
			throw new IllegalArgumentException("Formato de usuário inválido: " + linha);
		}

		int id = Integer.parseInt(partes[0]);
		String username = partes[1];
		String senhaHash = partes[2];
		String salt = partes[3];
		Role cargo = Role.fromString(partes[4]);
		boolean ativo = Boolean.parseBoolean(partes[5]);
		boolean deveTrocarSenha = Boolean.parseBoolean(partes[6]);
		int tenantId = 1;

		User user = new User(id, username, senhaHash, salt, cargo);
		user.setAtivo(ativo);
		user.setDeveTrocarSenha(deveTrocarSenha);

		// Parse tenant_id and permissoes
		if (partes.length == 8) {
			String[] extraParts = partes[7].split("\\|", 2);
			if (extraParts.length > 0 && !extraParts[0].isBlank()) {
				try {
					tenantId = Integer.parseInt(extraParts[0]);
				} catch (NumberFormatException e) {
					// Fallback
				}
			}
			user.setTenantId(tenantId);
			if (extraParts.length == 2 && !extraParts[1].isBlank()) {
				String[] permStrs = extraParts[1].split(";");
				for (String permStr : permStrs) {
					if (!permStr.isBlank()) {
						user.adicionarPermissao(Permissao.deserializar(permStr));
					}
				}
			}
		} else {
			user.setTenantId(tenantId);
		}

		return user;
	}
}
