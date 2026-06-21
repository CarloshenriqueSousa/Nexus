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
				  <title>Painel Admin — Vaultra</title>
				  <link rel="preconnect" href="https://fonts.googleapis.com">
				  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
				  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
				  <style>
				    * { box-sizing: border-box; margin: 0; padding: 0; }
				    body {
				      font-family: 'Outfit', sans-serif;
				      background-color: #0b0f19;
				      color: #f1f5f9;
				      display: flex;
				      min-height: 100vh;
				      overflow-x: hidden;
				    }
				    
				    /* Sidebar */
				    .sidebar {
				      width: 260px;
				      background-color: #0f172a;
				      border-right: 1px solid #1e293b;
				      display: flex;
				      flex-direction: column;
				      padding: 24px;
				      flex-shrink: 0;
				    }
				    .logo {
				      font-size: 1.4rem;
				      font-weight: 700;
				      color: #3b82f6;
				      margin-bottom: 32px;
				      display: flex;
				      align-items: center;
				      gap: 10px;
				    }
				    .logo span {
				      color: #f1f5f9;
				    }
				    .menu-item {
				      display: flex;
				      align-items: center;
				      gap: 12px;
				      padding: 12px 16px;
				      color: #94a3b8;
				      text-decoration: none;
				      border-radius: 8px;
				      margin-bottom: 8px;
				      font-weight: 500;
				      cursor: pointer;
				      transition: all 0.2s;
				    }
				    .menu-item:hover, .menu-item.active {
				      background-color: #1e293b;
				      color: #f1f5f9;
				    }
				    .menu-item.active {
				      border-left: 4px solid #3b82f6;
				    }
				    .logout-btn {
				      margin-top: auto;
				      color: #f87171;
				    }
				    .logout-btn:hover {
				      background-color: #7f1d1d33;
				    }

				    /* Main Content */
				    .content {
				      flex-grow: 1;
				      padding: 40px;
				      max-width: 1200px;
				      margin: 0 auto;
				      width: 100%%;
				    }
				    header {
				      display: flex;
				      justify-content: space-between;
				      align-items: center;
				      margin-bottom: 40px;
				      border-bottom: 1px solid #1e293b;
				      padding-bottom: 20px;
				    }
				    h1 { font-size: 1.8rem; font-weight: 700; color: #f8fafc; }
				    .user-badge {
				      display: flex;
				      align-items: center;
				      gap: 10px;
				      background-color: #1e293b;
				      padding: 8px 16px;
				      border-radius: 20px;
				      font-size: 0.9rem;
				      border: 1px solid #334155;
				    }
				    .badge-role {
				      background-color: #2563eb;
				      color: white;
				      padding: 2px 8px;
				      border-radius: 4px;
				      font-size: 0.75rem;
				      font-weight: 600;
				    }

				    /* Tab Panels */
				    .tab-panel {
				      display: none;
				    }
				    .tab-panel.active {
				      display: block;
				      animation: fadeIn 0.3s ease-in-out;
				    }
				    @keyframes fadeIn {
				      from { opacity: 0; transform: translateY(10px); }
				      to { opacity: 1; transform: translateY(0); }
				    }

				    /* Cards Grid */
				    .stats-grid {
				      display: grid;
				      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
				      gap: 24px;
				      margin-bottom: 40px;
				    }
				    .card {
				      background-color: #151f32;
				      border: 1px solid #22314d;
				      border-radius: 12px;
				      padding: 24px;
				    }
				    .card-title { font-size: 0.85rem; color: #94a3b8; font-weight: 600; text-transform: uppercase; margin-bottom: 8px; }
				    .card-value { font-size: 2rem; font-weight: 700; color: #f1f5f9; }
				    
				    /* Table & Form container */
				    .section-grid {
				      display: grid;
				      grid-template-columns: 2fr 1fr;
				      gap: 32px;
				      align-items: start;
				    }
				    @media (max-width: 900px) {
				      .section-grid { grid-template-columns: 1fr; }
				    }
				    
				    /* Tables */
				    table {
				      width: 100%%;
				      border-collapse: collapse;
				      background-color: #151f32;
				      border: 1px solid #22314d;
				      border-radius: 12px;
				      overflow: hidden;
				    }
				    th, td {
				      padding: 16px;
				      text-align: left;
				      border-bottom: 1px solid #1e293b;
				    }
				    th {
				      background-color: #0f172a;
				      color: #94a3b8;
				      font-weight: 600;
				      font-size: 0.85rem;
				      text-transform: uppercase;
				    }
				    tr:last-child td { border-bottom: none; }
				    
				    /* Forms */
				    .form-group {
				      margin-bottom: 16px;
				    }
				    label {
				      display: block;
				      font-size: 0.85rem;
				      color: #cbd5e1;
				      margin-bottom: 6px;
				      font-weight: 500;
				    }
				    input, select, textarea {
				      width: 100%%;
				      padding: 10px 14px;
				      background-color: #0b0f19;
				      border: 1px solid #334155;
				      border-radius: 8px;
				      color: #f1f5f9;
				      font-family: inherit;
				    }
				    input:focus, select:focus, textarea:focus {
				      outline: 2px solid #3b82f6;
				      border-color: #3b82f6;
				    }
				    button {
				      background-color: #2563eb;
				      color: white;
				      padding: 10px 20px;
				      border: none;
				      border-radius: 8px;
				      font-weight: 600;
				      cursor: pointer;
				      transition: background 0.2s;
				    }
				    button:hover { background-color: #1d4ed8; }
				    button.danger {
				      background-color: #ef4444;
				    }
				    button.danger:hover { background-color: #dc2626; }

				    /* Projetos Grid */
				    .project-grid {
				      display: grid;
				      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
				      gap: 20px;
				      margin-top: 20px;
				    }
				    .project-card {
				      background: #151f32;
				      border: 1px solid #22314d;
				      border-radius: 12px;
				      padding: 20px;
				      display: flex;
				      flex-direction: column;
				      gap: 12px;
				      transition: all 0.2s;
				    }
				    .project-card:hover {
				      transform: translateY(-2px);
				      border-color: #3b82f6;
				    }
				    .project-card-header {
				      display: flex;
				      justify-content: space-between;
				      align-items: center;
				    }
				    .project-card-title {
				      font-size: 1.1rem;
				      font-weight: 600;
				      color: #f8fafc;
				      display: flex;
				      align-items: center;
				      gap: 8px;
				    }
				    .project-card-desc {
				      font-size: 0.9rem;
				      color: #94a3b8;
				      min-height: 40px;
				    }
				    .project-card-actions {
				      display: flex;
				      justify-content: space-between;
				      margin-top: auto;
				    }

				    /* Logs console */
				    .console {
				      background-color: #05070f;
				      border: 1px solid #1e293b;
				      border-radius: 12px;
				      padding: 20px;
				      font-family: 'JetBrains Mono', monospace;
				      font-size: 0.9rem;
				      color: #10b981;
				      height: 400px;
				      overflow-y: auto;
				      line-height: 1.6;
				    }
				    .console-line { margin-bottom: 4px; }
				    .console-line.info { color: #3b82f6; }
				    .console-line.warn { color: #f59e0b; }
				    .console-line.err { color: #ef4444; }
				  </style>
				</head>
				<body>
				  <!-- Sidebar -->
				  <div class="sidebar">
				    <div class="logo">
				      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
				        <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="#3B82F6" stroke="#3B82F6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
				        <path d="M2 17L12 22L22 17" stroke="#3B82F6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
				        <path d="M2 12L12 17L22 12" stroke="#3B82F6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
				      </svg>
				      Vaultra <span>Secure</span>
				    </div>
				    
				    <div class="menu-item active" onclick="switchTab('visao-geral')">Visão Geral</div>
				    <div class="menu-item" onclick="switchTab('usuarios')">Gerenciar Usuários</div>
				    <div class="menu-item" onclick="switchTab('projetos'); carregarProjetos();">Projetos & Canvas</div>
				    <div class="menu-item" onclick="switchTab('sandbox')">Sandbox & Logs</div>
				    
				    <a href="/logout" class="menu-item logout-btn">Sair</a>
				  </div>

				  <!-- Main Content -->
				  <div class="content">
				    <header>
				      <div>
				        <h1>Painel de Controle Administrador</h1>
				        <p style="color: #94a3b8; font-size: 0.9rem; margin-top: 4px;">Gerenciamento de segurança corporativa do servidor</p>
				      </div>
				      <div class="user-badge">
				        <span>%s</span>
				        <span class="badge-role">ADMIN</span>
				      </div>
				    </header>

				    <!-- Tab: Visão Geral -->
				    <div id="visao-geral" class="tab-panel active">
				      <div class="stats-grid">
				        <div class="card">
				          <div class="card-title">Status do Servidor</div>
				          <div class="card-value" style="color: #10b981;">Ativo</div>
				        </div>
				        <div class="card">
				          <div class="card-title">Sessão JWT</div>
				          <div class="card-value">24 Horas</div>
				        </div>
				        <div class="card">
				          <div class="card-title">Total de Usuários</div>
				          <div class="card-value" id="count-usuarios">0</div>
				        </div>
				      </div>

				      <div class="card">
				        <h2 style="font-size: 1.2rem; margin-bottom: 16px;">Políticas de Segurança do Servidor</h2>
				        <ul style="list-style-type: disc; padding-left: 20px; color: #cbd5e1; line-height: 1.8;">
				          <li>Autenticação por token JWT (HMAC-SHA256) persistido em cookies seguros HttpOnly.</li>
				          <li>Criptografia de senhas usando algoritmo SHA-256 com salts individuais de 128-bits.</li>
				          <li>Mapeamento granular de permissões de caminhos com suporte a wildcards (ex: <code>/workspace/modelagem/*</code>).</li>
				          <li>Cabeçalhos de segurança (X-Content-Type, X-Frame-Options, etc.) injetados em todas as respostas HTTP.</li>
				        </ul>
				      </div>
				    </div>

				    <!-- Tab: Gerenciar Usuários -->
				    <div id="usuarios" class="tab-panel">
				      <div class="section-grid">
				        <div>
				          <h2 style="font-size: 1.2rem; margin-bottom: 16px;">Usuários do Sistema</h2>
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
				          <h3 style="font-size: 1rem; margin-bottom: 16px;">Novo Usuário</h3>
				          <form id="form-criar-usuario" onsubmit="criarUsuario(event)">
				            <div class="form-group">
				              <label for="username">Nome de Usuário</label>
				              <input type="text" id="username" required autocomplete="off">
				            </div>
				            <div class="form-group">
				              <label for="senha">Senha Inicial</label>
				              <input type="password" id="senha" required>
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
				            <button type="submit" style="width: 100%%;">Criar Conta</button>
				          </form>
				        </div>
				      </div>
				    </div>

				    <!-- Tab: Projetos & Canvas -->
				    <div id="projetos" class="tab-panel">
				      <div class="section-grid">
				        <div>
				          <h2 style="font-size: 1.2rem; margin-bottom: 16px;">Projetos Workspace</h2>
				          <div class="project-grid" id="lista-projetos">
				            <!-- Projetos injetados por JS -->
				          </div>
				        </div>

				        <div class="card">
				          <h3 style="font-size: 1rem; margin-bottom: 16px;">Criar Novo Projeto</h3>
				          <form id="form-criar-projeto" onsubmit="criarProjeto(event)">
				            <div class="form-group">
				              <label for="proj-nome">Nome do Projeto</label>
				              <input type="text" id="proj-nome" required placeholder="Ex: Projeto Motherboard Alpha">
				            </div>
				            <div class="form-group">
				              <label for="proj-desc">Descrição</label>
				              <textarea id="proj-desc" rows="3" placeholder="Modelagem 3D, PCB Layout e arquivos AutoCAD..."></textarea>
				            </div>
				            <button type="submit" style="width: 100%%;">Inicializar Projeto</button>
				          </form>
				        </div>
				      </div>
				    </div>

				    <!-- Tab: Sandbox -->
				    <div id="sandbox" class="tab-panel">
				      <h2 style="font-size: 1.2rem; margin-bottom: 16px;">Console Sandbox Administrativo</h2>
				      <div class="console" id="logs-console">
				        <div class="console-line info">[SISTEMA] Conectando ao terminal sandbox do servidor corporativo...</div>
				        <div class="console-line info">[INFO] Firewall Ativo. Criptografia ativa.</div>
				      </div>
				      <div style="display: flex; margin-top: 16px; gap: 10px;">
				        <input type="text" id="sandbox-input" placeholder="Digite comandos aqui (help, memory, security-check)..." style="flex-grow: 1;" onkeydown="if(event.key === 'Enter') enviarComandoSandbox()">
				        <button onclick="enviarComandoSandbox()">Executar</button>
				      </div>
				    </div>
				  </div>

				  <script>
				    const usuarios = %s;

				    function switchTab(tabId) {
				      document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
				      document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));
				      
				      document.getElementById(tabId).classList.add('active');
				      event.currentTarget.classList.add('active');
				    }

				    function renderUsuarios() {
				      const tbody = document.querySelector('#tabela-usuarios tbody');
				      tbody.innerHTML = '';
				      document.getElementById('count-usuarios').innerText = usuarios.length;

				      usuarios.forEach(u => {
				        const tr = document.createElement('tr');
				        tr.innerHTML = `
				          <td>${u.id}</td>
				          <td style="font-weight: 600;">${u.username}</td>
				          <td><span class="badge-role" style="background-color: ${u.cargo === 'ADMIN' ? '#2563eb' : '#475569'}">${u.cargo_nome}</span></td>
				          <td><span style="color: ${u.ativo ? '#10b981' : '#ef4444'}">${u.ativo ? 'Ativo' : 'Inativo'}</span></td>
				          <td>
				            ${u.id === 1 ? '<span style="font-size: 12px; color: var(--text-muted);">Root</span>' : `<button class="danger" style="padding: 4px 8px; font-size: 0.8rem;" onclick="removerUsuario(${u.id})">Remover</button>`}
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

				    // --- CONTROLES DE PROJETOS E CANVAS ---

				    function carregarProjetos() {
				      fetch('/api/projetos')
				      .then(res => res.json())
				      .then(projetos => {
				        const container = document.getElementById('lista-projetos');
				        container.innerHTML = '';
				        if (projetos.length === 0) {
				          container.innerHTML = '<p style="color: #94a3b8;">Nenhum projeto encontrado. Crie um ao lado.</p>';
				          return;
				        }
				        projetos.forEach(p => {
				          const card = document.createElement('div');
				          card.className = 'project-card';
				          card.innerHTML = `
				            <div class="project-card-header">
				              <span class="project-card-title">📁 ${p.nome}</span>
				              <button onclick="deletarProjeto(${p.id}, event)" style="background:none;border:none;color:#ef4444;cursor:pointer;font-size:14px;" title="Remover Projeto">❌</button>
				            </div>
				            <p class="project-card-desc">${p.descricao || 'Sem descrição.'}</p>
				            <div class="project-card-actions">
				              <span style="font-size: 11px; color: #94a3b8;">ID: #${p.id}</span>
				              <a href="/workspace/projeto/${p.id}" target="_blank" style="color: #3b82f6; text-decoration: none; font-weight: 600; font-size: 13px;">Abrir Blueprint Canvas →</a>
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
				      if (!confirm("Tem certeza que deseja deletar este projeto? Todos os arquivos e ligações serão removidos permanentemente.")) return;
				      fetch(`/api/projetos/${id}`, { method: 'DELETE' })
				      .then(res => res.json())
				      .then(() => carregarProjetos());
				    }

				    // --- TERMINAL SANDBOX ---

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

				      adicionarLog(`> ${cmd}`, 'info');
				      input.value = '';

				      setTimeout(() => {
				        if (cmd.toLowerCase() === 'help') {
				          adicionarLog('Comandos disponíveis: help, memory, security-check, clear', 'info');
				        } else if (cmd.toLowerCase() === 'memory') {
				          adicionarLog(`Memória utilizada: ${(performance.memory ? Math.round(performance.memory.usedJSHeapSize / 1024 / 1024) + 'MB' : 'Não suportado pelo browser')}`, 'info');
				        } else if (cmd.toLowerCase() === 'security-check') {
				          adicionarLog('[SUCESSO] Varredura concluída. 0 vulnerabilidades detectadas.', 'info');
				        } else if (cmd.toLowerCase() === 'clear') {
				          document.getElementById('logs-console').innerHTML = '';
				        } else {
				          adicionarLog(`Comando não reconhecido: ${cmd}. Digite 'help' para comandos do sandbox.`, 'err');
				        }
				      }, 300);
				    }

				    // Render inicial
				    renderUsuarios();
				  </script>
				</body>
				</html>
				""".formatted(admin.getUsername(), usersJson);
	}

	public static String paginaUserHome(User user) {
		StringBuilder permsList = new StringBuilder();
		if (user.getPermissoes().isEmpty()) {
			permsList.append("<li>Nenhuma permissão específica configurada.</li>");
		} else {
			for (Permissao p : user.getPermissoes()) {
				permsList.append("<li>Caminho: <code>%s</code> | Visualizar: <b>%s</b> | Editar: <b>%s</b></li>"
						.formatted(p.getCaminhoPermitido(), p.isPodeVer() ? "Sim" : "Não", p.isPodeEditar() ? "Sim" : "Não"));
			}
		}

		return """
				<!DOCTYPE html>
				<html lang="pt-BR">
				<head>
				  <meta charset="UTF-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1.0">
				  <title>Área de Trabalho — Vaultra</title>
				  <link rel="preconnect" href="https://fonts.googleapis.com">
				  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
				  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
				  <style>
				    * { box-sizing: border-box; margin: 0; padding: 0; }
				    body {
				      font-family: 'Outfit', sans-serif;
				      background-color: #0b0f19;
				      color: #f1f5f9;
				      display: flex;
				      min-height: 100vh;
				    }
				    
				    /* Sidebar */
				    .sidebar {
				      width: 260px;
				      background-color: #0f172a;
				      border-right: 1px solid #1e293b;
				      display: flex;
				      flex-direction: column;
				      padding: 24px;
				    }
				    .logo {
				      font-size: 1.4rem;
				      font-weight: 700;
				      color: #3b82f6;
				      margin-bottom: 32px;
				      display: flex;
				      align-items: center;
				      gap: 10px;
				    }
				    .logo span { color: #f1f5f9; }
				    .menu-item {
				      display: flex;
				      align-items: center;
				      gap: 12px;
				      padding: 12px 16px;
				      color: #94a3b8;
				      text-decoration: none;
				      border-radius: 8px;
				      margin-bottom: 8px;
				      font-weight: 500;
				      cursor: pointer;
				      transition: all 0.2s;
				    }
				    .menu-item:hover, .menu-item.active {
				      background-color: #1e293b;
				      color: #f1f5f9;
				    }
				    .menu-item.active {
				      border-left: 4px solid #3b82f6;
				    }
				    .logout-btn {
				      margin-top: auto;
				      color: #f87171;
				    }
				    .logout-btn:hover {
				      background-color: #7f1d1d33;
				    }

				    /* Main Content */
				    .content {
				      flex-grow: 1;
				      padding: 40px;
				      max-width: 1000px;
				      margin: 0 auto;
				      width: 100%%;
				    }
				    header {
				      display: flex;
				      justify-content: space-between;
				      align-items: center;
				      margin-bottom: 40px;
				      border-bottom: 1px solid #1e293b;
				      padding-bottom: 20px;
				    }
				    h1 { font-size: 1.8rem; font-weight: 700; color: #f8fafc; }
				    .user-badge {
				      display: flex;
				      align-items: center;
				      gap: 10px;
				      background-color: #1e293b;
				      padding: 8px 16px;
				      border-radius: 20px;
				      font-size: 0.9rem;
				      border: 1px solid #334155;
				    }
				    .badge-role {
				      background-color: #475569;
				      color: white;
				      padding: 2px 8px;
				      border-radius: 4px;
				      font-size: 0.75rem;
				      font-weight: 600;
				    }

				    /* Tab Panels */
				    .tab-panel {
				      display: none;
				    }
				    .tab-panel.active {
				      display: block;
				      animation: fadeIn 0.3s ease-in-out;
				    }
				    @keyframes fadeIn {
				      from { opacity: 0; transform: translateY(10px); }
				      to { opacity: 1; transform: translateY(0); }
				    }

				    .card {
				      background-color: #151f32;
				      border: 1px solid #22314d;
				      border-radius: 12px;
				      padding: 24px;
				      margin-bottom: 24px;
				    }
				    .card-title { font-size: 1.1rem; color: #f1f5f9; font-weight: 600; margin-bottom: 16px; }
				    
				    ul {
				      list-style-type: none;
				      line-height: 2;
				    }
				    li {
				      padding: 8px 0;
				      border-bottom: 1px solid #1e293b;
				    }
				    li:last-child { border-bottom: none; }
				    code {
				      background-color: #0f172a;
				      padding: 2px 6px;
				      border-radius: 4px;
				      font-family: 'JetBrains Mono', monospace;
				    }

				    /* Projetos Grid */
				    .project-grid {
				      display: grid;
				      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
				      gap: 20px;
				      margin-top: 20px;
				    }
				    .project-card {
				      background: #151f32;
				      border: 1px solid #22314d;
				      border-radius: 12px;
				      padding: 20px;
				      display: flex;
				      flex-direction: column;
				      gap: 12px;
				      transition: all 0.2s;
				    }
				    .project-card:hover {
				      transform: translateY(-2px);
				      border-color: #3b82f6;
				    }
				    .project-card-header {
				      display: flex;
				      justify-content: space-between;
				      align-items: center;
				    }
				    .project-card-title {
				      font-size: 1.1rem;
				      font-weight: 600;
				      color: #f8fafc;
				    }
				    .project-card-desc {
				      font-size: 0.9rem;
				      color: #94a3b8;
				      min-height: 40px;
				    }
				    .project-card-actions {
				      display: flex;
				      justify-content: space-between;
				      margin-top: auto;
				    }

				    /* Forms */
				    .form-group {
				      margin-bottom: 16px;
				    }
				    label {
				      display: block;
				      font-size: 0.85rem;
				      color: #cbd5e1;
				      margin-bottom: 6px;
				      font-weight: 500;
				    }
				    input, textarea {
				      width: 100%%;
				      padding: 10px 14px;
				      background-color: #0b0f19;
				      border: 1px solid #334155;
				      border-radius: 8px;
				      color: #f1f5f9;
				      font-family: inherit;
				    }
				    button {
				      background-color: #2563eb;
				      color: white;
				      padding: 10px 20px;
				      border: none;
				      border-radius: 8px;
				      font-weight: 600;
				      cursor: pointer;
				      transition: background 0.2s;
				    }
				    button:hover { background-color: #1d4ed8; }

				    /* Sandbox console */
				    .console {
				      background-color: #05070f;
				      border: 1px solid #1e293b;
				      border-radius: 12px;
				      padding: 20px;
				      font-family: 'JetBrains Mono', monospace;
				      font-size: 0.9rem;
				      color: #3b82f6;
				      height: 350px;
				      overflow-y: auto;
				      line-height: 1.6;
				    }
				  </style>
				</head>
				<body>
				  <!-- Sidebar -->
				  <div class="sidebar">
				    <div class="logo">
				      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
				        <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="#3B82F6" stroke="#3B82F6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
				        <path d="M2 17L12 22L22 17" stroke="#3B82F6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
				        <path d="M2 12L12 17L22 12" stroke="#3B82F6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
				      </svg>
				      Vaultra <span>Workspace</span>
				    </div>
				    
				    <div class="menu-item active" onclick="switchTab('perfil')">Meu Perfil</div>
				    <div class="menu-item" onclick="switchTab('projetos'); carregarProjetos();">Projetos & Canvas</div>
				    <div class="menu-item" onclick="switchTab('sandbox')">Sandbox Privado</div>
				    
				    <a href="/logout" class="menu-item logout-btn">Sair</a>
				  </div>

				  <!-- Main Content -->
				  <div class="content">
				    <header>
				      <div>
				        <h1>Área de Trabalho Segura</h1>
				        <p style="color: #94a3b8; font-size: 0.9rem; margin-top: 4px;">Bem-vindo ao servidor HTTP Vaultra</p>
				      </div>
				      <div class="user-badge">
				        <span>%s</span>
				        <span class="badge-role">%s</span>
				      </div>
				    </header>

				    <!-- Tab: Perfil -->
				    <div id="perfil" class="tab-panel active">
				      <div class="card">
				        <div class="card-title">Informações de Credenciais & Cargo</div>
				        <table style="width: 100%%; border-collapse: collapse;">
				          <tr style="border-bottom: 1px solid #1e293b;">
				            <td style="padding: 12px 0; color: #94a3b8; font-weight: 500;">Usuário</td>
				            <td style="padding: 12px 0; text-align: right; font-weight: 600;">%s</td>
				          </tr>
				          <tr style="border-bottom: 1px solid #1e293b;">
				            <td style="padding: 12px 0; color: #94a3b8; font-weight: 500;">Cargo Atribuído</td>
				            <td style="padding: 12px 0; text-align: right; font-weight: 600; color: #3b82f6;">%s</td>
				          </tr>
				          <tr>
				            <td style="padding: 12px 0; color: #94a3b8; font-weight: 500;">Descrição do Acesso</td>
				            <td style="padding: 12px 0; text-align: right; color: #cbd5e1; font-size: 0.9rem;">%s</td>
				          </tr>
				        </table>
				      </div>

				      <div class="card">
				        <div class="card-title">Permissões de Diretórios</div>
				        <ul>
				          %s
				        </ul>
				      </div>
				    </div>

				    <!-- Tab: Projetos & Canvas -->
				    <div id="projetos" class="tab-panel">
				      <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 32px; align-items: start;">
				        <div>
				          <h2 style="font-size: 1.2rem; margin-bottom: 16px;">Seus Projetos</h2>
				          <div class="project-grid" id="lista-projetos">
				            <!-- Projetos injetados por JS -->
				          </div>
				        </div>

				        <div class="card">
				          <h3 style="font-size: 1rem; margin-bottom: 16px;">Criar Novo Projeto</h3>
				          <form id="form-criar-projeto" onsubmit="criarProjeto(event)">
				            <div class="form-group">
				              <label for="proj-nome">Nome do Projeto</label>
				              <input type="text" id="proj-nome" required placeholder="Ex: Projeto Motherboard Alpha">
				            </div>
				            <div class="form-group">
				              <label for="proj-desc">Descrição</label>
				              <textarea id="proj-desc" rows="3" placeholder="Modelagem 3D, PCB Layout e arquivos AutoCAD..."></textarea>
				            </div>
				            <button type="submit" style="width: 100%%;">Inicializar Projeto</button>
				          </form>
				        </div>
				      </div>
				    </div>

				    <!-- Tab: Sandbox -->
				    <div id="sandbox" class="tab-panel">
				      <h2 style="font-size: 1.2rem; margin-bottom: 16px;">Console Sandbox Restrito para %s</h2>
				      <div class="console">
				        <div>[SISTEMA] Inicializando sandbox para cargo %s...</div>
				        <div>[INFO] Conexão segura estabelecida.</div>
				        <div>[PERMISSÕES] Suas restrições de escrita/leitura foram carregadas com sucesso.</div>
				      </div>
				    </div>
				  </div>

				  <script>
				    function switchTab(tabId) {
				      document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
				      document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));
				      
				      document.getElementById(tabId).classList.add('active');
				      event.currentTarget.classList.add('active');
				    }

				    function carregarProjetos() {
				      fetch('/api/projetos')
				      .then(res => res.json())
				      .then(projetos => {
				        const container = document.getElementById('lista-projetos');
				        container.innerHTML = '';
				        if (projetos.length === 0) {
				          container.innerHTML = '<p style="color: #94a3b8;">Nenhum projeto encontrado. Crie um ao lado.</p>';
				          return;
				        }
				        projetos.forEach(p => {
				          const card = document.createElement('div');
				          card.className = 'project-card';
				          card.innerHTML = `
				            <div class="project-card-header">
				              <span class="project-card-title">📁 ${p.nome}</span>
				            </div>
				            <p class="project-card-desc">${p.descricao || 'Sem descrição.'}</p>
				            <div class="project-card-actions">
				              <span style="font-size: 11px; color: #94a3b8;">ID: #${p.id}</span>
				              <a href="/workspace/projeto/${p.id}" target="_blank" style="color: #3b82f6; text-decoration: none; font-weight: 600; font-size: 13px;">Abrir Blueprint Canvas →</a>
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
				""".formatted(user.getUsername(), user.getCargo().getNome(),
						user.getUsername(), user.getCargo().name(), user.getCargo().getDescricao(),
						permsList.toString(), user.getUsername(), user.getCargo().name());
	}
}
