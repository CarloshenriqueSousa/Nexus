package Security;

import Persistencia.*;

/**
 * Define uma permissão granular para um caminho específico.
 * O admin pode atribuir permissões individuais a cada usuário,
 * controlando exatamente quais pastas/recursos podem ser acessados.
 * 
 * Exemplo de uso:
 * - Permissao.total("/*")                         → admin total
 * - Permissao.verEditar("/workspace/modelagem/*")  → modelador
 * - Permissao.somenteVer("/workspace/docs/*")      → visualizador
 */
@Entidade(tabela = "permissoes")
public class Permissao {

	@Id
	@Coluna(nome = "id", tipo = "SERIAL")
	private int id;

	@Coluna(nome = "usuario_id", tipo = "INTEGER")
	@ForeignKey(tabela = "usuarios", onDelete = "CASCADE")
	private int usuarioId;

	@Coluna(nome = "caminho_permitido", tipo = "VARCHAR(500)")
	private String caminhoPermitido; // Ex: "/modelagem/*", "/docs/*", "/*"

	@Coluna(nome = "pode_ver", tipo = "BOOLEAN", padrao = "false")
	private boolean podeVer;

	@Coluna(nome = "pode_editar", tipo = "BOOLEAN", padrao = "false")
	private boolean podeEditar;

	@Coluna(nome = "pode_deletar", tipo = "BOOLEAN", padrao = "false")
	private boolean podeDeletar;

	@Coluna(nome = "pode_criar", tipo = "BOOLEAN", padrao = "false")
	private boolean podeCriar;

	public Permissao() {
	}

	public Permissao(String caminhoPermitido, boolean podeVer, boolean podeEditar,
					 boolean podeDeletar, boolean podeCriar) {
		this.caminhoPermitido = caminhoPermitido;
		this.podeVer = podeVer;
		this.podeEditar = podeEditar;
		this.podeDeletar = podeDeletar;
		this.podeCriar = podeCriar;
	}

	public int getId() { return id; }
	public void setId(int id) { this.id = id; }

	public int getUsuarioId() { return usuarioId; }
	public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

	/** Cria permissão total (admin) */
	public static Permissao total(String caminho) {
		return new Permissao(caminho, true, true, true, true);
	}

	/** Cria permissão somente leitura */
	public static Permissao somenteVer(String caminho) {
		return new Permissao(caminho, true, false, false, false);
	}

	/** Cria permissão de ver e editar */
	public static Permissao verEditar(String caminho) {
		return new Permissao(caminho, true, true, false, false);
	}

	/** Cria permissão de ver, editar e criar */
	public static Permissao verEditarCriar(String caminho) {
		return new Permissao(caminho, true, true, false, true);
	}

	/**
	 * Verifica se um caminho é coberto por esta permissão.
	 * Suporta wildcard: "/modelagem/*" cobre "/modelagem/diagrama.xml"
	 */
	public boolean cobreCaminho(String caminho) {
		if (caminhoPermitido.equals("/*")) return true;
		if (caminhoPermitido.equals(caminho)) return true;
		if (caminhoPermitido.endsWith("/*")) {
			String prefixo = caminhoPermitido.substring(0, caminhoPermitido.length() - 2);
			return caminho.equals(prefixo) || caminho.startsWith(prefixo + "/");
		}
		return false;
	}

	// Getters
	public String getCaminhoPermitido() { return caminhoPermitido; }
	public boolean isPodeVer() { return podeVer; }
	public boolean isPodeEditar() { return podeEditar; }
	public boolean isPodeDeletar() { return podeDeletar; }
	public boolean isPodeCriar() { return podeCriar; }

	/**
	 * Serializa para formato de armazenamento: caminho:V,E,D,C
	 * Cada flag é incluída apenas se ativa.
	 */
	public String serializar() {
		StringBuilder sb = new StringBuilder(caminhoPermitido).append(":");
		if (podeVer) sb.append("V,");
		if (podeEditar) sb.append("E,");
		if (podeDeletar) sb.append("D,");
		if (podeCriar) sb.append("C,");
		String resultado = sb.toString();
		if (resultado.endsWith(",")) {
			resultado = resultado.substring(0, resultado.length() - 1);
		}
		return resultado;
	}

	/**
	 * Deserializa do formato: caminho:V,E,D,C
	 */
	public static Permissao deserializar(String texto) {
		String[] partes = texto.split(":", 2);
		if (partes.length < 2) {
			return somenteVer(partes[0]);
		}
		String caminho = partes[0];
		String flags = partes[1];
		return new Permissao(
			caminho,
			flags.contains("V"),
			flags.contains("E"),
			flags.contains("D"),
			flags.contains("C")
		);
	}
}
