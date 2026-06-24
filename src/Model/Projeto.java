package Model;

import Persistencia.*;

@Entidade(tabela = "projetos")
public class Projeto {
    @Id
    @Coluna(nome = "id", tipo = "SERIAL")
    private int id;

    @Coluna(nome = "nome", tipo = "VARCHAR(200)", naoNulo = true)
    private String nome;

    @Coluna(nome = "descricao", tipo = "TEXT")
    private String descricao;

    @Coluna(nome = "criador_id", tipo = "INTEGER")
    private int criadorId;

    @Coluna(nome = "caminho_raiz", tipo = "VARCHAR(500)")
    private String caminhoRaiz;

    @Coluna(nome = "icone", tipo = "VARCHAR(50)", padrao = "'📁'")
    private String icone;

    @Coluna(nome = "criado_em", tipo = "BIGINT")
    private long criadoEm;

    @Coluna(nome = "atualizado_em", tipo = "BIGINT")
    private long atualizadoEm;

    @Coluna(nome = "tenant_id", tipo = "INT", naoNulo = true, padrao = "1")
    private int tenantId;

    public Projeto() {
        this.criadoEm = System.currentTimeMillis();
        this.atualizadoEm = System.currentTimeMillis();
        this.icone = "📁";
        this.tenantId = 1;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public int getCriadorId() { return criadorId; }
    public void setCriadorId(int criadorId) { this.criadorId = criadorId; }

    public String getCaminhoRaiz() { return caminhoRaiz; }
    public void setCaminhoRaiz(String caminhoRaiz) { this.caminhoRaiz = caminhoRaiz; }

    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }

    public long getCriadoEm() { return criadoEm; }
    public void setCriadoEm(long criadoEm) { this.criadoEm = criadoEm; }

    public long getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(long atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    public int getTenantId() { return tenantId; }
    public void setTenantId(int tenantId) { this.tenantId = tenantId; }
}
