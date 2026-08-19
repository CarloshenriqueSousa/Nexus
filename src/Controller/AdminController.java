package Controller;

import Data.JsonBuilder;
import Data.JsonParser;
import Http.HttpRequest;
import Http.HttpResponse;
import Http.HttpStatus;
import Router.Router;
import Security.Permissao;
import Security.Role;
import Security.User;
import Security.UserStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminController {

    private final Router router;

    public AdminController(Router router) {
        this.router = router;
    }

    public HttpResponse listarUsuarios(HttpRequest req) {
        User user = req.getUser();
        if (user == null || user.getCargo() != Role.ADMIN) {
            return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Não autorizado."));
        }

        UserStore store = router.getUserStore();
        List<User> todos = store.listarTodos();
        if (user.getTenantId() != 1) {
            todos = todos.stream().filter(u -> u.getTenantId() == user.getTenantId()).collect(java.util.stream.Collectors.toList());
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < todos.size(); i++) {
            if (i > 0) sb.append(",");
            User u = todos.get(i);
            var cargo = cargoParaJson(u);

            // Serializar permissões do usuário
            StringBuilder permsSb = new StringBuilder("[");
            List<Permissao> perms = u.getPermissoes();
            for (int j = 0; j < perms.size(); j++) {
                if (j > 0) permsSb.append(",");
                Permissao p = perms.get(j);
                permsSb.append(new JsonBuilder()
                        .add("id", p.getId())
                        .add("caminho", p.getCaminhoPermitido())
                        .add("ver", p.isPodeVer())
                        .add("editar", p.isPodeEditar())
                        .add("deletar", p.isPodeDeletar())
                        .add("criar", p.isPodeCriar())
                        .build());
            }
            permsSb.append("]");

            String userJson = new JsonBuilder()
                    .add("id", u.getId())
                    .add("username", u.getUsername())
                    .add("cargo", cargo.get("cargo"))
                    .add("cargo_nome", cargo.get("cargo_nome"))
                    .add("ativo", u.isAtivo())
                    .build();
            // Injetar permissões no JSON
            userJson = userJson.substring(0, userJson.length() - 1) + ",\"permissoes\":" + permsSb + "}";
            sb.append(userJson);
        }
        sb.append("]");

        return HttpResponse.ok().json(sb.toString());
    }

    public HttpResponse criarUsuario(HttpRequest req) {
        User user = req.getUser();
        if (user == null || user.getCargo() != Role.ADMIN) {
            return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Não autorizado."));
        }

        String corpo = req.getCorpo();
        String username = extrairCampoJson(corpo, "username");
        String senha = extrairCampoJson(corpo, "senha");
        String cargoStr = extrairCampoJson(corpo, "cargo");

        if (username == null || senha == null || cargoStr == null ||
                username.isBlank() || senha.isBlank() || cargoStr.isBlank()) {
            return HttpResponse.requisicaoInvalida("Campos 'username', 'senha' e 'cargo' são obrigatórios.");
        }

        Role cargo = Role.fromString(cargoStr);
        UserStore store = router.getUserStore();
        User novoUser = store.criarUsuario(username, senha, cargo, user.getTenantId());

        if (novoUser == null) {
            return HttpResponse.requisicaoInvalida("Usuário '" + username + "' já existe.");
        }

        String json = new JsonBuilder()
                .add("status", "success")
                .add("message", "Usuário criado com sucesso")
                .add("id", novoUser.getId())
                .add("username", novoUser.getUsername())
                .add("cargo", novoUser.getCargo().name())
                .build();
        return HttpResponse.ok().json(json);
    }

    public HttpResponse deletarUsuario(HttpRequest req) {
        User user = req.getUser();
        if (user == null || user.getCargo() != Role.ADMIN) {
            return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Não autorizado."));
        }

        String idStr = req.getParametrosQuery("id");
        if (idStr.isBlank()) {
            return HttpResponse.requisicaoInvalida("ID do usuário é obrigatório. Ex: ?id=2");
        }
        try {
            int id = Integer.parseInt(idStr);
            UserStore store = router.getUserStore();
            Optional<User> targetOpt = store.buscarPorId(id);
            if (targetOpt.isEmpty()) {
                return HttpResponse.naoEncontrado();
            }
            User target = targetOpt.get();
            if (user.getTenantId() != 1 && target.getTenantId() != user.getTenantId()) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Não autorizado a remover usuário deste inquilino."));
            }
            if (id == 1) {
                return HttpResponse.requisicaoInvalida("Não é possível remover o administrador principal.");
            }
            boolean removido = store.removerUsuario(id);
            if (!removido) {
                return HttpResponse.naoEncontrado();
            }
            return HttpResponse.ok().json(JsonBuilder.sucesso("Usuário removido com sucesso."));
        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        }
    }

    /**
     * PUT /api/admin/usuarios/{uid}/permissoes — Atualiza permissões de um usuário.
     * Body: { "permissoes": [{"caminho": "/api/projetos/*", "ver": true, "editar": true, "deletar": false, "criar": true}] }
     */
    public HttpResponse atualizarPermissoes(HttpRequest req) {
        User user = req.getUser();
        if (user == null || user.getCargo() != Role.ADMIN) {
            return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Não autorizado."));
        }

        try {
            int uid = Integer.parseInt(req.getParametroPath("uid"));
            UserStore store = router.getUserStore();
            Optional<User> optTarget = store.buscarPorId(uid);

            if (optTarget.isEmpty()) {
                return HttpResponse.naoEncontrado();
            }

            User target = optTarget.get();
            if (user.getTenantId() != 1 && target.getTenantId() != user.getTenantId()) {
                return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Não autorizado a modificar permissões deste usuário."));
            }

            Map<String, Object> body = JsonParser.parse(req.getCorpo());
            List<Object> permsList = JsonParser.getArray(body, "permissoes");

            if (permsList == null) {
                return HttpResponse.requisicaoInvalida("Campo 'permissoes' é obrigatório (array).");
            }

            java.util.List<Permissao> novasPermissoes = new java.util.ArrayList<>();
            for (Object item : permsList) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> permMap = (Map<String, Object>) item;
                    String caminho = JsonParser.getString(permMap, "caminho");
                    if (caminho == null || caminho.isBlank()) continue;

                    boolean ver = JsonParser.getBoolean(permMap, "ver", false);
                    boolean editar = JsonParser.getBoolean(permMap, "editar", false);
                    boolean deletar = JsonParser.getBoolean(permMap, "deletar", false);
                    boolean criar = JsonParser.getBoolean(permMap, "criar", false);

                    novasPermissoes.add(new Permissao(caminho, ver, editar, deletar, criar));
                }
            }

            boolean ok = store.atualizarPermissoes(uid, novasPermissoes);
            if (!ok) {
                return HttpResponse.erroInterno("Erro ao atualizar permissões.");
            }

            return HttpResponse.ok().json(JsonBuilder.sucesso("Permissões updated com sucesso. Total: " + novasPermissoes.size()));

        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao processar permissões: " + e.getMessage());
        }
    }

    private static Map<String, String> cargoParaJson(User user) {
        Role cargo = user.getCargo() != null ? user.getCargo() : Role.VISUALIZADOR;
        return Map.of("cargo", cargo.name(), "cargo_nome", cargo.getNome());
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

