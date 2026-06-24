package Data;

import java.util.*;

/**
 * Parser JSON robusto que substitui o frágil extrairCampoJson manual.
 * Suporta nested objects, arrays, strings, números, booleans e null.
 *
 * Uso:
 *   Map<String, Object> json = JsonParser.parse(corpo);
 *   String nome = JsonParser.getString(json, "nome");
 *   int id = JsonParser.getInt(json, "id");
 *   Map<String, Object> nested = JsonParser.getObject(json, "config");
 */
public class JsonParser {

	private final String input;
	private int pos;

	private JsonParser(String input) {
		this.input = input.trim();
		this.pos = 0;
	}

	// ==================== API Pública ====================

	/**
	 * Parseia uma string JSON e retorna um Map representando o objeto.
	 * @throws IllegalArgumentException se o JSON for inválido
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> parse(String json) {
		if (json == null || json.isBlank()) {
			return new HashMap<>();
		}
		JsonParser parser = new JsonParser(json);
		Object result = parser.parseValue();
		if (result instanceof Map) {
			return (Map<String, Object>) result;
		}
		throw new IllegalArgumentException("JSON root deve ser um objeto, recebido: " + result);
	}

	/**
	 * Parseia qualquer valor JSON (pode ser objeto, array, string, etc.)
	 */
	public static Object parseAny(String json) {
		if (json == null || json.isBlank()) return null;
		return new JsonParser(json).parseValue();
	}

	// ==================== Getters Seguros ====================

	public static String getString(Map<String, Object> map, String key) {
		if (map == null) return null;
		Object val = map.get(key);
		if (val == null) return null;
		return val.toString();
	}

	public static String getString(Map<String, Object> map, String key, String fallback) {
		String val = getString(map, key);
		return val != null ? val : fallback;
	}

	public static int getInt(Map<String, Object> map, String key) {
		return getInt(map, key, 0);
	}

