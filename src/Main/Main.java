package Main;

import Controller.*;
import Data.JsonBuilder;
import Data.StaticFilesHandler;
import Http.HttpResponse;
import Http.HttpServer;
import Model.*;
import Persistencia.*;
import Router.Router;

import java.util.List;

public class Main {

	public static void main(String[] args) throws Exception {
		if (!ConexaoDB.isDisponivel()) {
			ConexaoDB.imprimirInstrucoesSetup();
			System.exit(1);
		}

		java.nio.file.Files.createDirectories(java.nio.file.Paths.get("public/Workspace"));

		// Inicializar tabelas de persistência do negócio
		GerenciadorEntidade.inicializarTabela(Tenant.class);
		GerenciadorEntidade.inicializarTabela(Projeto.class);
		GerenciadorEntidade.inicializarTabela(ArquivoProjeto.class);
		GerenciadorEntidade.inicializarTabela(NoCanvas.class);
		GerenciadorEntidade.inicializarTabela(LigacaoCanvas.class);
		GerenciadorEntidade.inicializarTabela(LogAtividade.class);

		Router router = new Router();

		// Controladores
		AuthController authController = new AuthController(router);
		ProjectController projectController = new ProjectController();
		CanvasController canvasController = new CanvasController();
		AdminController adminController = new AdminController(router);

		// Rotas de Monitoramento e Info
		router.get("/api/health", req -> HttpResponse.ok().json(
				new JsonBuilder()
						.add("status", ConexaoDB.isDisponivel() ? "ok" : "degraded")
						.add("db", ConexaoDB.isDisponivel())
						.build()));

		router.get("/api/info-server", req -> {
			String json = new JsonBuilder()
					.add("servidor", "Java HTTP Server")
					.add("versao", "1.0.0")
					.add("java", System.getProperty("java.version"))
					.build();
			return HttpResponse.ok().json(json);
		});

		// Rotas de Autenticação
		router.post("/api/login", authController::login);
		router.get("/api/me", authController::me);
		router.get("/api/logout", authController::logout);
		router.post("/api/logout", authController::logout);
		router.post("/api/troca-senha", authController::trocaSenha);

		// Rotas de Projetos e Arquivos
		router.get("/api/projetos", projectController::listarProjetos);
		router.post("/api/projetos", projectController::criarProjeto);
		router.get("/api/projetos/{id}", projectController::obterProjeto);
		router.delete("/api/projetos/{id}", projectController::deletarProjeto);
		router.get("/api/projetos/{id}/arquivos", projectController::listarArquivos);
		router.post("/api/projetos/{id}/pastas", projectController::criarPasta);
		router.put("/api/projetos/{id}/arquivos/{aid}/mover", projectController::moverArquivo);
		router.delete("/api/projetos/{id}/arquivos/{aid}", projectController::deletarArquivo);
		router.put("/api/projetos/{id}/arquivos/{aid}/renomear", projectController::renomearArquivo);
		router.get("/api/projetos/{id}/arvore", projectController::obterArvore);
		router.delete("/api/projetos/{id}/pastas/{pid}", projectController::deletarPasta);
		router.put("/api/projetos/{id}/pastas/{pid}/renomear", projectController::renomearPasta);
		router.post("/api/projetos/{id}/upload", projectController::uploadArquivo);
		router.get("/api/projetos/{id}/arquivos/{aid}/conteudo", projectController::obterConteudoArquivo);
		router.put("/api/projetos/{id}/arquivos/{aid}/conteudo", projectController::salvarConteudoArquivo);

		// Rotas do Canvas
		router.get("/api/projetos/{id}/canvas", canvasController::obterCanvas);
		router.post("/api/projetos/{id}/nos", canvasController::adicionarNo);
		router.put("/api/projetos/{id}/nos/{nid}", canvasController::atualizarNo);
		router.delete("/api/projetos/{id}/nos/{nid}", canvasController::removerNo);
		router.post("/api/projetos/{id}/ligacoes", canvasController::adicionarLigacao);
		router.delete("/api/projetos/{id}/ligacoes/{lid}", canvasController::removerLigacao);

		// Rotas Administrativas
		router.get("/api/admin/usuarios", adminController::listarUsuarios);
		router.post("/api/admin/usuarios", adminController::criarUsuario);
		router.delete("/api/admin/usuarios", adminController::deletarUsuario);
		router.put("/api/admin/usuarios/{uid}/permissoes", adminController::atualizarPermissoes);
		router.post("/api/admin/sandbox/exec", Security.SandboxHandler::handle);

		// Rota de Log de Atividade por Projeto
		router.get("/api/projetos/{id}/atividade", req -> {
			int projId = Integer.parseInt(req.getParametroPath("id"));
			List<LogAtividade> logs = GerenciadorEntidade.buscarPor(LogAtividade.class, "projetoId", projId);
			StringBuilder sb = new StringBuilder("[");
			for (int i = 0; i < logs.size(); i++) {
				if (i > 0) sb.append(",");
				LogAtividade log = logs.get(i);
				sb.append(new JsonBuilder()
						.add("id", log.getId())
						.add("acao", log.getAcao())
						.add("username", log.getUsername() != null ? log.getUsername() : "")
						.add("timestamp", log.getTimestamp())
						.add("detalhes", log.getDetalhes() != null ? log.getDetalhes() : "")
						.build());
			}
			sb.append("]");
			return HttpResponse.ok().json(sb.toString());
		});

		// Rotas de Tenants (ADMIN)
		TenantController tenantController = new TenantController();
		router.get("/api/admin/tenants", tenantController::listarTenants);
		router.post("/api/admin/tenants", tenantController::criarTenant);
		router.put("/api/admin/tenants/{id}", tenantController::atualizarTenant);
		router.delete("/api/admin/tenants/{id}", tenantController::desativarTenant);

		// Servidor de Arquivos Estáticos (Fallback para SPA do Angular)
		StaticFilesHandler staticFiles = new StaticFilesHandler("public");
		router.get("/*", staticFiles);

		HttpServer servidor = new HttpServer(8081, router);
		servidor.iniciar();
	}
}
