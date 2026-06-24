package Security;

import java.util.List;

public final class DashboardPages {

	private DashboardPages() {}

	public static String paginaAdmin(User admin, String usersJson) {
		return """
				<!DOCTYPE html>
				<html lang="pt-BR">
				<head>
				  <meta charset="UTF-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1.0">
				  <title>Vaultra \u2014 Painel de Controle</title>
				  <link rel="stylesheet" href="/css/vaultra.css">
				</head>
				<body class="dashboard-layout">
				  <!-- Sidebar -->
				  <aside class="sidebar">
				    <div class="sidebar-brand">
				      <div class="sidebar-logo">
				        <!-- TODO: SVG Logo -->
				        <span class="sidebar-logo-letter">V</span>
				      </div>
				      <div class="sidebar-brand-text">Vaultra</div>
				    </div>
				    
				    <div class="sidebar-section-label">Plataforma</div>
				    <button class="menu-item active" onclick="switchTab('visao-geral')">
				      Visão Geral
				    </button>
				    <button class="menu-item" onclick="switchTab('projetos'); carregarProjetos();">
				      Projetos & Workspace
				    </button>
				    
				    <div class="sidebar-section-label">Administração</div>
				    <button class="menu-item" onclick="switchTab('usuarios')">
				      Gerenciar Usuários
				    </button>
				    <button class="menu-item" onclick="switchTab('sandbox')">
				      Console Vaultra
				    </button>
				    
				    <a href="/logout" class="menu-item logout mt-auto">Sair</a>
				  </aside>

				  <!-- Main Content -->
				  <main class="main-content">
				    <header class="page-header">
				      <div>
				        <h1>Painel de Controle Administrador</h1>
				        <p class="page-header-subtitle">Gerenciamento de infraestrutura e acessos corporativos</p>
				      </div>
				      <div class="user-badge">
				        <span>%s</span>
				        <span class="badge-role badge-admin">ADMIN</span>
				      </div>
				    </header>

				    <!-- Tab: Visão Geral -->
				    <div id="visao-geral" class="tab-panel active">
				      <div class="stats-grid">
				        <div class="stat-card">
				          <div class="stat-card-label">Status do Servidor</div>
				          <div class="stat-card-value success">Ativo</div>
				        </div>
				        <div class="stat-card">
				          <div class="stat-card-label">Sessão JWT</div>
				          <div class="stat-card-value info">24 Horas</div>
				        </div>
				        <div class="stat-card">
				          <div class="stat-card-label">Total de Usuários</div>
				          <div class="stat-card-value" id="count-usuarios">0</div>
				        </div>
				      </div>

				      <div class="card">
				        <h2 class="card-title">Políticas de Segurança do Servidor</h2>
				        <ul class="policy-list">
				          <li><span class="policy-icon">✓</span> Autenticação por token JWT (HMAC-SHA256) persistido em cookies seguros HttpOnly.</li>
				          <li><span class="policy-icon">✓</span> Criptografia de senhas usando algoritmo SHA-256 com salts individuais de 128-bits.</li>
				          <li><span class="policy-icon">✓</span> Mapeamento granular de permissões de caminhos com suporte a wildcards (ex: <code>/workspace/modelagem/*</code>).</li>
				          <li><span class="policy-icon">✓</span> Cabeçalhos de segurança injetados em todas as respostas HTTP.</li>
				        </ul>
				      </div>
				    </div>

				    <!-- Tab: Gerenciar Usuários -->
				    <div id="usuarios" class="tab-panel">
				      <div class="section-grid">
				        <div>
				          <h2 class="card-title">Usuários do Sistema</h2>
				          <table id="tabela-usuarios">
				            <thead>
				              <tr>
				                <th>ID</th>
				                <th>Usuário</th>
				                <th>Cargo</th>
				                <th>Status</th>
				                <th>Ações</th>
				              </tr>
				            </thead>
				            <tbody></tbody>
				          </table>
				        </div>
				        
				        <div class="card">
				          <h3 class="card-title">Novo Usuário</h3>
				          <form id="form-criar-usuario" onsubmit="criarUsuario(event)">
				            <div class="form-group">
				              <label for="username">Nome de Usuário</label>
				              <input type="text" id="username" required autocomplete="off" placeholder="usuario.novo">
				            </div>
				            <div class="form-group">
				              <label for="senha">Senha Inicial</label>
				              <input type="password" id="senha" required placeholder="••••••••">
				            </div>
				            <div class="form-group">
				              <label for="cargo">Cargo</label>
				              <select id="cargo">
				                <option value="VISUALIZADOR">Visualizador</option>
				                <option value="MODELADOR">Modelador 3D</option>
				                <option value="ARQUITETO">Arquiteto de Placas</option>
				                <option value="DESENVOLVEDOR">Desenvolvedor CAD</option>
				                <option value="ADMIN">Administrador</option>
				              </select>
				            </div>
				            <button type="submit" class="btn btn-primary btn-block mt-16">Criar Conta</button>
				          </form>
				        </div>
				      </div>
				    </div>

				    <!-- Tab: Projetos & Canvas -->
				    <div id="projetos" class="tab-panel">
				      <div class="section-grid">
				        <div>
				          <h2 class="card-title">Projetos Vaultra</h2>
				          <div class="project-grid" id="lista-projetos">
				            <!-- Injetados via JS -->
				          </div>
				        </div>

				        <div class="card">
				          <h3 class="card-title">Novo Projeto</h3>
				          <form id="form-criar-projeto" onsubmit="criarProjeto(event)">
				            <div class="form-group">
				              <label for="proj-nome">Nome do Projeto</label>
				              <input type="text" id="proj-nome" required placeholder="Ex: Infraestrutura Alfa">
				            </div>
				            <div class="form-group">
				              <label for="proj-desc">Descrição</label>
				              <textarea id="proj-desc" rows="3" placeholder="Modelagem 3D, arquitetura e arquivos..."></textarea>
				            </div>
				            <button type="submit" class="btn btn-primary btn-block mt-16">Inicializar Projeto</button>
				          </form>
				        </div>
				      </div>
				    </div>

				    <!-- Tab: Sandbox -->
				    <div id="sandbox" class="tab-panel">
				      <h2 class="card-title">Console Vaultra Administrativo</h2>
				      <div class="console" id="logs-console">
				        <div class="console-line info">[SISTEMA] Conectando ao cluster do servidor Vaultra...</div>
				        <div class="console-line system">[INFO] Autenticação forte estabelecida via HMAC-SHA256.</div>
				      </div>
				      <div class="console-input-row">
				        <input type="text" id="sandbox-input" placeholder="Digite comandos aqui (help, status, users)..." onkeydown="if(event.key === 'Enter') enviarComandoSandbox()">
				        <button class="btn btn-primary" onclick="enviarComandoSandbox()">Executar</button>
				      </div>
				    </div>
				  </main>

				  <script>
				    const usuarios = %s;

				    function switchTab(tabId) {
				      document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
				      document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));
				      
				      document.getElementById(tabId).classList.add('active');
				      if(event && event.currentTarget) {
				        event.currentTarget.classList.add('active');
				      }
				    }

				    function getRoleBadgeClass(cargo) {
				      const c = cargo.toLowerCase();
				      if (c === 'admin') return 'badge-admin';
				      if (c === 'modelador') return 'badge-modelador';
				      if (c === 'arquiteto') return 'badge-arquiteto';
				      if (c === 'desenvolvedor') return 'badge-desenvolvedor';
				      return 'badge-visualizador';
				    }

				    function renderUsuarios() {
				      const tbody = document.querySelector('#tabela-usuarios tbody');
				      tbody.innerHTML = '';
				      document.getElementById('count-usuarios').innerText = usuarios.length;

				      usuarios.forEach(u => {
				        const tr = document.createElement('tr');
				        tr.innerHTML = `
				          <td class="text-dim">#${u.id}</td>
				          <td class="fw-600">${u.username}</td>
				          <td><span class="badge-role ${getRoleBadgeClass(u.cargo)}">${u.cargo_nome}</span></td>
				          <td class="${u.ativo ? 'status-active' : 'status-inactive'}">${u.ativo ? 'Ativo' : 'Inativo'}</td>
				          <td>
				            ${u.id === 1 ? '<span class="text-dim font-mono">Root</span>' : `<button class="btn btn-danger btn-sm" onclick="removerUsuario(${u.id})">Remover</button>`}
				          </td>
				        `;
				        tbody.appendChild(tr);
				      });
				    }

				    function criarUsuario(e) {
				      e.preventDefault();
				      const username = document.getElementById('username').value;
				      const senha = document.getElementById('senha').value;
				      const cargo = document.getElementById('cargo').value;

				      fetch('/api/admin/usuarios', {
				        method: 'POST',
				        headers: { 'Content-Type': 'application/json' },
				        body: JSON.stringify({ username, senha, cargo })
				      })
				      .then(res => res.json())
				      .then(data => {
				        if (data.status === 'success') {
				          usuarios.push({
				            id: data.id,
				            username: data.username,
				            cargo: data.cargo,
				            cargo_nome: cargo,
				            ativo: true
				          });
				          renderUsuarios();
				          document.getElementById('form-criar-usuario').reset();
				          adicionarLog(`[INFO] Usuário '${data.username}' adicionado com sucesso.`, 'info');
				        } else {
				          alert(data.error || 'Erro ao criar usuário');
				        }
				      });
				    }

				    function removerUsuario(id) {
				      if (!confirm('Deseja realmente remover este usuário?')) return;
				      fetch(`/api/admin/usuarios?id=${id}`, { method: 'DELETE' })
				      .then(res => res.json())
				      .then(data => {
				        if (data.status === 'success') {
				          const idx = usuarios.findIndex(u => u.id === id);
				          if (idx !== -1) {
				            const u = usuarios.splice(idx, 1)[0];
				            renderUsuarios();
				            adicionarLog(`[AVISO] Usuário '${u.username}' removido do sistema.`, 'warn');
				          }
				        } else {
				          alert(data.error || 'Erro ao remover usuário');
				        }
				      });
				    }

				    function carregarProjetos() {
				      fetch('/api/projetos')
				      .then(res => res.json())
				      .then(projetos => {
				        const container = document.getElementById('lista-projetos');
				        container.innerHTML = '';
				        if (projetos.length === 0) {
				          container.innerHTML = '<p class="text-muted">Nenhum projeto encontrado.</p>';
				          return;
				        }
				        projetos.forEach(p => {
				          const card = document.createElement('div');
				          card.className = 'project-card';
				          card.innerHTML = `
				            <div class="project-card-header">
				              <span class="project-card-title">
				                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>
				                ${p.nome}
				              </span>
				              <button onclick="deletarProjeto(${p.id}, event)" class="btn-icon text-error" title="Remover Projeto">
				                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
				              </button>
				            </div>
				            <p class="project-card-desc">${p.descricao || 'Sem descrição.'}</p>
				            <div class="project-card-footer">
				              <span class="project-card-id">#${p.id}</span>
				              <a href="/workspace/projeto/${p.id}" target="_blank" class="project-card-link">Acessar Workspace →</a>
				            </div>
				          `;
				          container.appendChild(card);
				        });
				      });
				    }

				    function criarProjeto(e) {
				      e.preventDefault();
				      const nome = document.getElementById('proj-nome').value;
				      const descricao = document.getElementById('proj-desc').value;

				      fetch('/api/projetos', {
				        method: 'POST',
				        headers: { 'Content-Type': 'application/json' },
				        body: JSON.stringify({ nome, descricao })
				      })
				      .then(res => res.json())
				      .then(data => {
				        if (data.status === 'success') {
				          document.getElementById('form-criar-projeto').reset();
				          carregarProjetos();
				        } else {
				          alert("Erro ao criar projeto: " + data.message);
				        }
				      });
				    }

				    function deletarProjeto(id, event) {
				      event.stopPropagation();
				      if (!confirm("Tem certeza que deseja deletar este projeto? Todos os dados serão perdidos.")) return;
				      fetch(`/api/projetos/${id}`, { method: 'DELETE' })
				      .then(res => res.json())
				      .then(() => carregarProjetos());
				    }

				    function adicionarLog(texto, classe = '') {
				      const con = document.getElementById('logs-console');
				      const div = document.createElement('div');
				      div.className = `console-line ${classe}`;
				      div.innerText = `[${new Date().toLocaleTimeString()}] ${texto}`;
				      con.appendChild(div);
				      con.scrollTop = con.scrollHeight;
				    }

				    function enviarComandoSandbox() {
				      const input = document.getElementById('sandbox-input');
				      const cmd = input.value.trim();
				      if (!cmd) return;

				      adicionarLog(`> ${cmd}`, 'dim');
				      input.value = '';

				      fetch('/api/admin/sandbox/exec', {
				        method: 'POST',
				        headers: { 'Content-Type': 'application/json' },
				        body: JSON.stringify({ comando: cmd })
				      })
				      .then(res => res.json())
				      .then(data => {
				        if (data.status === 'success') {
				          if (cmd.toLowerCase() === 'clear') {
				            document.getElementById('logs-console').innerHTML = '';
				            return;
				          }
				          const lines = data.output.split('\\n');
				          lines.forEach(line => {
				            if (line.trim() !== '') adicionarLog(line, 'info');
				          });
				        } else {
				          adicionarLog(`[ERRO] ${data.error}`, 'err');
				        }
				      })
				      .catch(err => {
				        adicionarLog(`[ERRO DE CONEXÃO] Não foi possível acessar o servidor Sandbox.`, 'err');
				      });
				    }

				    // Init
				    renderUsuarios();
				  </script>
				</body>
				</html>
				""".formatted(admin.getUsername(), usersJson);
	}

