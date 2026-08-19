package Model;

import Persistencia.*;

@Entidade(tabela = "ligacoes_canvas")
public class LigacaoCanvas {
    @Id
    @Coluna(nome = "id", tipo = "SERIAL")
    private int id;

    @Coluna(nome = "projeto_id", tipo = "INTEGER", naoNulo = true)
    @ForeignKey(tabela = "projetos", onDelete = "CASCADE")
    private int projetoId;

    @Coluna(nome = "origem_id", tipo = "INTEGER", naoNulo = true)
    private int origemId;

    @Coluna(nome = "destino_id", tipo = "INTEGER", naoNulo = true)
    private int destinoId;

    @Coluna(nome = "tipo", tipo = "VARCHAR(50)", padrao = "'referencia'")
    private String tipo; // "dependencia", "referencia", "fluxo"

    @Coluna(nome = "cor", tipo = "VARCHAR(20)", padrao = "'#64748b'")
    private String cor;

    @Coluna(nome = "label", tipo = "VARCHAR(200)")
    private String label;

    @Coluna(nome = "porta_origem", tipo = "VARCHAR(10)", padrao = "'right'")
    private String portaOrigem;

    @Coluna(nome = "porta_destino", tipo = "VARCHAR(10)", padrao = "'left'")
    private String portaDestino;

    public LigacaoCanvas() {
        this.tipo = "referencia";
        this.cor = "#64748b";
        this.portaOrigem = "right";
        this.portaDestino = "left";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjetoId() { return projetoId; }
    public void setProjetoId(int projetoId) { this.projetoId = projetoId; }

    public int getOrigemId() { return origemId; }
    public void setOrigemId(int origemId) { this.origemId = origemId; }

    public int getDestinoId() { return destinoId; }
    public void setDestinoId(int destinoId) { this.destinoId = destinoId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getPortaOrigem() { return portaOrigem; }
    public void setPortaOrigem(String portaOrigem) { this.portaOrigem = portaOrigem; }

    public String getPortaDestino() { return portaDestino; }
    public void setPortaDestino(String portaDestino) { this.portaDestino = portaDestino; }
}
