package Main;

import Data.JsonBuilder;
import Data.StaticFilesHandler;
import Http.HttpException;
import Http.HttpResponse;
import Http.HttpServer;
import Http.HttpStatus;
import Model.*;
import Persistencia.*;
import Router.Router;
import Security.DashboardPages;
import Security.LoginPages;
import Security.Role;
import Security.User;
import Security.UserStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Main {

	public static void main(String[] args) throws Exception {
		if (!ConexaoDB.isDisponivel()) {
			ConexaoDB.imprimirInstrucoesSetup();
			System.exit(1);
		}

		java.nio.file.Files.createDirectories(java.nio.file.Paths.get("public/Workspace"));

		// Inicializar tabelas de persistência do negócio
		GerenciadorEntidade.inicializarTabela(Projeto.class);
		GerenciadorEntidade.inicializarTabela(ArquivoProjeto.class);
		GerenciadorEntidade.inicializarTabela(NoCanvas.class);
		GerenciadorEntidade.inicializarTabela(LigacaoCanvas.class);

		Router router = new Router();

		router.get("/api/health", req -> HttpResponse.ok().json(
				new JsonBuilder()
						.add("status", ConexaoDB.isDisponivel() ? "ok" : "degraded")
						.add("db", ConexaoDB.isDisponivel())
						.build()));

		// Rota inicial pública
		router.get("/api/info", req -> HttpResponse.ok()
				.html("""
						<!DOCTYPE html>
                    <html lang="pt-BR">
                    <head>
                      <meta charset="UTF-8">
                      <title>Servidor Java HTTP</title>
                      <style>
                        body { font-family: monospace; max-width: 700px; margin: 40px auto; padding: 0 20px; }
                        h1   { color: #c0392b; }
                        a    { color: #2980b9; }
                        code { background: #f0f0f0; padding: 2px 6px; border-radius: 3px; }
                        pre  { background: #1e1e1e; color: #d4d4d4; padding: 16px; border-radius: 6px; }
                        .tag { background: #2980b9; color: white; padding: 2px 8px;
                               border-radius: 3px; font-size: 12px; margin-right: 6px; }
                      </style>
                    </head>
                    <body>
                      <h1>Servidor HTTP em Java puro</h1>
                      <p>Construído do zero — sem frameworks, sem dependências externas.</p>
                      <hr>
                      <h2>Rotas disponíveis</h2>
                      <ul>
                        <li><span class="tag">GET</span>  <a href="/login">/login</a> (Página de login segura)</li>
                        <li><span class="tag">GET</span>  <a href="/api/info-server">/api/info-server</a></li>
                        <li><span class="tag">GET</span>  <a href="/api/echo?mensagem=ola">/api/echo?mensagem=ola</a></li>
                      </ul>
                      <h2>Testando com curl</h2>
                      <pre>
                    curl http://localhost:8080/api/info-server
                    curl "http://localhost:8080/api/echo?mensagem=Java+puro"
                      </pre>
                    </body>
                    </html>
						"""));

		router.get("/", req -> {
			if (req.getUser() != null) {
				if (req.getUser().getCargo() == Role.ADMIN) {
					return HttpResponse.redirect("/workspace/dashboard");
				} else {
					return HttpResponse.redirect("/workspace/home");
				}
			}
			return HttpResponse.ok().html(LoginPages.paginaLogin(""));
		});

		router.post("/login", req -> {
			Map<String, String> formData = req.getFormData();
			String usuario = formData.get("usuario");
			String senha = formData.get("senha");

			if (usuario == null || senha == null || usuario.isBlank() || senha.isBlank()) {
				return HttpResponse.ok().html(LoginPages.paginaLogin("Preencha todos os campos."));
			}

			UserStore store = router.getUserStore();
			Optional<User> optUser = store.buscarPorUsername(usuario);

			if (optUser.isEmpty() || !optUser.get().verificarSenha(senha)) {
				return HttpResponse.ok().html(LoginPages.paginaLogin("Usuário ou senha incorretos."));
			}

			User user = optUser.get();
			if (!user.isAtivo()) {
				return HttpResponse.ok().html(LoginPages.paginaLogin("Usuário inativo. Contate o administrador."));
			}

			// Gerar token
			String token = router.getAuthManager().gerarToken(user);
			int maxAge = 24 * 3600; // 24 horas em segundos

			String redirectUrl = "/workspace/home";
			if (user.getCargo() == Role.ADMIN) {
				redirectUrl = "/workspace/dashboard";
			}

			return HttpResponse.redirect(redirectUrl)
					.cookie("session_token", token, maxAge);
		});

		router.get("/logout", req -> {
			return HttpResponse.redirect("/login").limparCookie("session_token");
		});

		// --- ROTAS DA ÁREA DE TRABALHO SEGURA ---

		router.get("/workspace/dashboard", req -> {
			try {
				User user = req.getUser();
				if (user == null || user.getCargo() == null || user.getCargo() != Role.ADMIN) {
					return HttpResponse.redirect("/login");
				}

				// Listar usuários em JSON para injetar na página
				UserStore store = router.getUserStore();
				List<User> todos = store.listarTodos();

				StringBuilder sb = new StringBuilder("[");
				for (int i = 0; i < todos.size(); i++) {
					if (i > 0) sb.append(",");
					User u = todos.get(i);
					if (u == null) continue;
					if (u.getCargo() == null) {
						// Evita NullPointerException no .name() / .getNome()
						sb.append(new JsonBuilder()
								.add("id", u.getId())
								.add("username", u.getUsername())
								.add("cargo", "VISUALIZADOR")
								.add("cargo_nome", Role.VISUALIZADOR.getNome())
								.add("ativo", u.isAtivo())
								.build());
						continue;
					}

					sb.append(new JsonBuilder()
							.add("id", u.getId())
							.add("username", u.getUsername())
							.add("cargo", u.getCargo().name())
							.add("cargo_nome", u.getCargo().getNome())
							.add("ativo", u.isAtivo())
							.build());
				}
				sb.append("]");

				String usersJson = sb.toString();
				if (usersJson == null || usersJson.isBlank()) usersJson = "[]";

				return HttpResponse.ok().html(DashboardPages.paginaAdmin(user, usersJson));
			} catch (Exception e) {
				System.err.println("[Main] Erro ao renderizar /workspace/dashboard: " + e.getMessage());
				e.printStackTrace();
				return HttpResponse.erroInterno("Erro interno ao processar /workspace/dashboard.");
			}
		});

		router.get("/workspace/home", req -> {
			User user = req.getUser();
			if (user == null) {
				return HttpResponse.redirect("/login");
			}
			return HttpResponse.ok().html(DashboardPages.paginaUserHome(user));
		});

		// --- API ENDPOINTS ---

		router.get("/api/info-server", req -> {
			String json = new JsonBuilder()
					.add("servidor", "Java HTTP Server")
					.add("versao", "1.0.0")
					.add("java", System.getProperty("java.version"))
					.build();
			return HttpResponse.ok().json(json);
		});

		router.get("/api/echo", req -> {
			String mensagem = req.getParametrosQuery("mensagem");
			if (mensagem.isBlank()) {
				throw new HttpException(HttpStatus.BAD_REQUEST,
						"Parametro 'mensagem' é obrigatório. Ex: /api/echo?mensagem=ola");
			}
			String json = new JsonBuilder()
					.add("original", mensagem)
					.add("revertida", new StringBuilder(mensagem).reverse().toString())
					.build();
			return HttpResponse.ok().json(json);
		});

		router.get("/api/me", req -> {
			User user = req.getUser();
			if (user == null) {
				return new HttpResponse().status(HttpStatus.UNAUTHORIZED).json(JsonBuilder.erro("Não autenticado."));
			}
			var cargo = cargoParaJson(user);
			String json = new JsonBuilder()
					.add("id", user.getId())
					.add("username", user.getUsername())
					.add("cargo", cargo.get("cargo"))
					.add("cargo_nome", cargo.get("cargo_nome"))
					.add("ativo", user.isAtivo())
					.build();
			return HttpResponse.ok().json(json);
		});

		// --- ROTAS ADMINISTRATIVAS DE API ---

		router.get("/api/admin/usuarios", req -> {
			User user = req.getUser();
			if (user == null || user.getCargo() != Role.ADMIN) {
				return new HttpResponse().status(HttpStatus.FORBIDDEN).json(JsonBuilder.erro("Não autorizado."));
			}

			UserStore store = router.getUserStore();
			List<User> todos = store.listarTodos();
			StringBuilder sb = new StringBuilder("[");
			for (int i = 0; i < todos.size(); i++) {
				if (i > 0) sb.append(",");
				User u = todos.get(i);
				var cargo = cargoParaJson(u);
				sb.append(new JsonBuilder()
						.add("id", u.getId())
						.add("username", u.getUsername())
						.add("cargo", cargo.get("cargo"))
						.add("cargo_nome", cargo.get("cargo_nome"))
						.add("ativo", u.isAtivo())
						.build());
			}
			sb.append("]");

			return HttpResponse.ok().json(sb.toString());
		});

		router.post("/api/admin/usuarios", req -> {
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
			User novoUser = store.criarUsuario(username, senha, cargo);

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
		});

		router.delete("/api/admin/usuarios", req -> {
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
		});

		router.post("/api/admin/sandbox/exec", Security.SandboxHandler::handle);

		// --- API DE PROJETOS E CANVAS ---

		// Listar projetos do usuário
		router.get("/api/projetos", req -> {
			List<Projeto> projetos = GerenciadorEntidade.buscarTodos(Projeto.class);
			StringBuilder sb = new StringBuilder("[");
			for (int i = 0; i < projetos.size(); i++) {
				if (i > 0) sb.append(",");
				Projeto p = projetos.get(i);
				sb.append(new JsonBuilder()
						.add("id", p.getId())
						.add("nome", p.getNome())
						.add("descricao", p.getDescricao())
						.add("criador_id", p.getCriadorId())
						.add("caminho_raiz", p.getCaminhoRaiz())
						.add("icone", p.getIcone())
						.add("criado_em", p.getCriadoEm())
						.add("atualizado_em", p.getAtualizadoEm())
						.build());
			}
			sb.append("]");
			return HttpResponse.ok().json(sb.toString());
		});

		// Criar novo projeto
		router.post("/api/projetos", req -> {
			String corpo = req.getCorpo();
			String nome = extrairCampoJson(corpo, "nome");
			String descricao = extrairCampoJson(corpo, "descricao");

			if (nome == null || nome.isBlank()) {
				return HttpResponse.requisicaoInvalida("Nome do projeto é obrigatório.");
			}

			Projeto p = new Projeto();
			p.setNome(nome);
			p.setDescricao(descricao != null ? descricao : "");
			p.setCriadorId(req.getUser().getId());

			// Criar diretório em public/Workspace/<nome>
			String pastaNome = nome.replaceAll("[^a-zA-Z0-9_-]", "_");
			String caminhoRaiz = "public/Workspace/" + pastaNome;
			p.setCaminhoRaiz(caminhoRaiz);
			try {
				java.nio.file.Files.createDirectories(java.nio.file.Paths.get(caminhoRaiz));
			} catch (Exception e) {
				System.err.println("[Main] Erro ao criar diretório físico para projeto: " + e.getMessage());
			}

			GerenciadorEntidade.salvar(p);

			// Criar nó inicial de pasta
			NoCanvas noInicial = new NoCanvas();
			noInicial.setProjetoId(p.getId());
			noInicial.setTipo("pasta");
			noInicial.setTitulo(p.getNome());
			noInicial.setPosX(100);
			noInicial.setPosY(100);
			GerenciadorEntidade.salvar(noInicial);

			String jsonResponse = new JsonBuilder()
					.add("status", "success")
					.add("id", p.getId())
					.add("nome", p.getNome())
					.build();
			return HttpResponse.ok().json(jsonResponse);
		});

		// Detalhes do projeto
		router.get("/api/projetos/{id}", req -> {
			try {
				int id = Integer.parseInt(req.getParametroPath("id"));
				Optional<Projeto> opt = GerenciadorEntidade.buscarPorId(Projeto.class, id);
				if (opt.isEmpty()) return HttpResponse.naoEncontrado();
				Projeto p = opt.get();
				String json = new JsonBuilder()
						.add("id", p.getId())
						.add("nome", p.getNome())
						.add("descricao", p.getDescricao())
						.add("caminho_raiz", p.getCaminhoRaiz())
						.add("icone", p.getIcone())
						.build();
				return HttpResponse.ok().json(json);
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
			}
		});

		// Deletar projeto
		router.delete("/api/projetos/{id}", req -> {
			try {
				int id = Integer.parseInt(req.getParametroPath("id"));
				boolean removido = GerenciadorEntidade.remover(Projeto.class, id);
				if (!removido) return HttpResponse.naoEncontrado();
				return HttpResponse.ok().json(JsonBuilder.sucesso("Projeto removido com sucesso."));
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
			}
		});

		// Canvas - Retorna nós + ligações do projeto
		router.get("/api/projetos/{id}/canvas", req -> {
			try {
				int projetoId = Integer.parseInt(req.getParametroPath("id"));
				List<NoCanvas> nos = GerenciadorEntidade.buscarPor(NoCanvas.class, "projetoId", projetoId);
				List<LigacaoCanvas> ligacoes = GerenciadorEntidade.buscarPor(LigacaoCanvas.class, "projetoId", projetoId);

				StringBuilder sbNos = new StringBuilder("[");
				for (int i = 0; i < nos.size(); i++) {
					if (i > 0) sbNos.append(",");
					NoCanvas n = nos.get(i);
					sbNos.append(new JsonBuilder()
							.add("id", n.getId())
							.add("projeto_id", n.getProjetoId())
							.add("arquivo_id", n.getArquivoId())
							.add("tipo", n.getTipo())
							.add("titulo", n.getTitulo())
							.add("pos_x", n.getPosX())
							.add("pos_y", n.getPosY())
							.add("largura", n.getLargura())
							.add("altura", n.getAltura())
							.add("cor", n.getCor())
							.add("dados_extra", n.getDadosExtra())
							.build());
				}
				sbNos.append("]");

				StringBuilder sbLigacoes = new StringBuilder("[");
				for (int i = 0; i < ligacoes.size(); i++) {
					if (i > 0) sbLigacoes.append(",");
					LigacaoCanvas l = ligacoes.get(i);
					sbLigacoes.append(new JsonBuilder()
							.add("id", l.getId())
							.add("projeto_id", l.getProjetoId())
							.add("origem_id", l.getOrigemId())
							.add("destino_id", l.getDestinoId())
							.add("tipo", l.getTipo())
							.add("cor", l.getCor())
							.add("label", l.getLabel())
							.add("porta_origem", l.getPortaOrigem())
							.add("porta_destino", l.getPortaDestino())
							.build());
				}
				sbLigacoes.append("]");

				String res = "{\"nos\":" + sbNos.toString() + ",\"ligacoes\":" + sbLigacoes.toString() + "}";
				return HttpResponse.ok().json(res);
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
			}
		});

		// Adicionar Nó no canvas
		router.post("/api/projetos/{id}/nos", req -> {
			try {
				int projetoId = Integer.parseInt(req.getParametroPath("id"));
				String corpo = req.getCorpo();
				String tipo = extrairCampoJson(corpo, "tipo");
				String titulo = extrairCampoJson(corpo, "titulo");
				String posXStr = extrairCampoJson(corpo, "pos_x");
				String posYStr = extrairCampoJson(corpo, "pos_y");
				String arquivoIdStr = extrairCampoJson(corpo, "arquivo_id");

				NoCanvas n = new NoCanvas();
				n.setProjetoId(projetoId);
				n.setTipo(tipo != null ? tipo : "nota");
				n.setTitulo(titulo != null ? titulo : "Novo Bloco");
				if (posXStr != null) n.setPosX(Double.parseDouble(posXStr));
				if (posYStr != null) n.setPosY(Double.parseDouble(posYStr));
				if (arquivoIdStr != null && !arquivoIdStr.equals("null") && !arquivoIdStr.isEmpty()) {
					n.setArquivoId(Integer.parseInt(arquivoIdStr));
				}

				GerenciadorEntidade.salvar(n);

				String res = new JsonBuilder()
						.add("status", "success")
						.add("id", n.getId())
						.add("tipo", n.getTipo())
						.add("titulo", n.getTitulo())
						.build();
				return HttpResponse.ok().json(res);
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID inválido.");
			}
		});

		// Atualizar Nó no canvas
		router.put("/api/projetos/{id}/nos/{nid}", req -> {
			try {
				int nid = Integer.parseInt(req.getParametroPath("nid"));
				Optional<NoCanvas> opt = GerenciadorEntidade.buscarPorId(NoCanvas.class, nid);
				if (opt.isEmpty()) return HttpResponse.naoEncontrado();

				NoCanvas n = opt.get();
				String corpo = req.getCorpo();
				String posXStr = extrairCampoJson(corpo, "pos_x");
				String posYStr = extrairCampoJson(corpo, "pos_y");
				String larguraStr = extrairCampoJson(corpo, "largura");
				String alturaStr = extrairCampoJson(corpo, "altura");
				String titulo = extrairCampoJson(corpo, "titulo");
				String cor = extrairCampoJson(corpo, "cor");
				String dadosExtra = extrairCampoJson(corpo, "dados_extra");

				if (posXStr != null) n.setPosX(Double.parseDouble(posXStr));
				if (posYStr != null) n.setPosY(Double.parseDouble(posYStr));
				if (larguraStr != null) n.setLargura(Double.parseDouble(larguraStr));
				if (alturaStr != null) n.setAltura(Double.parseDouble(alturaStr));
				if (titulo != null) n.setTitulo(titulo);
				if (cor != null) n.setCor(cor);
				if (dadosExtra != null) n.setDadosExtra(dadosExtra);

				GerenciadorEntidade.salvar(n);
				return HttpResponse.ok().json(JsonBuilder.sucesso("Nó atualizado."));
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID inválido.");
			}
		});

		// Remover Nó no canvas
		router.delete("/api/projetos/{id}/nos/{nid}", req -> {
			try {
				int nid = Integer.parseInt(req.getParametroPath("nid"));
				GerenciadorEntidade.remover(NoCanvas.class, nid);
				return HttpResponse.ok().json(JsonBuilder.sucesso("Nó removido."));
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID inválido.");
			}
		});

		// Criar Ligação no canvas
		router.post("/api/projetos/{id}/ligacoes", req -> {
			try {
				int projetoId = Integer.parseInt(req.getParametroPath("id"));
				String corpo = req.getCorpo();
				String origemIdStr = extrairCampoJson(corpo, "origem_id");
				String destinoIdStr = extrairCampoJson(corpo, "destino_id");
				String tipo = extrairCampoJson(corpo, "tipo");
				String portaOrigem = extrairCampoJson(corpo, "porta_origem");
				String portaDestino = extrairCampoJson(corpo, "porta_destino");

				if (origemIdStr == null || destinoIdStr == null) {
					return HttpResponse.requisicaoInvalida("Nós de origem e destino são obrigatórios.");
				}

				LigacaoCanvas l = new LigacaoCanvas();
				l.setProjetoId(projetoId);
				l.setOrigemId(Integer.parseInt(origemIdStr));
				l.setDestinoId(Integer.parseInt(destinoIdStr));
				if (tipo != null) l.setTipo(tipo);
				if (portaOrigem != null) l.setPortaOrigem(portaOrigem);
				if (portaDestino != null) l.setPortaDestino(portaDestino);

				GerenciadorEntidade.salvar(l);

				String res = new JsonBuilder()
						.add("status", "success")
						.add("id", l.getId())
						.build();
				return HttpResponse.ok().json(res);
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID inválido.");
			}
		});

		// Remover Ligação no canvas
		router.delete("/api/projetos/{id}/ligacoes/{lid}", req -> {
			try {
				int lid = Integer.parseInt(req.getParametroPath("lid"));
				GerenciadorEntidade.remover(LigacaoCanvas.class, lid);
				return HttpResponse.ok().json(JsonBuilder.sucesso("Ligação removida."));
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID inválido.");
			}
		});

		// Upload de arquivo (suporta pasta_id para upload direto em pasta)
		router.post("/api/projetos/{id}/upload", req -> {
			try {
				int projetoId = Integer.parseInt(req.getParametroPath("id"));
				Optional<Projeto> opt = GerenciadorEntidade.buscarPorId(Projeto.class, projetoId);
				if (opt.isEmpty()) return HttpResponse.naoEncontrado();

				Projeto p = opt.get();
				String corpo = req.getCorpo();
				String nome = extrairCampoJson(corpo, "name");
				String contentBase64 = extrairCampoJson(corpo, "content");
				String tipoArquivo = extrairCampoJson(corpo, "type");
				String pastaIdStr = extrairCampoJson(corpo, "pasta_id");
				if (tipoArquivo == null || tipoArquivo.isBlank() || tipoArquivo.equals("null")) {
					tipoArquivo = Files.FilesInfo.obterTipoArquivo(nome);
				}

				if (nome == null || contentBase64 == null) {
					return HttpResponse.requisicaoInvalida("Nome do arquivo e conteúdo são obrigatórios.");
				}

				byte[] bytes = java.util.Base64.getDecoder().decode(contentBase64);
				java.nio.file.Path destino = java.nio.file.Paths.get(p.getCaminhoRaiz(), nome);
				java.nio.file.Files.write(destino, bytes);

				// Salvar metadados no banco
				ArquivoProjeto aq = new ArquivoProjeto();
				aq.setProjetoId(p.getId());
				aq.setNome(nome);
				aq.setCaminho("/workspace/" + p.getNome().replaceAll("[^a-zA-Z0-9_-]", "_") + "/" + nome);
				aq.setTamanhoBytes(bytes.length);
				aq.setTipoArquivo(tipoArquivo != null ? tipoArquivo : "documento");
				aq.setTipoMime(Data.MimeTypes.porCaminho(nome));

				// Atribuir à pasta, se especificada
				if (pastaIdStr != null && !pastaIdStr.isBlank() && !pastaIdStr.equals("null")) {
					aq.setPastaPaiId(Integer.parseInt(pastaIdStr));
				}

				GerenciadorEntidade.salvar(aq);

				// Criar nó automático no canvas para o arquivo
				NoCanvas n = new NoCanvas();
				n.setProjetoId(p.getId());
				n.setArquivoId(aq.getId());
				n.setTipo("arquivo");
				n.setTitulo(nome);
				n.setPosX(300);
				n.setPosY(200);
				n.setDadosExtra(new JsonBuilder()
						.add("caminho", aq.getCaminho())
						.add("tipo_arquivo", aq.getTipoArquivo())
						.add("tamanho", aq.getTamanhoBytes())
						.build());

				GerenciadorEntidade.salvar(n);

				String jsonRes = new JsonBuilder()
						.add("status", "success")
						.add("id", aq.getId())
						.add("no_id", n.getId())
						.add("nome", aq.getNome())
						.add("caminho", aq.getCaminho())
						.build();
				return HttpResponse.ok().json(jsonRes);
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
			} catch (Exception e) {
				System.err.println("Erro ao fazer upload de arquivo: " + e.getMessage());
				return HttpResponse.erroInterno("Erro ao processar upload do arquivo.");
			}
		});

		// Listar arquivos do projeto (suporta ?pasta_id=X para filtrar por pasta)
		router.get("/api/projetos/{id}/arquivos", req -> {
			try {
				int projetoId = Integer.parseInt(req.getParametroPath("id"));
				List<ArquivoProjeto> arquivos = GerenciadorEntidade.buscarPor(ArquivoProjeto.class, "projetoId", projetoId);

				// Filtrar por pasta_id se especificado
				String pastaIdParam = req.getParametrosQuery("pasta_id");
				if (pastaIdParam != null && !pastaIdParam.isBlank()) {
					int pastaId = Integer.parseInt(pastaIdParam);
					arquivos = arquivos.stream()
							.filter(a -> a.getPastaPaiId() != null && a.getPastaPaiId() == pastaId)
							.collect(java.util.stream.Collectors.toList());
				}

				StringBuilder sb = new StringBuilder("[");
				for (int i = 0; i < arquivos.size(); i++) {
					if (i > 0) sb.append(",");
					ArquivoProjeto aq = arquivos.get(i);
					sb.append(new JsonBuilder()
							.add("id", aq.getId())
							.add("projeto_id", aq.getProjetoId())
							.add("nome", aq.getNome())
							.add("caminho", aq.getCaminho())
							.add("tipo_mime", aq.getTipoMime())
							.add("tamanho_bytes", aq.getTamanhoBytes())
							.add("tipo_arquivo", aq.getTipoArquivo())
							.add("eh_pasta", aq.isEhPasta())
							.add("pasta_pai_id", aq.getPastaPaiId())
							.add("criado_em", aq.getCriadoEm())
							.build());
				}
				sb.append("]");
				return HttpResponse.ok().json(sb.toString());
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
			}
		});

		// Criar pasta dentro do projeto
		router.post("/api/projetos/{id}/pastas", req -> {
			try {
				int projetoId = Integer.parseInt(req.getParametroPath("id"));
				String corpo = req.getCorpo();
				String nome = extrairCampoJson(corpo, "nome");
				String pastaPaiIdStr = extrairCampoJson(corpo, "pasta_pai_id");

				if (nome == null || nome.isBlank()) {
					return HttpResponse.requisicaoInvalida("Nome da pasta é obrigatório.");
				}

				ArquivoProjeto pasta = new ArquivoProjeto();
				pasta.setProjetoId(projetoId);
				pasta.setNome(nome);
				pasta.setEhPasta(true);
				pasta.setCaminho("");
				pasta.setTipoArquivo("pasta");
				pasta.setTipoMime("");

				if (pastaPaiIdStr != null && !pastaPaiIdStr.isBlank() && !pastaPaiIdStr.equals("null")) {
					pasta.setPastaPaiId(Integer.parseInt(pastaPaiIdStr));
				}

				GerenciadorEntidade.salvar(pasta);

				String json = new JsonBuilder()
						.add("status", "success")
						.add("id", pasta.getId())
						.add("nome", pasta.getNome())
						.add("eh_pasta", true)
						.build();
				return HttpResponse.ok().json(json);
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID inválido.");
			}
		});

		// Mover arquivo/pasta para outra pasta
		router.put("/api/projetos/{id}/arquivos/{aid}/mover", req -> {
			try {
				int aid = Integer.parseInt(req.getParametroPath("aid"));
				Optional<ArquivoProjeto> opt = GerenciadorEntidade.buscarPorId(ArquivoProjeto.class, aid);
				if (opt.isEmpty()) return HttpResponse.naoEncontrado();

				ArquivoProjeto aq = opt.get();
				String corpo = req.getCorpo();
				String novaPastaIdStr = extrairCampoJson(corpo, "nova_pasta_id");

				if (novaPastaIdStr == null || novaPastaIdStr.isBlank() || novaPastaIdStr.equals("null")) {
					aq.setPastaPaiId(null); // Mover para raiz
				} else {
					aq.setPastaPaiId(Integer.parseInt(novaPastaIdStr));
				}

				GerenciadorEntidade.salvar(aq);
				return HttpResponse.ok().json(JsonBuilder.sucesso("Arquivo movido com sucesso."));
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID inválido.");
			}
		});

		// Árvore hierárquica de arquivos/pastas do projeto
		router.get("/api/projetos/{id}/arvore", req -> {
			try {
				int projetoId = Integer.parseInt(req.getParametroPath("id"));
				List<ArquivoProjeto> todos = GerenciadorEntidade.buscarPor(ArquivoProjeto.class, "projetoId", projetoId);

				// Construir árvore: separar raiz e filhos
				StringBuilder raiz = new StringBuilder("[");
				boolean primeiro = true;
				for (ArquivoProjeto aq : todos) {
					if (aq.getPastaPaiId() == null) {
						if (!primeiro) raiz.append(",");
						primeiro = false;
						raiz.append(construirNoArvore(aq, todos));
					}
				}
				raiz.append("]");

				return HttpResponse.ok().json("{\"raiz\":" + raiz.toString() + "}");
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
			}
		});

		// Deletar pasta (move conteúdo para raiz)
		router.delete("/api/projetos/{id}/pastas/{pid}", req -> {
			try {
				int pid = Integer.parseInt(req.getParametroPath("pid"));
				Optional<ArquivoProjeto> opt = GerenciadorEntidade.buscarPorId(ArquivoProjeto.class, pid);
				if (opt.isEmpty()) return HttpResponse.naoEncontrado();

				ArquivoProjeto pasta = opt.get();
				if (!pasta.isEhPasta()) {
					return HttpResponse.requisicaoInvalida("O item especificado não é uma pasta.");
				}

				// Mover todos os filhos da pasta para a raiz (pasta_pai_id = null)
				int projetoId = pasta.getProjetoId();
				List<ArquivoProjeto> todos = GerenciadorEntidade.buscarPor(ArquivoProjeto.class, "projetoId", projetoId);
				for (ArquivoProjeto filho : todos) {
					if (filho.getPastaPaiId() != null && filho.getPastaPaiId() == pid) {
						filho.setPastaPaiId(null);
						GerenciadorEntidade.salvar(filho);
					}
				}

				GerenciadorEntidade.remover(ArquivoProjeto.class, pid);
				return HttpResponse.ok().json(JsonBuilder.sucesso("Pasta removida. Conteúdo movido para a raiz."));
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID inválido.");
			}
		});

		// Renomear pasta
		router.put("/api/projetos/{id}/pastas/{pid}/renomear", req -> {
			try {
				int pid = Integer.parseInt(req.getParametroPath("pid"));
				Optional<ArquivoProjeto> opt = GerenciadorEntidade.buscarPorId(ArquivoProjeto.class, pid);
				if (opt.isEmpty()) return HttpResponse.naoEncontrado();

				ArquivoProjeto pasta = opt.get();
				String corpo = req.getCorpo();
				String novoNome = extrairCampoJson(corpo, "nome");

				if (novoNome == null || novoNome.isBlank()) {
					return HttpResponse.requisicaoInvalida("Nome é obrigatório.");
				}

				pasta.setNome(novoNome);
				GerenciadorEntidade.salvar(pasta);
				return HttpResponse.ok().json(JsonBuilder.sucesso("Pasta renomeada com sucesso."));
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID inválido.");
			}
		});

		// Página do Canvas Workspace do projeto
		router.get("/workspace/projeto/{id}", req -> {
			try {
				int id = Integer.parseInt(req.getParametroPath("id"));
				Optional<Projeto> opt = GerenciadorEntidade.buscarPorId(Projeto.class, id);
				if (opt.isEmpty()) return HttpResponse.naoEncontrado();
				return HttpResponse.ok().html(Security.SandboxPages.paginaCanvas(req.getUser(), opt.get()));
			} catch (NumberFormatException e) {
				return HttpResponse.requisicaoInvalida("ID do projeto inválido.");
			}
		});

		StaticFilesHandler workspaceDirectory = new StaticFilesHandler("public/Workspace", "/workspace/");
		router.get("/workspace/*", workspaceDirectory);
		
		StaticFilesHandler cssDirectory = new StaticFilesHandler("public/css", "/css/");
		router.get("/css/*", cssDirectory);
		
		StaticFilesHandler assetsDirectory = new StaticFilesHandler("public/assets", "/assets/");
		router.get("/assets/*", assetsDirectory);
		
		StaticFilesHandler jsDirectory = new StaticFilesHandler("public/js", "/js/");
		router.get("/js/*", jsDirectory);

		HttpServer servidor = new HttpServer(8081, router);
		servidor.iniciar();
	}

	private static Map<String, String> cargoParaJson(User user) {
		Role cargo = user.getCargo() != null ? user.getCargo() : Role.VISUALIZADOR;
		return Map.of("cargo", cargo.name(), "cargo_nome", cargo.getNome());
	}

	/**
	 * Constrói recursivamente um nó da árvore de arquivos em JSON.
	 */
	private static String construirNoArvore(ArquivoProjeto item, List<ArquivoProjeto> todos) {
		JsonBuilder jb = new JsonBuilder()
				.add("id", item.getId())
				.add("nome", item.getNome())
				.add("eh_pasta", item.isEhPasta())
				.add("tipo_arquivo", item.getTipoArquivo() != null ? item.getTipoArquivo() : "")
				.add("caminho", item.getCaminho() != null ? item.getCaminho() : "")
				.add("tamanho_bytes", item.getTamanhoBytes())
				.add("tipo_mime", item.getTipoMime() != null ? item.getTipoMime() : "")
				.add("criado_em", item.getCriadoEm());

		if (item.getPastaPaiId() != null) {
			jb.add("pasta_pai_id", item.getPastaPaiId());
		}

		if (item.isEhPasta()) {
			// Buscar filhos desta pasta
			StringBuilder filhos = new StringBuilder("[");
			boolean primeiro = true;
			for (ArquivoProjeto filho : todos) {
				if (filho.getPastaPaiId() != null && filho.getPastaPaiId() == item.getId()) {
					if (!primeiro) filhos.append(",");
					primeiro = false;
					filhos.append(construirNoArvore(filho, todos));
				}
			}
			filhos.append("]");

			// Injetar array de filhos manualmente no JSON
			String base = jb.build();
			// Remover o último } e adicionar o campo filhos
			return base.substring(0, base.length() - 1) + ",\"filhos\":" + filhos.toString() + "}";
		}

		return jb.build();
	}

	private static String extrairCampoJson(String json, String campo) {
		try {
			java.util.Map<String, Object> map = Data.JsonParser.parse(json);
			return Data.JsonParser.getString(map, campo);
		} catch (Exception e) {
			return null;
		}
	}
}
