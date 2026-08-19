package Controller;

import Data.JsonBuilder;
import Data.JsonParser;
import Http.HttpRequest;
import Http.HttpResponse;
import Http.HttpStatus;
import Model.Tenant;
import Persistencia.GerenciadorEntidade;
import Security.Role;
import Security.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CRUD de Tenants — apenas acessível por ADMIN.
 */
public class TenantController {

    /**
     * GET /api/admin/tenants — Lista todos os tenants.
     */
    public HttpResponse listarTenants(HttpRequest req) {
        User user = req.getUser();
        if (user == null || user.getCargo() != Role.ADMIN) {
            return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso restrito a administradores."));
        }

        List<Tenant> tenants = GerenciadorEntidade.buscarTodos(Tenant.class);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tenants.size(); i++) {
            if (i > 0) sb.append(",");
            Tenant t = tenants.get(i);
            sb.append(new JsonBuilder()
                    .add("id", t.getId())
                    .add("nome", t.getNome())
                    .add("dominio", t.getDominio() != null ? t.getDominio() : "")
                    .add("ativo", t.isAtivo())
                    .build());
        }
        sb.append("]");
        return HttpResponse.ok().json(sb.toString());
    }

    /**
     * POST /api/admin/tenants — Cria um novo tenant.
     * Body: { "nome": "...", "dominio": "..." }
     */
    public HttpResponse criarTenant(HttpRequest req) {
        User user = req.getUser();
        if (user == null || user.getCargo() != Role.ADMIN) {
            return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso restrito a administradores."));
        }

        try {
            Map<String, Object> body = JsonParser.parse(req.getCorpo());
            String nome = JsonParser.getString(body, "nome");
            String dominio = JsonParser.getString(body, "dominio");

            if (nome == null || nome.isBlank()) {
                return HttpResponse.requisicaoInvalida("O campo 'nome' é obrigatório.");
            }

            Tenant tenant = new Tenant();
            tenant.setNome(nome.trim());
            if (dominio != null && !dominio.isBlank()) {
                tenant.setDominio(dominio.trim());
            }

            GerenciadorEntidade.salvar(tenant);

            String res = new JsonBuilder()
                    .add("status", "success")
                    .add("id", tenant.getId())
                    .add("nome", tenant.getNome())
                    .build();
            return new HttpResponse().status(HttpStatus.CREATED).json(res);

        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao criar tenant: " + e.getMessage());
        }
    }

    /**
     * PUT /api/admin/tenants/{id} — Atualiza um tenant existente.
     * Body: { "nome": "...", "dominio": "..." }
     */
    public HttpResponse atualizarTenant(HttpRequest req) {
        User user = req.getUser();
        if (user == null || user.getCargo() != Role.ADMIN) {
            return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso restrito a administradores."));
        }

        try {
            int id = Integer.parseInt(req.getParametroPath("id"));
            Optional<Tenant> opt = GerenciadorEntidade.buscarPorId(Tenant.class, id);
            if (opt.isEmpty()) {
                return HttpResponse.naoEncontrado();
            }

            Tenant tenant = opt.get();
            Map<String, Object> body = JsonParser.parse(req.getCorpo());

            String nome = JsonParser.getString(body, "nome");
            if (nome != null && !nome.isBlank()) {
                tenant.setNome(nome.trim());
            }

            String dominio = JsonParser.getString(body, "dominio");
            if (dominio != null) {
                tenant.setDominio(dominio.isBlank() ? null : dominio.trim());
            }

            GerenciadorEntidade.salvar(tenant);

            return HttpResponse.ok().json(JsonBuilder.sucesso("Tenant atualizado com sucesso."));

        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao atualizar tenant: " + e.getMessage());
        }
    }

    /**
     * DELETE /api/admin/tenants/{id} — Soft-delete: desativa o tenant.
     * O tenant padrão (id=1) não pode ser desativado.
     */
    public HttpResponse desativarTenant(HttpRequest req) {
        User user = req.getUser();
        if (user == null || user.getCargo() != Role.ADMIN) {
            return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Acesso restrito a administradores."));
        }

        try {
            int id = Integer.parseInt(req.getParametroPath("id"));

            if (id == 1) {
                return HttpResponse.requisicaoInvalida("O tenant padrão (id=1) não pode ser desativado.");
            }

            Optional<Tenant> opt = GerenciadorEntidade.buscarPorId(Tenant.class, id);
            if (opt.isEmpty()) {
                return HttpResponse.naoEncontrado();
            }

            Tenant tenant = opt.get();
            tenant.setAtivo(false);
            GerenciadorEntidade.salvar(tenant);

            return HttpResponse.ok().json(JsonBuilder.sucesso("Tenant desativado com sucesso."));

        } catch (NumberFormatException e) {
            return HttpResponse.requisicaoInvalida("ID inválido.");
        } catch (Exception e) {
            return HttpResponse.erroInterno("Erro ao desativar tenant: " + e.getMessage());
        }
    }
}
