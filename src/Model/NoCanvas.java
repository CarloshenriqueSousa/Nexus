package Model;

import Persistencia.*;

@Entidade(tabela = "nos_canvas")
public class NoCanvas {
    @Id
    @Coluna(nome = "id", tipo = "SERIAL")
    private int id;

    @Coluna(nome = "projeto_id", tipo = "INTEGER", naoNulo = true)
    private int projetoId;

    @Coluna(nome = "arquivo_id", tipo = "INTEGER")
    private Integer arquivoId; // Nullable se for pasta ou nota

    @Coluna(nome = "tipo", tipo = "VARCHAR(50)", padrao = "'arquivo'")
    private String tipo; // "arquivo", "pasta", "nota", "grupo"

    @Coluna(nome = "titulo", tipo = "VARCHAR(300)")
    private String titulo;

    @Coluna(nome = "pos_x", tipo = "DOUBLE PRECISION", padrao = "0")
    private double posX;

    @Coluna(nome = "pos_y", tipo = "DOUBLE PRECISION", padrao = "0")
    private double posY;

    @Coluna(nome = "largura", tipo = "DOUBLE PRECISION", padrao = "220")
    private double largura;

    @Coluna(nome = "altura", tipo = "DOUBLE PRECISION", padrao = "150")
    private double altura;

    @Coluna(nome = "cor", tipo = "VARCHAR(20)", padrao = "'#3b82f6'")
    private String cor;

    @Coluna(nome = "dados_extra", tipo = "TEXT")
    private String dadosExtra; // JSON livre

    @Coluna(nome = "tenant_id", tipo = "INT", naoNulo = true, padrao = "1")
    private int tenantId;

    public NoCanvas() {
        this.tipo = "arquivo";
        this.largura = 220;
        this.altura = 150;
        this.cor = "#3b82f6";
        this.tenantId = 1;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjetoId() { return projetoId; }
    public void setProjetoId(int projetoId) { this.projetoId = projetoId; }

    public Integer getArquivoId() { return arquivoId; }
    public void setArquivoId(Integer arquivoId) { this.arquivoId = arquivoId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public double getPosX() { return posX; }
    public void setPosX(double posX) { this.posX = posX; }

    public double getPosY() { return posY; }
    public void setPosY(double posY) { this.posY = posY; }

    public double getLargura() { return largura; }
    public void setLargura(double largura) { this.largura = largura; }

    public double getAltura() { return altura; }
    public void setAltura(double altura) { this.altura = altura; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }

    public String getDadosExtra() { return dadosExtra; }
    public void setDadosExtra(String dadosExtra) { this.dadosExtra = dadosExtra; }

    public int getTenantId() { return tenantId; }
    public void setTenantId(int tenantId) { this.tenantId = tenantId; }
}
