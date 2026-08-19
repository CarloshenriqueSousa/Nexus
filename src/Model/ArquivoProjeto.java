package Model;

import Persistencia.*;

@Entidade(tabela = "arquivos_projeto")
public class ArquivoProjeto {
    @Id
    @Coluna(nome = "id", tipo = "SERIAL")
    private int id;

    @Coluna(nome = "projeto_id", tipo = "INTEGER", naoNulo = true)
    @ForeignKey(tabela = "projetos", onDelete = "CASCADE")
    private int projetoId;

    @Coluna(nome = "nome", tipo = "VARCHAR(300)", naoNulo = true)
    private String nome;

    @Coluna(nome = "caminho", tipo = "VARCHAR(500)", naoNulo = true)
    private String caminho;

    @Coluna(nome = "tipo_mime", tipo = "VARCHAR(100)")
    private String tipoMime;

    @Coluna(nome = "tamanho_bytes", tipo = "BIGINT")
    private long tamanhoBytes;

    @Coluna(nome = "tipo_arquivo", tipo = "VARCHAR(50)")
    private String tipoArquivo; // "3d_model", "pcb_design", "autocad", "imagem", "documento", "codigo"

    @Coluna(nome = "thumbnail_path", tipo = "VARCHAR(500)")
    private String thumbnailPath;

    @Coluna(nome = "pasta_pai_id", tipo = "INTEGER")
    private Integer pastaPaiId;

    @Coluna(nome = "eh_pasta", tipo = "BOOLEAN", padrao = "false")
    private boolean ehPasta;

    @Coluna(nome = "criado_em", tipo = "BIGINT")
    private long criadoEm;

    public ArquivoProjeto() {
        this.criadoEm = System.currentTimeMillis();
        this.ehPasta = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjetoId() { return projetoId; }
    public void setProjetoId(int projetoId) { this.projetoId = projetoId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCaminho() { return caminho; }
    public void setCaminho(String caminho) { this.caminho = caminho; }

    public String getTipoMime() { return tipoMime; }
    public void setTipoMime(String tipoMime) { this.tipoMime = tipoMime; }

    public long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }

    public String getTipoArquivo() { return tipoArquivo; }
    public void setTipoArquivo(String tipoArquivo) { this.tipoArquivo = tipoArquivo; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public Integer getPastaPaiId() { return pastaPaiId; }
    public void setPastaPaiId(Integer pastaPaiId) { this.pastaPaiId = pastaPaiId; }

    public boolean isEhPasta() { return ehPasta; }
    public void setEhPasta(boolean ehPasta) { this.ehPasta = ehPasta; }

    public long getCriadoEm() { return criadoEm; }
    public void setCriadoEm(long criadoEm) { this.criadoEm = criadoEm; }
}
