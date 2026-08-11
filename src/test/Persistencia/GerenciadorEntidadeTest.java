package test.Persistencia;

import Security.User;
import Security.Permissao;
import Security.Role;
import Security.UserStore;
import Persistencia.ConexaoDB;
import Persistencia.GerenciadorEntidade;
import java.util.List;
import java.util.Optional;

public class GerenciadorEntidadeTest {

    public static void testCrudOperations() throws Exception {
        ConexaoDB.validarDisponibilidade();

        // 1. Criar entidade temporaria de User
        User user = new User(0, "__temp_test_user__", "hash", "salt", Role.DESENVOLVEDOR);
        
        // 2. Salvar
        User salvo = GerenciadorEntidade.salvar(user);
        if (salvo.getId() <= 0) {
            throw new AssertionError("ID do usuario salvo deveria ser positivo, obtido: " + salvo.getId());
        }

        // 3. Buscar por ID
        Optional<User> opt = GerenciadorEntidade.buscarPorId(User.class, salvo.getId());
        if (opt.isEmpty()) {
            throw new AssertionError("Usuario deveria ter sido encontrado por ID");
        }
        User buscado = opt.get();
        if (!"__temp_test_user__".equals(buscado.getUsername())) {
            throw new AssertionError("Username esperado '__temp_test_user__', obtido: " + buscado.getUsername());
        }

        // 4. Buscar por username
        List<User> list = GerenciadorEntidade.buscarPor(User.class, "username", "__temp_test_user__");
        if (list.isEmpty()) {
            throw new AssertionError("Usuario deveria ter sido encontrado por campo username");
        }

        // 5. Atualizar cargo
        buscado.setCargo(Role.ARQUITETO);
        GerenciadorEntidade.salvar(buscado);

        Optional<User> optAtualizado = GerenciadorEntidade.buscarPorId(User.class, buscado.getId());
        if (optAtualizado.isEmpty() || optAtualizado.get().getCargo() != Role.ARQUITETO) {
            throw new AssertionError("Cargo do usuario deveria ter sido atualizado para ARQUITETO");
        }

        // 6. Remover
        boolean removido = GerenciadorEntidade.remover(User.class, buscado.getId());
        if (!removido) {
            throw new AssertionError("Metodo remover retornou false para id existente");
        }

        // 7. Validar remocao
        Optional<User> optRemovido = GerenciadorEntidade.buscarPorId(User.class, buscado.getId());
        if (optRemovido.isPresent()) {
            throw new AssertionError("Usuario ainda existe apos remocao");
        }
    }

    public static void testUserStoreOperations() throws Exception {
        ConexaoDB.validarDisponibilidade();
        UserStore store = new UserStore(null);

        // 1. Criar usuario via UserStore
        User criado = store.criarUsuario("__store_test_user__", "senha123", Role.MODELADOR);
        if (criado == null) {
            throw new AssertionError("criarUsuario retornou null");
        }

        try {
            // 2. Buscar por username
            Optional<User> buscado = store.buscarPorUsername("__store_test_user__");
            if (buscado.isEmpty()) {
                throw new AssertionError("Usuario criado nao foi encontrado no store por username");
            }
            User user = buscado.get();
            if (user.getCargo() != Role.MODELADOR) {
                throw new AssertionError("Cargo esperado MODELADOR");
            }
            if (user.getPermissoes().isEmpty()) {
                throw new AssertionError("Usuario criado deveria possuir as permissoes padrao");
            }

            // 3. Atualizar cargo
            store.atualizarCargo(user.getId(), Role.VISUALIZADOR);
            Optional<User> buscadoAtualizado = store.buscarPorId(user.getId());
            if (buscadoAtualizado.isEmpty() || buscadoAtualizado.get().getCargo() != Role.VISUALIZADOR) {
                throw new AssertionError("Cargo nao foi atualizado via store");
            }

            // 4. Atualizar senha
            boolean senhaOk = store.atualizarSenha(user.getId(), "novaSenha123");
            if (!senhaOk) {
                throw new AssertionError("atualizarSenha retornou false");
            }
            // Buscar de novo para recarregar salt/hash e verificar
            Optional<User> buscadoSenha = store.buscarPorId(user.getId());
            if (buscadoSenha.isEmpty() || !buscadoSenha.get().verificarSenha("novaSenha123")) {
                throw new AssertionError("Nova senha nao confere ou falhou ao validar");
            }

        } finally {
            // 5. Limpar do banco
            store.removerUsuario(criado.getId());
            // Limpar permissoes explicitamente via PreparedStatement
            GerenciadorEntidade.executarAtualizacao("DELETE FROM permissoes WHERE usuario_id = ?", criado.getId());
        }
    }

    public static void testSequenceSyncAndUserCreation() throws Exception {
        ConexaoDB.validarDisponibilidade();

        // 1. Inserir usuário com ID explícito elevado (simulando inserção manual ou criação de admin)
        User userExpl = new User(9999, "__seq_test_explicit__", "hash", "salt", Role.VISUALIZADOR);
        GerenciadorEntidade.salvar(userExpl);

        try {
            // 2. Tentar criar novo usuário com ID autoincrementado (id = 0)
            User userAuto = new User(0, "__seq_test_auto__", "hash", "salt", Role.VISUALIZADOR);
            User autoSalvo = GerenciadorEntidade.salvar(userAuto);

            if (autoSalvo.getId() <= 9999) {
                throw new AssertionError("ID do auto-incremento deveria ser maior que 9999 após ressincronização da sequência. Obtido: " + autoSalvo.getId());
            }

            // 3. Limpar usuário auto-incrementado
            GerenciadorEntidade.remover(User.class, autoSalvo.getId());
        } finally {
            // 4. Limpar usuário com ID explícito
            GerenciadorEntidade.remover(User.class, 9999);
        }
    }
}
