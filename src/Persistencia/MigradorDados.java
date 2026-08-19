package Persistencia;

import Security.User;
import Security.Permissao;
import Security.Role;
import Security.SenhaUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MigradorDados {

    public static void executar() {
        // 1. Garantir tabela _migracoes
        GerenciadorEntidade.executarDdl(
            "CREATE TABLE IF NOT EXISTS _migracoes (" +
            "id SERIAL PRIMARY KEY, " +
            "nome VARCHAR(200) UNIQUE NOT NULL, " +
            "executada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ");"
        );

        // 2. Verificar se a migração já rodou
        if (migracaoExecutada("migracao_inicial_usuarios")) {
            System.out.println("[MigradorDados] Migração de usuários já executada anteriormente.");
            return;
        }

        System.out.println("[MigradorDados] Iniciando migração de usuários do flat file para o PostgreSQL...");

        // 3. Procurar users.dat
        Path path = Paths.get("src/config/users.dat");
        if (!Files.exists(path)) {
            path = Paths.get("config/users.dat");
        }

        if (!Files.exists(path)) {
            System.out.println("[MigradorDados] Arquivo users.dat não encontrado. Pulando migração (tabelas serão inicializadas vazias ou com admin padrão via UserStore).");
            registrarMigracao("migracao_inicial_usuarios");
            return;
        }

        try {
            List<String> linhas = Files.readAllLines(path);
            int countUsers = 0;
            int countPerms = 0;

            for (String linha : linhas) {
                if (linha.isBlank() || linha.startsWith("#")) continue;
                try {
                    User user = deserializarUsuarioLegado(linha);
                    
                    // Salvar usuário via ORM
                    GerenciadorEntidade.salvar(user);
                    countUsers++;

                    // Salvar permissões via ORM
                    for (Permissao perm : user.getPermissoes()) {
                        perm.setUsuarioId(user.getId());
                        GerenciadorEntidade.salvar(perm);
                        countPerms++;
                    }
                } catch (Exception e) {
                    System.err.println("[MigradorDados] Erro ao migrar linha: " + linha + " - " + e.getMessage());
                }
            }

            // Sincronizar as sequences do PostgreSQL para evitar erro de ID duplicado no INSERT
            GerenciadorEntidade.executarDdl("SELECT setval(pg_get_serial_sequence('usuarios', 'id'), COALESCE(max(id), 1)) FROM usuarios;");
            GerenciadorEntidade.executarDdl("SELECT setval(pg_get_serial_sequence('permissoes', 'id'), COALESCE(max(id), 1)) FROM permissoes;");

            System.out.println("[MigradorDados] Migração concluída com sucesso: " + countUsers + " usuários e " + countPerms + " permissões migradas.");
            registrarMigracao("migracao_inicial_usuarios");

        } catch (IOException e) {
            System.err.println("[MigradorDados] Erro ao ler users.dat para migração: " + e.getMessage());
        }
    }

    private static boolean migracaoExecutada(String nomeMigracao) {
        String sql = "SELECT count(*) FROM _migracoes WHERE nome = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = ConexaoDB.obterConexao();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, nomeMigracao);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[MigradorDados] Erro ao verificar migração: " + e.getMessage());
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            ConexaoDB.liberarConexao(conn);
        }
        return false;
    }

    private static void registrarMigracao(String nomeMigracao) {
        String sql = "INSERT INTO _migracoes (nome) VALUES (?) ON CONFLICT DO NOTHING";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = ConexaoDB.obterConexao();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, nomeMigracao);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[MigradorDados] Erro ao registrar migração: " + e.getMessage());
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            ConexaoDB.liberarConexao(conn);
        }
    }

    /**
     * Deserializa usuário do formato legado flat-file (users.dat).
     * Formato: id|username|senhaHash|salt|cargo|ativo|deveTrocarSenha|tenantId|perm1;perm2;...
     * Método movido de User.java — único consumidor é esta migração.
     */
    private static User deserializarUsuarioLegado(String linha) {
        String[] partes = linha.split("\\|", 8);
        if (partes.length < 7) {
            throw new IllegalArgumentException("Formato de usuário inválido: " + linha);
        }

        int id = Integer.parseInt(partes[0]);
        String username = partes[1];
        String senhaHash = partes[2];
        String salt = partes[3];
        Role cargo = Role.fromString(partes[4]);
        boolean ativo = Boolean.parseBoolean(partes[5]);
        boolean deveTrocarSenha = Boolean.parseBoolean(partes[6]);
        int tenantId = 1;

        User user = new User(id, username, senhaHash, salt, cargo);
        user.setAtivo(ativo);
        user.setDeveTrocarSenha(deveTrocarSenha);

        if (partes.length == 8) {
            String[] extraParts = partes[7].split("\\|", 2);
            if (extraParts.length > 0 && !extraParts[0].isBlank()) {
                try {
                    tenantId = Integer.parseInt(extraParts[0]);
                } catch (NumberFormatException e) {
                    // Fallback
                }
            }
            user.setTenantId(tenantId);
            if (extraParts.length == 2 && !extraParts[1].isBlank()) {
                String[] permStrs = extraParts[1].split(";");
                for (String permStr : permStrs) {
                    if (!permStr.isBlank()) {
                        user.adicionarPermissao(Permissao.deserializar(permStr));
                    }
                }
            }
        } else {
            user.setTenantId(tenantId);
        }

        return user;
    }
}
