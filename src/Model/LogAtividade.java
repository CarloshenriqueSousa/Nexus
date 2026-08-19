package Model;

import Persistencia.*;

/**
 * Registra ações de usuários em projetos para auditoria e timeline.
 * Cada registro captura quem fez o quê, quando, e em qual projeto.
 */
@Entidade(tabela = "log_atividade")
public class LogAtividade {

    @Id
    @Coluna(nome = "id", tipo = "SERIAL")
    private int id;

    @Coluna(nome = "acao", tipo = "VARCHAR(100)", naoNulo = true)
    private String acao; // Ex: "CRIAR_PROJETO", "REMOVER_NO", "UPLOAD_ARQUIVO"

    @Coluna(nome = "usuario_id", tipo = "INT", naoNulo = true)
    @ForeignKey(tabela = "usuarios", onDelete = "CASCADE")
    private int usuarioId;

    @Coluna(nome = "projeto_id", tipo = "INT")
    @ForeignKey(tabela = "projetos", onDelete = "CASCADE")
    private int projetoId;

    @Coluna(nome = "timestamp", tipo = "BIGINT", naoNulo = true)
    private long timestamp;

    @Coluna(nome = "detalhes", tipo = "TEXT")
    private String detalhes; // Informação adicional em texto livre

    @Coluna(nome = "username", tipo = "VARCHAR(100)")
    private String username; // Denormalizado para performance na listagem

    public LogAtividade() {
        this.timestamp = System.currentTimeMillis();
    }

    public LogAtividade(String acao, int usuarioId, int projetoId, String detalhes, String username) {
        this.acao = acao;
        this.usuarioId = usuarioId;
        this.projetoId = projetoId;
        this.detalhes = detalhes;
        this.username = username;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public int getId() { return id; }
    public String getAcao() { return acao; }
    public int getUsuarioId() { return usuarioId; }
    public int getProjetoId() { return projetoId; }
    public long getTimestamp() { return timestamp; }
    public String getDetalhes() { return detalhes; }
    public String getUsername() { return username; }

    // Setters
    public void setAcao(String acao) { this.acao = acao; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public void setProjetoId(int projetoId) { this.projetoId = projetoId; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
    public void setUsername(String username) { this.username = username; }

    /**
     * Helper para registrar uma atividade de forma concisa.
     */
    public static void registrar(String acao, Security.User user, int projetoId, String detalhes) {
        if (user == null) return;
        LogAtividade log = new LogAtividade(acao, user.getId(), projetoId, detalhes, user.getUsername());
        try {
            GerenciadorEntidade.salvar(log);
        } catch (Exception e) {
            System.err.println("[LogAtividade] Erro ao registrar atividade: " + e.getMessage());
        }
    }
}
