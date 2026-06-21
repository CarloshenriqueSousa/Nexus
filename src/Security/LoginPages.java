package Security;

public final class LoginPages {

	private LoginPages() {}

	public static String paginaLogin(String mensagemErro) {
		String alerta = mensagemErro == null || mensagemErro.isBlank()
				? ""
				: "<p class=\"erro\">" + escapar(mensagemErro) + "</p>";

		return """
				<!DOCTYPE html>
				<html lang="pt-BR">
				<head>
				  <meta charset="UTF-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1.0">
				  <title>Login — Servidor HTTP</title>
				  <style>
				    * { box-sizing: border-box; }
				    body {
				      margin: 0; min-height: 100vh; display: grid; place-items: center;
				      font-family: 'Segoe UI', system-ui, sans-serif;
				      background: linear-gradient(135deg, #0f172a 0%%, #1e293b 50%%, #0f172a 100%%);
				      color: #e2e8f0;
				    }
				    .card {
				      width: min(420px, 92vw); padding: 32px; border-radius: 16px;
				      background: rgba(15, 23, 42, 0.85); border: 1px solid #334155;
				      box-shadow: 0 20px 60px rgba(0,0,0,0.45);
				    }
				    h1 { margin: 0 0 8px; font-size: 1.6rem; color: #f8fafc; }
				    p.sub { margin: 0 0 24px; color: #94a3b8; font-size: 0.95rem; }
				    label { display: block; margin-bottom: 6px; font-size: 0.85rem; color: #cbd5e1; }
				    input {
				      width: 100%%; padding: 12px 14px; margin-bottom: 16px; border-radius: 8px;
				      border: 1px solid #475569; background: #0f172a; color: #f1f5f9;
				    }
				    input:focus { outline: 2px solid #3b82f6; border-color: #3b82f6; }
				    button {
				      width: 100%%; padding: 12px; border: none; border-radius: 8px;
				      background: #2563eb; color: white; font-weight: 600; cursor: pointer;
				    }
				    button:hover { background: #1d4ed8; }
				    .erro {
				      background: #7f1d1d; color: #fecaca; padding: 10px 12px;
				      border-radius: 8px; margin-bottom: 16px; font-size: 0.9rem;
				    }
				    .hint { margin-top: 16px; font-size: 0.8rem; color: #64748b; text-align: center; }
				  </style>
				</head>
				<body>
				  <div class="card">
				    <h1>Entrar no Workspace</h1>
				    <p class="sub">Autentique-se para acessar o painel do servidor.</p>
				    %s
				    <form method="POST" action="/login">
				      <label for="usuario">Usuário</label>
				      <input id="usuario" name="usuario" type="text" autocomplete="username" required>
				      <label for="senha">Senha</label>
				      <input id="senha" name="senha" type="password" autocomplete="current-password" required>
				      <button type="submit">Acessar</button>
				    </form>
				    <p class="hint">Sessão segura via cookie HttpOnly</p>
				  </div>
				</body>
				</html>
				""".formatted(alerta);
	}

	private static String escapar(String texto) {
		return texto.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}
}