	public static String paginaUserHome(User user) {
		StringBuilder permsList = new StringBuilder();
		if (user.getPermissoes().isEmpty()) {
			permsList.append("<li class=\"text-muted\">Nenhuma permissão específica configurada.</li>");
		} else {
			for (Permissao p : user.getPermissoes()) {
				permsList.append("<li>Caminho: <code>%s</code> | Ler: <b>%s</b> | Escrever: <b>%s</b></li>"
						.formatted(p.getCaminhoPermitido(), p.isPodeVer() ? "Sim" : "Não", p.isPodeEditar() ? "Sim" : "Não"));
			}
		}

		return """
				<!DOCTYPE html>
				<html lang="pt-BR">
				<head>
				  <meta charset="UTF-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1.0">
				  <title>Vaultra \u2014 Workspace Privado</title>
				  <link rel="stylesheet" href="/css/vaultra.css">
				</head>
				<body class="dashboard-layout">
				  <!-- Sidebar -->
				  <aside class="sidebar">
				    <div class="sidebar-brand">
				      <div class="sidebar-logo">
				        <!-- TODO: SVG Logo -->
				        <span class="sidebar-logo-letter">V</span>
				      </div>
				      <div class="sidebar-brand-text">Vaultra</div>
				    </div>
				    
				    <div class="sidebar-section-label">Plataforma</div>
				    <button class="menu-item active" onclick="switchTab('perfil')">Meu Perfil</button>
				    <button class="menu-item" onclick="switchTab('projetos'); carregarProjetos();">Projetos & Workspace</button>
				    
				    <div class="sidebar-section-label">Acesso</div>
				    <button class="menu-item" onclick="switchTab('sandbox')">Console Privado</button>
				    
				    <a href="/logout" class="menu-item logout mt-auto">Sair</a>
				  </aside>

				  <!-- Main Content -->
				  <main class="main-content">
				    <header class="page-header">
				      <div>
				        <h1>Área de Trabalho Segura</h1>
				        <p class="page-header-subtitle">Vaultra Data Platform</p>
				      </div>
				      <div class="user-badge">
				        <span>%s</span>
				        <span class="badge-role badge-%s">%s</span>
				      </div>
				    </header>

				    <!-- Tab: Perfil -->
				    <div id="perfil" class="tab-panel active">
				      <div class="card mb-24">
				        <h2 class="card-title">Informações de Acesso</h2>
				        <table style="width: 100%%; border-collapse: collapse;">
				          <tr>
				            <td class="text-muted fw-600">Usuário</td>
				            <td class="text-right fw-600">%s</td>
				          </tr>
				          <tr>
				            <td class="text-muted fw-600">Cargo Atribuído</td>
				            <td class="text-right text-accent fw-600">%s</td>
				          </tr>
				          <tr>
				            <td class="text-muted fw-600">Escopo de Acesso</td>
				            <td class="text-right text-dim">%s</td>
				          </tr>
				        </table>
				      </div>

				      <div class="card">
				        <h2 class="card-title">Regras de Acesso e Permissões</h2>
				        <ul class="policy-list">
				          %s
				        </ul>
				      </div>
				    </div>

				    <!-- Tab: Projetos & Canvas -->
				    <div id="projetos" class="tab-panel">
				      <div class="section-grid">
				        <div>
				          <h2 class="card-title">Meus Projetos Vaultra</h2>
				          <div class="project-grid" id="lista-projetos">
				            <!-- Injetados por JS -->
				          </div>
				        </div>

				        <div class="card">
				          <h3 class="card-title">Novo Projeto</h3>
				          <form id="form-criar-projeto" onsubmit="criarProjeto(event)">
				            <div class="form-group">
				              <label for="proj-nome">Nome do Projeto</label>
				              <input type="text" id="proj-nome" required placeholder="Ex: Planta Industrial Beta">
				            </div>
				            <div class="form-group">
				              <label for="proj-desc">Descrição</label>
				              <textarea id="proj-desc" rows="3" placeholder="Informações..."></textarea>
				            </div>
				            <button type="submit" class="btn btn-primary btn-block mt-16">Inicializar Projeto</button>
				          </form>
				        </div>
				      </div>
				    </div>

				    <!-- Tab: Sandbox -->
				    <div id="sandbox" class="tab-panel">
				      <h2 class="card-title">Console Restrito para %s</h2>
				      <div class="console">
				        <div class="console-line system">[SISTEMA] Inicializando sandbox do cargo %s...</div>
				        <div class="console-line info">[INFO] Conexão segura estabelecida.</div>
				        <div class="console-line success">[VERIFICADO] Permissões carregadas.</div>
				      </div>
				    </div>
				  </main>

				  <script>
				    function switchTab(tabId) {
				      document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
				      document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));
				      
				      document.getElementById(tabId).classList.add('active');
				      if(event && event.currentTarget) {
				        event.currentTarget.classList.add('active');
				      }
				    }

				    function carregarProjetos() {
				      fetch('/api/projetos')
				      .then(res => res.json())
				      .then(projetos => {
				        const container = document.getElementById('lista-projetos');
				        container.innerHTML = '';
				        if (projetos.length === 0) {
				          container.innerHTML = '<p class="text-muted">Nenhum projeto encontrado.</p>';
				          return;
				        }
				        projetos.forEach(p => {
				          const card = document.createElement('div');
				          card.className = 'project-card';
				          card.innerHTML = `
				            <div class="project-card-header">
				              <span class="project-card-title">
				                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>
				                ${p.nome}
				              </span>
				            </div>
				            <p class="project-card-desc">${p.descricao || 'Sem descrição.'}</p>
				            <div class="project-card-footer">
				              <span class="project-card-id">#${p.id}</span>
				              <a href="/workspace/projeto/${p.id}" target="_blank" class="project-card-link">Acessar Workspace →</a>
				            </div>
				          `;
				          container.appendChild(card);
				        });
				      });
				    }

				    function criarProjeto(e) {
				      e.preventDefault();
				      const nome = document.getElementById('proj-nome').value;
				      const descricao = document.getElementById('proj-desc').value;

				      fetch('/api/projetos', {
				        method: 'POST',
				        headers: { 'Content-Type': 'application/json' },
				        body: JSON.stringify({ nome, descricao })
				      })
				      .then(res => res.json())
				      .then(data => {
				        if (data.status === 'success') {
				          document.getElementById('form-criar-projeto').reset();
				          carregarProjetos();
				        } else {
				          alert("Erro ao criar projeto: " + data.message);
				        }
				      });
				    }
				  </script>
				</body>
				</html>
				""".formatted(
						user.getUsername(), user.getCargo().name().toLowerCase(), user.getCargo().getNome(),
						user.getUsername(), user.getCargo().name(), user.getCargo().getDescricao(),
						permsList.toString(), user.getUsername(), user.getCargo().name());
	}
}