	public static int getInt(Map<String, Object> map, String key, int fallback) {
		if (map == null) return fallback;
		Object val = map.get(key);
		if (val == null) return fallback;
		if (val instanceof Number) return ((Number) val).intValue();
		try {
			return Integer.parseInt(val.toString());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public static long getLong(Map<String, Object> map, String key) {
		return getLong(map, key, 0L);
	}

	public static long getLong(Map<String, Object> map, String key, long fallback) {
		if (map == null) return fallback;
		Object val = map.get(key);
		if (val == null) return fallback;
		if (val instanceof Number) return ((Number) val).longValue();
		try {
			return Long.parseLong(val.toString());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public static double getDouble(Map<String, Object> map, String key) {
		return getDouble(map, key, 0.0);
	}

	public static double getDouble(Map<String, Object> map, String key, double fallback) {
		if (map == null) return fallback;
		Object val = map.get(key);
		if (val == null) return fallback;
		if (val instanceof Number) return ((Number) val).doubleValue();
		try {
			return Double.parseDouble(val.toString());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public static boolean getBoolean(Map<String, Object> map, String key) {
		return getBoolean(map, key, false);
	}

	public static boolean getBoolean(Map<String, Object> map, String key, boolean fallback) {
		if (map == null) return fallback;
		Object val = map.get(key);
		if (val == null) return fallback;
		if (val instanceof Boolean) return (Boolean) val;
		return Boolean.parseBoolean(val.toString());
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getObject(Map<String, Object> map, String key) {
		if (map == null) return null;
		Object val = map.get(key);
		if (val instanceof Map) return (Map<String, Object>) val;
		return null;
	}

	@SuppressWarnings("unchecked")
	public static List<Object> getArray(Map<String, Object> map, String key) {
		if (map == null) return null;
		Object val = map.get(key);
		if (val instanceof List) return (List<Object>) val;
		return null;
	}

	/**
	 * Verifica se uma chave existe e não é null no mapa.
	 */
	public static boolean has(Map<String, Object> map, String key) {
		return map != null && map.containsKey(key) && map.get(key) != null;
	}

	// ==================== Parser Interno ====================

	private Object parseValue() {
		skipWhitespace();
		if (pos >= input.length()) {
			throw error("Fim inesperado do JSON");
		}

		char c = input.charAt(pos);
		return switch (c) {
			case '{' -> parseObject();
			case '[' -> parseArray();
			case '"' -> parseString();
			case 't', 'f' -> parseBoolean();
			case 'n' -> parseNull();
			default -> {
				if (c == '-' || Character.isDigit(c)) {
					yield parseNumber();
				}
				throw error("Caractere inesperado: '" + c + "'");
			}
		};
	}

	private Map<String, Object> parseObject() {
		Map<String, Object> map = new LinkedHashMap<>();
		expect('{');
		skipWhitespace();

		if (pos < input.length() && input.charAt(pos) == '}') {
			pos++;
			return map;
		}

		while (true) {
			skipWhitespace();
			String key = parseString();
			skipWhitespace();
			expect(':');
			Object value = parseValue();
			map.put(key, value);
			skipWhitespace();

			if (pos >= input.length()) break;
			if (input.charAt(pos) == '}') {
				pos++;
				return map;
			}
			expect(',');
		}

		return map;
	}

	private List<Object> parseArray() {
		List<Object> list = new ArrayList<>();
		expect('[');
		skipWhitespace();

		if (pos < input.length() && input.charAt(pos) == ']') {
			pos++;
			return list;
		}

		while (true) {
			Object value = parseValue();
			list.add(value);
			skipWhitespace();

			if (pos >= input.length()) break;
			if (input.charAt(pos) == ']') {
				pos++;
				return list;
			}
			expect(',');
		}

		return list;
	}

	private String parseString() {
		skipWhitespace();
		expect('"');
		StringBuilder sb = new StringBuilder();

		while (pos < input.length()) {
			char c = input.charAt(pos);
			if (c == '"') {
				pos++;
				return sb.toString();
			}
			if (c == '\\') {
				pos++;
				if (pos >= input.length()) {
					throw error("Sequência de escape incompleta");
				}
				char escaped = input.charAt(pos);
				switch (escaped) {
					case '"' -> sb.append('"');
					case '\\' -> sb.append('\\');
					case '/' -> sb.append('/');
					case 'n' -> sb.append('\n');
					case 'r' -> sb.append('\r');
					case 't' -> sb.append('\t');
					case 'b' -> sb.append('\b');
					case 'f' -> sb.append('\f');
					case 'u' -> {
						if (pos + 4 >= input.length()) {
							throw error("Sequência unicode incompleta");
						}
						String hex = input.substring(pos + 1, pos + 5);
						sb.append((char) Integer.parseInt(hex, 16));
						pos += 4;
					}
					default -> {
						sb.append('\\');
						sb.append(escaped);
					}
				}
			} else {
				sb.append(c);
			}
			pos++;
		}

		throw error("String não finalizada");
	}

	private Number parseNumber() {
		int start = pos;
		boolean isDouble = false;

		if (pos < input.length() && input.charAt(pos) == '-') pos++;

		while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;

		if (pos < input.length() && input.charAt(pos) == '.') {
			isDouble = true;
			pos++;
			while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
		}

		if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
			isDouble = true;
			pos++;
			if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) pos++;
			while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
		}

		String numStr = input.substring(start, pos);
		if (isDouble) {
			return Double.parseDouble(numStr);
		}

		long val = Long.parseLong(numStr);
		if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
			return (int) val;
		}
		return val;
	}

	private Boolean parseBoolean() {
		if (input.startsWith("true", pos)) {
			pos += 4;
			return Boolean.TRUE;
		}
		if (input.startsWith("false", pos)) {
			pos += 5;
			return Boolean.FALSE;
		}
		throw error("Boolean inválido");
	}

	private Object parseNull() {
		if (input.startsWith("null", pos)) {
			pos += 4;
			return null;
		}
		throw error("Null inválido");
	}

	// ==================== Utilitários ====================

	private void skipWhitespace() {
		while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
			pos++;
		}
	}

	private void expect(char c) {
		skipWhitespace();
		if (pos >= input.length() || input.charAt(pos) != c) {
			throw error("Esperado '" + c + "', encontrado: " +
					(pos < input.length() ? "'" + input.charAt(pos) + "'" : "EOF"));
		}
		pos++;
	}

	private IllegalArgumentException error(String msg) {
		int contextStart = Math.max(0, pos - 20);
		int contextEnd = Math.min(input.length(), pos + 20);
		String context = input.substring(contextStart, contextEnd);
		return new IllegalArgumentException(
				"[JsonParser] " + msg + " na posição " + pos + " | contexto: ..." + context + "...");
	}
}
