package Persistencia;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para declarar Foreign Key constraints em campos de entidade.
 * 
 * Uso: @ForeignKey(tabela = "projetos", coluna = "id", onDelete = "CASCADE")
 * 
 * O GerenciadorEntidade criará automaticamente a FK constraint após
 * inicializar a tabela via ALTER TABLE ... ADD CONSTRAINT IF NOT EXISTS.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ForeignKey {
    /** Tabela referenciada (ex: "projetos") */
    String tabela();

    /** Coluna referenciada na tabela alvo (ex: "id") */
    String coluna() default "id";

    /** Ação ON DELETE (ex: "CASCADE", "SET NULL", "RESTRICT") */
    String onDelete() default "NO ACTION";
}
