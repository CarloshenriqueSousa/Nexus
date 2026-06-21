package Router;

import Data.JsonBuilder;
import Handler.Handler;
import Http.HttpException;
import Http.HttpMethod;
import Http.HttpRequest;
import Http.HttpResponse;
import Http.HttpStatus;
import Persistencia.PersistenciaException;
import Security.AuthManager;
import Security.User;
import Security.UserStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Router {

	private static class Rota {
		final HttpMethod metodo;
		final String padrao;
		final Handler handler;

		Rota(HttpMethod metodo, String padrao, Handler handler) {
			this.metodo = metodo;
			this.padrao = padrao;
			this.handler = handler;
		}

		boolean combina(HttpMethod m, String caminho, HttpRequest req) {
			if (this.metodo != m) return false;
			Map<String, String> params = new HashMap<>();
			if (padraoCorresponde(padrao, caminho, params)) {
				for (Map.Entry<String, String> entry : params.entrySet()) {
					req.setParametroPath(entry.getKey(), entry.getValue());
				}
				return true;
			}
			return false;
		}
	}

	private final List<Rota> rotas = new ArrayList<>();
	private Handler handlerNaoEncontrado = req -> HttpResponse.naoEncontrado();

	private final AuthManager authManager;
	private final UserStore userStore;

	private final Set<String> rotasPublicas = new HashSet<>();
	private final List<String> prefixosPublicos = new ArrayList<>();

	public Router() {
		// Carregar configurações de segurança
		Properties props = new Properties();
		Path path = Paths.get("src/config/security.properties");
		if (!Files.exists(path)) {
			path = Paths.get("config/security.properties");
		}
		if (Files.exists(path)) {
			try (InputStream in = Files.newInputStream(path)) {
				props.load(in);
			} catch (IOException e) {
				System.err.println("[Router] Erro ao carregar security.properties: " + e.getMessage());
			}
		} else {
			System.err.println("[Router] Aviso: security.properties não encontrado. Usando padrões.");
		}

		int expHoras = Integer.parseInt(props.getProperty("jwt.expiracao.horas", "24"));
		String secretFile = props.getProperty("jwt.secret.file", "config/jwt_secret.key");
		String usersFile = props.getProperty("users.file", "config/users.dat");

		this.authManager = new AuthManager(secretFile, expHoras);
		this.userStore = new UserStore(usersFile);

		// Carrega rotas públicas
		String publicRoutesProp = props.getProperty("public.routes", "/,/login,/favicon.ico,/api/health,/api/info");
		for (String r : publicRoutesProp.split(",")) {
			if (!r.isBlank()) rotasPublicas.add(r.trim());
		}

		String publicPrefixesProp = props.getProperty("public.prefixes", "/public/");
		for (String p : publicPrefixesProp.split(",")) {
			if (!p.isBlank()) prefixosPublicos.add(p.trim());
		}
	}

	public AuthManager getAuthManager() {
		return authManager;
	}

	public UserStore getUserStore() {
		return userStore;
	}

	public Router get(String caminho, Handler handler) {
		rotas.add(new Rota(HttpMethod.GET, caminho, handler));
		return this;
	}

	public Router post(String caminho, Handler handler) {
		rotas.add(new Rota(HttpMethod.POST, caminho, handler));
		return this;
	}

	public Router put(String caminho, Handler handler) {
		rotas.add(new Rota(HttpMethod.PUT, caminho, handler));
		return this;
	}

	public Router delete(String caminho, Handler handler) {
		rotas.add(new Rota(HttpMethod.DELETE, caminho, handler));
		return this;
	}

	public Router naoEncontrado(Handler handler) {
		this.handlerNaoEncontrado = handler;
		return this;
	}

	public Router fallback(Handler handler) {
		this.handlerNaoEncontrado = handler;
		return this;
	}
	public HttpResponse despachar(HttpRequest requisicao) {
		String caminho = requisicao.getCaminho();

		// 1. Tenta extrair usuário do token JWT no cookie
		String token = requisicao.getCookie("session_token");
		if (token != null && !token.isBlank()) {
			AuthManager.TokenInfo info = authManager.validarToken(token);
			if (info != null) {
				userStore.buscarPorId(info.getId()).ifPresent(requisicao::setUser);
			}
		}

		boolean publica = rotasPublicas.contains(caminho) || prefixosPublicos.stream().anyMatch(caminho::startsWith);

		// 2. FILTRO DE AUTENTICAÇÃO: Intercepta e protege rotas privadas
		if (!publica) {
			if (requisicao.getUser() == null) {
				System.err.println("[ACESSO BLOQUEADO] Tentativa de acesso não autenticada na rota: " + caminho);
				if (caminho.startsWith("/api/")) {
					return new HttpResponse()
							.status(HttpStatus.UNAUTHORIZED)
							.json(JsonBuilder.erro("Acesso não autorizado. Faça login."));
				} else {
					return HttpResponse.redirect("/login");
				}
			}

			// 3. FILTRO DE AUTORIZAÇÃO POR CARGO (Exceto rotas de auto-serviço)
			boolean rotaAutoServico = caminho.equals("/api/me") || caminho.equals("/logout") || caminho.equals("/workspace/home");
			if (!rotaAutoServico) {
				User user = requisicao.getUser();
				boolean permitido = false;
				switch (requisicao.getMetodo()) {
					case GET -> permitido = user.podeVer(caminho);
					case POST -> permitido = user.podeCriar(caminho);
					case PUT -> permitido = user.podeEditar(caminho);
					case DELETE -> permitido = user.podeDeletar(caminho);
					default -> permitido = false;
				}

				if (!permitido) {
					System.err.println("[ACESSO NEGADO] Usuário " + user.getUsername() + " sem permissão para " + requisicao.getMetodo() + " " + caminho);
					return new HttpResponse()
							.status(HttpStatus.FORBIDDEN)
							.json(JsonBuilder.erro("Acesso negado. Seu cargo (" + user.getCargo().getNome() + ") não tem permissão para esta ação."));
				}
			}
		}

		// 4. ROTEAMENTO PADRÃO
		for (Rota rota : rotas) {
			if (rota.combina(requisicao.getMetodo(), requisicao.getCaminho(), requisicao)) {
				try {
					return rota.handler.handle(requisicao);
				} catch (HttpException e) {
					return new HttpResponse().status(e.getStatus()).json(JsonBuilder.erro(e.getMessage()));
				} catch (PersistenciaException e) {
					return HttpResponse.servicoIndisponivel(e.getMessage());
				} catch (Exception e) {
					System.err.println("Erro no handler: " + e.getMessage());
					e.printStackTrace();
					return HttpResponse.erroInterno("Erro interno ao processar requisição.");
				}
			}
		}
		return handlerNaoEncontrado.handle(requisicao);
	}

	private static boolean padraoCorresponde(String padrao, String caminho, Map<String, String> paramsOut) {
		if (padrao.equals(caminho))
			return true;
		if (padrao.endsWith("/*")) {
			String prefixo = padrao.substring(0, padrao.length() - 1);
			return caminho.startsWith(prefixo);
		}

		String[] partesPadrao = padrao.split("/");
		String[] partesCaminho = caminho.split("/");

		boolean endsWithWildcard = padrao.endsWith("*");
		if (!endsWithWildcard && partesPadrao.length != partesCaminho.length) {
			return false;
		}

		int limit = Math.min(partesPadrao.length, partesCaminho.length);
		for (int i = 0; i < limit; i++) {
			String pPadrao = partesPadrao[i];
			if (pPadrao.equals("*")) {
				return true;
			}
			if (i >= partesCaminho.length) {
				return false;
			}
			String pCaminho = partesCaminho[i];

			if (pPadrao.startsWith("{") && pPadrao.endsWith("}")) {
				String paramName = pPadrao.substring(1, pPadrao.length() - 1);
				if (paramsOut != null) {
					paramsOut.put(paramName, pCaminho);
				}
			} else if (!pPadrao.equals(pCaminho)) {
				return false;
			}
		}

		return partesPadrao.length == partesCaminho.length;
	}
}