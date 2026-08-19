package Controller;

import Data.JsonBuilder;
import Http.HttpRequest;
import Http.HttpResponse;
import Http.HttpStatus;
import Router.Router;
import Security.AuthManager;
import Security.Role;
import Security.User;
import Security.UserStore;
import java.util.Map;
import java.util.Optional;

public class AuthController {

    private final Router router;

    public AuthController(Router router) {
        this.router = router;
    }

    public HttpResponse login(HttpRequest req) {
        String usuario = null;
        String senha = null;

        String contentType = req.getCabecalhos("Content-Type").toLowerCase();
        if (contentType.contains("application/json")) {
            String corpo = req.getCorpo();
            usuario = extrairCampoJson(corpo, "username");
            senha = extrairCampoJson(corpo, "password");
            if (senha == null) {
                senha = extrairCampoJson(corpo, "senha");
            }
        } else {
            Map<String, String> formData = req.getFormData();
            usuario = formData.get("usuario");
            senha = formData.get("senha");
        }

        if (usuario == null || senha == null || usuario.isBlank() || senha.isBlank()) {
            return HttpResponse.requisicaoInvalida("Preencha todos os campos.");
        }

        UserStore store = router.getUserStore();
        Optional<User> optUser = store.buscarPorUsername(usuario);

        if (optUser.isEmpty() || !optUser.get().verificarSenha(senha)) {
            return new HttpResponse().status(HttpStatus.UNAUTHORIZED).json(JsonBuilder.erro("Usuário ou senha incorretos."));
        }

        User user = optUser.get();
        if (!user.isAtivo()) {
            return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Usuário inativo. Contate o administrador."));
        }

        String token = router.getAuthManager().gerarToken(user);
        int maxAge = 24 * 3600;

        String cargo = user.getCargo() != null ? user.getCargo().name() : Role.VISUALIZADOR.name();
        String cargoNome = user.getCargo() != null ? user.getCargo().getNome() : Role.VISUALIZADOR.getNome();

        String res = new JsonBuilder()
                .add("status", "success")
                .add("token", token)
                .add("id", user.getId())
                .add("username", user.getUsername())
                .add("cargo", cargo)
                .add("cargo_nome", cargoNome)
                .add("deve_trocar_senha", user.isDeveTrocarSenha())
                .build();

        return HttpResponse.ok().json(res).cookie("session_token", token, maxAge);
    }

    public HttpResponse me(HttpRequest req) {
        User user = req.getUser();
        if (user == null) {
            return new HttpResponse().status(HttpStatus.UNAUTHORIZED).json(JsonBuilder.erro("Não autenticado."));
        }
        Role cargo = user.getCargo() != null ? user.getCargo() : Role.VISUALIZADOR;
        
        StringBuilder permsBuilder = new StringBuilder("[");
        if (user.getPermissoes() != null) {
            boolean primeiro = true;
            for (Security.Permissao p : user.getPermissoes()) {
                if (!primeiro) permsBuilder.append(",");
                primeiro = false;
                permsBuilder.append(new JsonBuilder()
                        .add("caminho", p.getCaminhoPermitido())
                        .add("ver", p.isPodeVer())
                        .add("editar", p.isPodeEditar())
                        .build());
            }
        }
        permsBuilder.append("]");

        String json = new JsonBuilder()
                .add("id", user.getId())
                .add("username", user.getUsername())
                .add("cargo", cargo.name())
                .add("cargo_nome", cargo.getNome())
                .add("cargo_desc", cargo.getDescricao())
                .add("ativo", user.isAtivo())
                .build();
        
        // Injetar a lista de permissões em JSON
        String baseJson = json.substring(0, json.length() - 1) + ",\"permissoes\":" + permsBuilder.toString() + "}";
        
        return HttpResponse.ok().json(baseJson);
    }

    public HttpResponse logout(HttpRequest req) {
        return HttpResponse.ok().json(JsonBuilder.sucesso("Logout realizado com sucesso.")).limparCookie("session_token");
    }

    /**
     * Troca de senha forçada ou voluntária.
     * Requer autenticação. Aceita JSON: { senha_atual, nova_senha }
     */
    public HttpResponse trocaSenha(HttpRequest req) {
        User user = req.getUser();
        if (user == null) {
            return new HttpResponse().status(HttpStatus.UNAUTHORIZED).json(JsonBuilder.erro("Não autenticado."));
        }

        try {
            Map<String, Object> body = Data.JsonParser.parse(req.getCorpo());
            String senhaAtual = Data.JsonParser.getString(body, "senha_atual");
            String novaSenha = Data.JsonParser.getString(body, "nova_senha");

            if (senhaAtual == null || novaSenha == null || senhaAtual.isBlank() || novaSenha.isBlank()) {
                return HttpResponse.requisicaoInvalida("Preencha a senha atual e a nova senha.");
            }

            if (!user.verificarSenha(senhaAtual)) {
                return new HttpResponse().status(HttpStatus.UNAUTHORIZED).json(JsonBuilder.erro("Senha atual incorreta."));
            }

            if (novaSenha.length() < 6) {
                return HttpResponse.requisicaoInvalida("A nova senha deve ter no mínimo 6 caracteres.");
            }

            if (senhaAtual.equals(novaSenha)) {
                return HttpResponse.requisicaoInvalida("A nova senha não pode ser igual à atual.");
            }

            // Atualizar senha
            user.setSenha(novaSenha);
            user.setDeveTrocarSenha(false);
            Persistencia.GerenciadorEntidade.salvar(user);

            return HttpResponse.ok().json(JsonBuilder.sucesso("Senha alterada com sucesso."));
        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao processar troca de senha: " + e.getMessage());
        }
    }

    private String extrairCampoJson(String json, String campo) {
        try {
            java.util.Map<String, Object> map = Data.JsonParser.parse(json);
            return Data.JsonParser.getString(map, campo);
        } catch (Exception e) {
            return null;
        }
    }
}
