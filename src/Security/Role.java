package Security;

/**
 * Cargos disponíveis no sistema.
 * O admin pode atribuir qualquer cargo a cada usuário do projeto.
 * Cada cargo define um nível de acesso padrão que pode ser customizado.
 */
public enum Role {

	ADMIN("Administrador", "Acesso total ao sistema — pode criar, editar, apagar e gerenciar usuários"),
	MODELADOR("Modelador", "Acesso às pastas de modelagem de dados"),
	ARQUITETO("Arquiteto", "Acesso aos arquivos de arquitetura e placas-mãe"),
	DESENVOLVEDOR("Desenvolvedor", "Acesso ao código-fonte e documentação técnica"),
	VISUALIZADOR("Visualizador", "Acesso somente leitura aos recursos permitidos");

	private final String nome;
	private final String descricao;

	Role(String nome, String descricao) {
		this.nome = nome;
		this.descricao = descricao;
	}

	public String getNome() { return nome; }
	public String getDescricao() { return descricao; }

	/**
	 * Converte string para Role. Retorna VISUALIZADOR se não encontrar.
	 */
	public static Role fromString(String valor) {
		try {
			return valueOf(valor.toUpperCase().trim());
		} catch (IllegalArgumentException e) {
			return VISUALIZADOR;
		}
	}
}
