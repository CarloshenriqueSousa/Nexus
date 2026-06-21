package Persistencia;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GerenciadorEntidade {

    public static void inicializarTabela(Class<?> classe) {
        if (!classe.isAnnotationPresent(Entidade.class)) {
            return;
        }

        Entidade entidade = classe.getAnnotation(Entidade.class);
        String nomeTabela = entidade.tabela();

        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(nomeTabela).append(" (\n");

        List<String> colunasSql = new ArrayList<>();
        Field[] fields = classe.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Coluna.class)) {
                Coluna col = field.getAnnotation(Coluna.class);
                StringBuilder colDef = new StringBuilder();
                colDef.append(col.nome()).append(" ").append(col.tipo());

                if (field.isAnnotationPresent(Id.class)) {
                    colDef.append(" PRIMARY KEY");
                } else {
                    if (col.naoNulo()) {
                        colDef.append(" NOT NULL");
                    }
                    if (col.unico()) {
                        colDef.append(" UNIQUE");
                    }
                    if (!col.padrao().isEmpty()) {
                        colDef.append(" DEFAULT ").append(col.padrao());
                    }
                }
                colunasSql.add(colDef.toString());
            }
        }

        sql.append(String.join(",\n", colunasSql));
        sql.append("\n);");

        System.out.println("[GerenciadorEntidade] Inicializando tabela: " + nomeTabela);
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = ConexaoDB.obterConexao();
            stmt = conn.createStatement();
            stmt.execute(sql.toString());
        } catch (SQLException e) {
            System.err.println("[GerenciadorEntidade] Erro ao inicializar tabela " + nomeTabela + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException ignored) {}
            }
            ConexaoDB.liberarConexao(conn);
        }
    }

    public static <T> T salvar(T obj) {
        Class<?> classe = obj.getClass();
        if (!classe.isAnnotationPresent(Entidade.class)) {
            throw new IllegalArgumentException("Classe não é uma @Entidade: " + classe.getName());
        }

        Entidade entidade = classe.getAnnotation(Entidade.class);
        String nomeTabela = entidade.tabela();

        Field idField = obterCampoId(classe);
        if (idField == null) {
            throw new IllegalArgumentException("Classe " + classe.getName() + " não possui campo com @Id");
        }

        idField.setAccessible(true);
        try {
            Object idValValue = idField.get(obj);
            int idVal = 0;
            if (idValValue instanceof Number) {
                idVal = ((Number) idValValue).intValue();
            }

            if (idVal == 0) {
                // INSERT
                return inserir(obj, nomeTabela, idField);
            } else {
                // UPDATE ou verificar se existe primeiro (UPSERT)
                // Se existe, UPDATE, senão INSERT
                if (buscarPorId(classe, idVal).isPresent()) {
                    return atualizar(obj, nomeTabela, idField, idVal);
                } else {
                    return inserirComId(obj, nomeTabela, idField, idVal);
                }
            }
        } catch (PersistenciaException e) {
            throw e;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao persistir entidade " + classe.getSimpleName(), e);
        } catch (Exception e) {
            throw new PersistenciaException("Erro ao persistir entidade " + classe.getSimpleName(), e);
        }
    }

    private static <T> T inserir(T obj, String nomeTabela, Field idField) throws Exception {
        Class<?> classe = obj.getClass();
        List<Field> camposColuna = new ArrayList<>();
        List<String> nomesColunas = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (Field field : classe.getDeclaredFields()) {
            if (field.isAnnotationPresent(Coluna.class) && !field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                Coluna col = field.getAnnotation(Coluna.class);
                nomesColunas.add(col.nome());
                placeholders.add("?");
                camposColuna.add(field);
            }
        }

        String sql = "INSERT INTO " + nomeTabela + " (" + String.join(", ", nomesColunas) + ") VALUES (" + String.join(", ", placeholders) + ")";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = ConexaoDB.obterConexao();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            int paramIndex = 1;
            for (Field field : camposColuna) {
                Object valor = field.get(obj);
                setParametroPstmt(pstmt, paramIndex++, valor, field.getType());
            }

            pstmt.executeUpdate();
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int generatedId = rs.getInt(1);
                idField.set(obj, generatedId);
            }
            return obj;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            ConexaoDB.liberarConexao(conn);
        }
    }

    private static <T> T inserirComId(T obj, String nomeTabela, Field idField, int idVal) throws Exception {
        Class<?> classe = obj.getClass();
        List<Field> camposColuna = new ArrayList<>();
        List<String> nomesColunas = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (Field field : classe.getDeclaredFields()) {
            if (field.isAnnotationPresent(Coluna.class)) {
                field.setAccessible(true);
                Coluna col = field.getAnnotation(Coluna.class);
                nomesColunas.add(col.nome());
                placeholders.add("?");
                camposColuna.add(field);
            }
        }

        String sql = "INSERT INTO " + nomeTabela + " (" + String.join(", ", nomesColunas) + ") VALUES (" + String.join(", ", placeholders) + ")";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = ConexaoDB.obterConexao();
            pstmt = conn.prepareStatement(sql);

            int paramIndex = 1;
            for (Field field : camposColuna) {
                Object valor = field.get(obj);
                setParametroPstmt(pstmt, paramIndex++, valor, field.getType());
            }

            pstmt.executeUpdate();
            return obj;
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            ConexaoDB.liberarConexao(conn);
        }
    }

    private static <T> T atualizar(T obj, String nomeTabela, Field idField, int idVal) throws Exception {
        Class<?> classe = obj.getClass();
        List<Field> camposColuna = new ArrayList<>();
        List<String> setClausula = new ArrayList<>();

        for (Field field : classe.getDeclaredFields()) {
            if (field.isAnnotationPresent(Coluna.class) && !field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                Coluna col = field.getAnnotation(Coluna.class);
                setClausula.add(col.nome() + " = ?");
                camposColuna.add(field);
            }
        }

        Coluna idCol = idField.getAnnotation(Coluna.class);
        String idColNome = (idCol != null) ? idCol.nome() : "id";

        String sql = "UPDATE " + nomeTabela + " SET " + String.join(", ", setClausula) + " WHERE " + idColNome + " = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = ConexaoDB.obterConexao();
            pstmt = conn.prepareStatement(sql);

            int paramIndex = 1;
            for (Field field : camposColuna) {
                Object valor = field.get(obj);
                setParametroPstmt(pstmt, paramIndex++, valor, field.getType());
            }
            pstmt.setInt(paramIndex, idVal);

            pstmt.executeUpdate();
            return obj;
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            ConexaoDB.liberarConexao(conn);
        }
    }

    public static <T> Optional<T> buscarPorId(Class<T> classe, int id) {
        if (!classe.isAnnotationPresent(Entidade.class)) {
            return Optional.empty();
        }

        Entidade entidade = classe.getAnnotation(Entidade.class);
        String nomeTabela = entidade.tabela();

        Field idField = obterCampoId(classe);
        if (idField == null) return Optional.empty();

        Coluna idCol = idField.getAnnotation(Coluna.class);
        String idColNome = (idCol != null) ? idCol.nome() : "id";

        String sql = "SELECT * FROM " + nomeTabela + " WHERE " + idColNome + " = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = ConexaoDB.obterConexao();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                T obj = mapearResultSetParaObjeto(rs, classe);
                return Optional.of(obj);
            }
        } catch (Exception e) {
            System.err.println("[GerenciadorEntidade] Erro ao buscar " + classe.getSimpleName() + " por ID: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            ConexaoDB.liberarConexao(conn);
        }
        return Optional.empty();
    }

    public static <T> List<T> buscarTodos(Class<T> classe) {
        List<T> resultado = new ArrayList<>();
        if (!classe.isAnnotationPresent(Entidade.class)) {
            return resultado;
        }

        Entidade entidade = classe.getAnnotation(Entidade.class);
        String nomeTabela = entidade.tabela();

        String sql = "SELECT * FROM " + nomeTabela;

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = ConexaoDB.obterConexao();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                T obj = mapearResultSetParaObjeto(rs, classe);
                resultado.add(obj);
            }
        } catch (Exception e) {
            System.err.println("[GerenciadorEntidade] Erro ao buscar todos de " + classe.getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConexaoDB.liberarConexao(conn);
        }
        return resultado;
    }

    public static <T> List<T> buscarPor(Class<T> classe, String campo, Object valor) {
        List<T> resultado = new ArrayList<>();
        if (!classe.isAnnotationPresent(Entidade.class)) {
            return resultado;
        }

        Entidade entidade = classe.getAnnotation(Entidade.class);
        String nomeTabela = entidade.tabela();

        // Mapeia o nome do campo da classe para a coluna do banco de dados (se houver correspondente)
        String nomeColuna = campo;
        try {
            Field field = classe.getDeclaredField(campo);
            if (field.isAnnotationPresent(Coluna.class)) {
                nomeColuna = field.getAnnotation(Coluna.class).nome();
            }
        } catch (NoSuchFieldException ignored) {}

        String sql = "SELECT * FROM " + nomeTabela + " WHERE " + nomeColuna + " = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = ConexaoDB.obterConexao();
            pstmt = conn.prepareStatement(sql);
            setParametroPstmt(pstmt, 1, valor, valor != null ? valor.getClass() : Object.class);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                T obj = mapearResultSetParaObjeto(rs, classe);
                resultado.add(obj);
            }
        } catch (Exception e) {
            System.err.println("[GerenciadorEntidade] Erro ao buscar " + classe.getSimpleName() + " por " + campo + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            ConexaoDB.liberarConexao(conn);
        }
        return resultado;
    }

    public static boolean remover(Class<?> classe, int id) {
        if (!classe.isAnnotationPresent(Entidade.class)) {
            return false;
        }

        Entidade entidade = classe.getAnnotation(Entidade.class);
        String nomeTabela = entidade.tabela();

        Field idField = obterCampoId(classe);
        if (idField == null) return false;

        Coluna idCol = idField.getAnnotation(Coluna.class);
        String idColNome = (idCol != null) ? idCol.nome() : "id";

        String sql = "DELETE FROM " + nomeTabela + " WHERE " + idColNome + " = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = ConexaoDB.obterConexao();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[GerenciadorEntidade] Erro ao remover " + classe.getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            ConexaoDB.liberarConexao(conn);
        }
    }

    public static void executarDdl(String sql) {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = ConexaoDB.obterConexao();
            stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("[GerenciadorEntidade] Erro ao executar DDL: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
            ConexaoDB.liberarConexao(conn);
        }
    }

    // ==================== Métodos Auxiliares de Mapeamento/Reflexão ====================

    private static Field obterCampoId(Class<?> classe) {
        for (Field field : classe.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        return null;
    }

    private static void setParametroPstmt(PreparedStatement pstmt, int index, Object valor, Class<?> tipo) throws SQLException {
        if (valor == null) {
            pstmt.setNull(index, Types.NULL);
        } else if (tipo.isEnum()) {
            pstmt.setString(index, ((Enum<?>) valor).name());
        } else if (tipo == int.class || tipo == Integer.class) {
            pstmt.setInt(index, (Integer) valor);
        } else if (tipo == long.class || tipo == Long.class) {
            pstmt.setLong(index, (Long) valor);
        } else if (tipo == double.class || tipo == Double.class) {
            pstmt.setDouble(index, (Double) valor);
        } else if (tipo == boolean.class || tipo == Boolean.class) {
            pstmt.setBoolean(index, (Boolean) valor);
        } else {
            pstmt.setString(index, valor.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapearResultSetParaObjeto(ResultSet rs, Class<T> classe) throws Exception {
        T obj = classe.getDeclaredConstructor().newInstance();

        for (Field field : classe.getDeclaredFields()) {
            if (field.isAnnotationPresent(Coluna.class)) {
                field.setAccessible(true);
                Coluna col = field.getAnnotation(Coluna.class);
                String colNome = col.nome();
                Class<?> tipoField = field.getType();

                Object dbValor = null;
                try {
                    dbValor = rs.getObject(colNome);
                } catch (SQLException e) {
                    // Ignora caso a coluna não exista no ResultSet
                    continue;
                }

                if (dbValor == null) {
                    continue;
                }

                if (tipoField.isEnum()) {
                    String strVal = dbValor.toString();
                    Class<? extends Enum> enumType = (Class<? extends Enum>) tipoField;
                    field.set(obj, Enum.valueOf(enumType, strVal));
                } else if (tipoField == int.class || tipoField == Integer.class) {
                    field.set(obj, ((Number) dbValor).intValue());
                } else if (tipoField == long.class || tipoField == Long.class) {
                    field.set(obj, ((Number) dbValor).longValue());
                } else if (tipoField == double.class || tipoField == Double.class) {
                    field.set(obj, ((Number) dbValor).doubleValue());
                } else if (tipoField == boolean.class || tipoField == Boolean.class) {
                    field.set(obj, (Boolean) dbValor);
                } else {
                    field.set(obj, dbValor.toString());
                }
            }
        }
        return obj;
    }
}
