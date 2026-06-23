package test.Security;

import Security.AuthManager;
import Security.User;
import Security.Role;

public class AuthManagerTest {

    public static void testGerarEValidarToken() throws Exception {
        AuthManager manager = new AuthManager(null, 1); // 1 hora de expiracao
        User user = new User(10, "testuser", "hash", "salt", Role.DESENVOLVEDOR);

        String token = manager.gerarToken(user);
        if (token == null || token.isBlank()) {
            throw new AssertionError("Token gerado esta vazio");
        }

        AuthManager.TokenInfo info = manager.validarToken(token);
        if (info == null) {
            throw new AssertionError("Validacao retornou null para um token valido");
        }

        if (info.getId() != 10) {
            throw new AssertionError("ID esperado 10, obtido: " + info.getId());
        }
        if (!"testuser".equals(info.getUsername())) {
            throw new AssertionError("Username esperado 'testuser', obtido: " + info.getUsername());
        }
        if (!"DESENVOLVEDOR".equals(info.getCargo())) {
            throw new AssertionError("Cargo esperado 'DESENVOLVEDOR', obtido: " + info.getCargo());
        }
    }

    public static void testTokenExpirado() throws Exception {
        AuthManager manager = new AuthManager(null, -1); // Expiracao negativa para forcar expirar
        User user = new User(10, "testuser", "hash", "salt", Role.DESENVOLVEDOR);

        String token = manager.gerarToken(user);
        AuthManager.TokenInfo info = manager.validarToken(token);
        if (info != null) {
            throw new AssertionError("Token expirado deveria ser invalidado, mas foi validado com sucesso");
        }
    }

    public static void testTokenAssinaturaInvalida() throws Exception {
        AuthManager manager = new AuthManager(null, 1);
        User user = new User(10, "testuser", "hash", "salt", Role.DESENVOLVEDOR);

        String token = manager.gerarToken(user);
        String[] partes = token.split("\\.");
        // Alterar a assinatura (parte 3)
        partes[2] = partes[2] + "x";
        String tokenAdulterado = partes[0] + "." + partes[1] + "." + partes[2];

        AuthManager.TokenInfo info = manager.validarToken(tokenAdulterado);
        if (info != null) {
            throw new AssertionError("Token com assinatura adulterada deveria ser invalidado");
        }
    }

    public static void testTokenMalformado() throws Exception {
        AuthManager manager = new AuthManager(null, 1);
        
        if (manager.validarToken("token_invalido_sem_pontos") != null) {
            throw new AssertionError("Token sem partes deveria retornar null");
        }
        if (manager.validarToken("parte1.parte2") != null) {
            throw new AssertionError("Token com duas partes deveria retornar null");
        }
        if (manager.validarToken("") != null) {
            throw new AssertionError("Token vazio deveria retornar null");
        }
        if (manager.validarToken(null) != null) {
            throw new AssertionError("Token null deveria retornar null");
        }
    }
}
