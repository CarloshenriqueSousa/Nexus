package Data;

import java.util.*;

/**
 * CONCEITO: Padrão builder + Encadeamento de métodos
 * O builder Pattern permite construir objetos complexos passo a passo
 * Cada método retorna 'this' ou seja a pŕopia instancia
 * 
 * CONCEITO: StringBuilder
 * String em Java é umitável: cada concatenação cria um novo objeto
 * StringBuilder é mútavel e muito mais eficiente para construir strings incrementalmente
 */
public class JsonBuilder {
	private final StringBuilder sb = new StringBuilder();
	private boolean primeiroItem = true; // Controla se precisa de vírgula ou não
	public JsonBuilder(){
		sb.append("{");
	}
	
	/**
	 * CONCEITO:Sobrecarga de método
	 * Podemos ter vários métodos com o mesmo nome, desde que tenham parametros diferentes
	 */
	
	public JsonBuilder add(String chave, Object valor) {
		if(!primeiroItem) sb.append(",");
		primeiroItem = false;
		
		sb.append("\"").append(escapar(chave)).append("\":");
		adicionarValor(valor);
		return this;
		
	}
	
	public JsonBuilder add(String chave, int valor) {
		return add(chave, (Object) valor);
	}
	
	public JsonBuilder add(String chave, boolean valor) {
		return add(chave, (Object) valor);
	}

	public JsonBuilder add(String chave, long valor) {
		return add(chave, (Object) valor);
	}
	
	private void adicionarValor(Object valor) {
		if(valor == null) {
			sb.append("null");
		} else if(valor instanceof String) {
			sb.append("\"").append(escapar((String) valor)).append("\"");
		} else if (valor instanceof Boolean || valor instanceof Number) {
			sb.append(valor);
		} else if(valor instanceof List) {
			/**
			 * CONCEITO: WildCard (<?>)
			 * List<?> significa "uma lista de qualquer tipo
			 * Usado pára quando não é importante o tipo exato do elemento
			 */
			
			List<?> lista = (List<?>) valor;
			sb.append("[");
			for(int i = 0; i < lista.size(); i++) {
				if (i > 0) sb.append(",");
				adicionarValor(lista.get(i));
			}
			sb.append("]");
		} else {
			//Converte para String usando toString(), comumente chamado Fallback
			sb.append("\"").append(escapar(valor.toString())).append("\"");
			
		}
	}
	
	private String escapar(String s) {
		/**
		 * CONCEITO: Encadeamento em Strings
		 * replace() retorna uma nova String. Encadeamento das chamadas para aplicar múltiplas substituições em sequencia
		 */
		
		return s.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}
	
	public String build() {
		return sb.toString() + "}";
	}
	
	/**
	 * CONCEITO: Métodos fábricados estáticos
	 * COnvenientes para criar objetos JSON comuns sem precisar instanciar o builder manualmente
	 */
	
	public static String erro(String messagem) {
		return new JsonBuilder()
				.add("error", messagem)
				.build();
	}
	
	public static String sucesso(String mensagem) {
		return new JsonBuilder()
				.add("status", "success")
				.add("message", mensagem)
				.build();
	}
}
