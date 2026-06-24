package Security;

public final class LoginPages {

	private LoginPages() {}

	public static String paginaLogin(String mensagemErro) {
		String alerta = mensagemErro == null || mensagemErro.isBlank()
				? ""
				: "<div class=\"login-error\">" + escapar(mensagemErro) + "</div>";

		return """
				<!DOCTYPE html>
				<html lang="pt-BR">
				<head>
				  <meta charset="UTF-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1.0">
				  <title>Vaultra \u2014 Autentica\u00e7\u00e3o</title>
				  <link rel="stylesheet" href="/css/vaultra.css">
				</head>
				<body class="login-page">
				  <div class="login-hero">
				    <div class="login-hero-content">
				      <div class="login-logo">
				        <!-- TODO: Insert real SVG logo here -->
				        <span class="login-logo-text">V</span>
				      </div>
				      <h1>Vaultra Data Platform</h1>
				      <p class="login-hero-subtitle">Acesso seguro à infraestrutura de dados para engenharia, arquitetura e indústria.</p>
				      <div class="login-features">
				        <div class="login-feature">
				          <div class="login-feature-icon">⚡</div>
				          <span>Performance otimizada em Java puro</span>
				        </div>
				        <div class="login-feature">
				          <div class="login-feature-icon">🔒</div>
				          <span>Segurança premium com tokens JWT isolados</span>
				        </div>
				        <div class="login-feature">
				          <div class="login-feature-icon">🗂️</div>
				          <span>Armazenamento dinâmico no workspace interativo</span>
				        </div>
				      </div>
				    </div>
				  </div>
				  <div class="login-form-side">
				    <div class="login-form-header">
				      <h2>Bem-vindo de volta</h2>
				      <p>Insira suas credenciais para continuar.</p>
				    </div>
				    %s
				    <form class="login-form" method="POST" action="/login">
				      <div class="form-group">
				        <label for="usuario">Usuário</label>
				        <input id="usuario" name="usuario" type="text" autocomplete="username" placeholder="seu.usuario" required>
				      </div>
				      <div class="form-group">
				        <label for="senha">Senha</label>
				        <input id="senha" name="senha" type="password" autocomplete="current-password" placeholder="••••••••" required>
				      </div>
				      <div class="form-actions">
				        <button type="submit" class="btn btn-primary btn-block">Acessar Vaultra</button>
				      </div>
				      <p class="login-hint">Acesso seguro garantido por HMAC-SHA256.</p>
				    </form>
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
