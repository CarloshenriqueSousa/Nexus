package Model;

import Persistencia.Coluna;
import Persistencia.Entidade;
import Persistencia.Id;

@Entidade(tabela = "tenants")
public class Tenant {

    @Id
    @Coluna(nome = "id", tipo = "SERIAL")
    private int id;

    @Coluna(nome = "nome", tipo = "VARCHAR(150)", naoNulo = true)
    private String nome;

    @Coluna(nome = "dominio", tipo = "VARCHAR(100)", unico = true)
    private String dominio;

    @Coluna(nome = "ativo", tipo = "BOOLEAN", padrao = "true")
    private boolean ativo;

    @Coluna(nome = "plano", tipo = "VARCHAR(50)", padrao = "'INDIVIDUAL'")
    private String plano;

    public Tenant() {
        this.ativo = true;
        this.plano = "INDIVIDUAL";
    }

    public Tenant(int id, String nome, String dominio) {
        this.id = id;
        this.nome = nome;
        this.dominio = dominio;
        this.ativo = true;
        this.plano = "INDIVIDUAL";
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getDominio() { return dominio; }
    public boolean isAtivo() { return ativo; }
    public String getPlano() { return plano; }

    public void setNome(String nome) { this.nome = nome; }
    public void setDominio(String dominio) { this.dominio = dominio; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public void setPlano(String plano) { this.plano = plano; }
}
