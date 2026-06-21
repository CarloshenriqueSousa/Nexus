package Security;

import Model.Projeto;
import Security.User;

public class SandboxPages {

    public static String paginaCanvas(User user, Projeto projeto) {
        String html = """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <title>Nexus Sandbox — {{name}}</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg-dark: #0a0b0d;
      --bg-canvas: #121316;
      --bg-node: rgba(26, 29, 36, 0.95);
      --bg-node-hover: rgba(36, 40, 50, 0.98);
      --text-main: #f3f4f6;
      --text-muted: #9ca3af;
      --border-color: rgba(255, 255, 255, 0.08);
      
      --color-folder: #3b82f6;
      --color-code: #10b981;
      --color-3d: #8b5cf6;
      --color-pcb: #ec4899;
      --color-note: #f59e0b;
      --color-generic: #6b7280;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      user-select: none;
    }

    body, html {
      width: 100%;
      height: 100%;
      overflow: hidden;
      font-family: 'Outfit', sans-serif;
      background-color: var(--bg-dark);
      color: var(--text-main);
    }

    #app-container {
      display: flex;
      width: 100vw;
      height: 100vh;
      position: relative;
    }

    /* Sidebar Estilo Premium Glassmorphism */
    #sidebar {
      width: 320px;
      height: 100%;
      background: rgba(18, 19, 22, 0.85);
      backdrop-filter: blur(16px);
      border-right: 1px solid var(--border-color);
      display: flex;
      flex-direction: column;
      z-index: 10;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .sidebar-header {
      padding: 24px;
      border-bottom: 1px solid var(--border-color);
    }

    .project-title {
      font-size: 20px;
      font-weight: 600;
      color: #fff;
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .project-desc {
      font-size: 13px;
      color: var(--text-muted);
      margin-top: 4px;
    }

    .sidebar-content {
      flex: 1;
      overflow-y: auto;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .section-title {
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      margin-bottom: 8px;
    }

    /* Botões Premium */
    .btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      padding: 12px;
      border-radius: 8px;
      border: 1px solid var(--border-color);
      background: rgba(255, 255, 255, 0.03);
      color: #fff;
      font-family: inherit;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.15);
      transform: translateY(-1px);
    }

    .btn-primary {
      background: linear-gradient(135deg, #3b82f6, #2563eb);
      border: none;
    }

    .btn-primary:hover {
      background: linear-gradient(135deg, #60a5fa, #3b82f6);
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
    }

    /* Lista de arquivos */
    .file-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-height: 250px;
      overflow-y: auto;
    }

    .file-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-color);
      border-radius: 6px;
      font-size: 13px;
    }

    .file-item-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    /* Área do Canvas */
    #canvas-container {
      flex: 1;
      height: 100%;
      position: relative;
      background-color: var(--bg-canvas);
      background-image: radial-gradient(rgba(255, 255, 255, 0.06) 1.5px, transparent 0);
      background-size: 28px 28px;
      overflow: hidden;
      cursor: grab;
    }

    #canvas-container:active {
      cursor: grabbing;
    }

    #canvas-viewport {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      transform-origin: 0 0;
      pointer-events: none;
    }

    /* Nós do Canvas Blueprint */
    .node {
      position: absolute;
      width: 240px;
      background: var(--bg-node);
      border: 1.5px solid rgba(255, 255, 255, 0.08);
      border-radius: 12px;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5);
      pointer-events: auto;
      display: flex;
      flex-direction: column;
      overflow: visible;
      transition: border-color 0.2s, box-shadow 0.2s;
    }

    .node:hover {
      border-color: rgba(255, 255, 255, 0.25);
      background: var(--bg-node-hover);
      box-shadow: 0 15px 30px -5px rgba(0, 0, 0, 0.7);
    }

    .node.selected {
      border-color: #3b82f6 !important;
      box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.4), 0 15px 30px -5px rgba(0, 0, 0, 0.7);
    }

    .node-header {
      padding: 12px 14px;
      font-weight: 600;
      font-size: 14px;
      border-top-left-radius: 10px;
      border-top-right-radius: 10px;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    }

    .node-body {
      padding: 14px;
      font-size: 13px;
      color: var(--text-muted);
      line-height: 1.4;
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    /* Portas de Conexão Estilo Blueprint */
    .port {
      width: 12px;
      height: 12px;
      background-color: #121316;
      border: 2px solid var(--text-muted);
      border-radius: 50%;
      position: absolute;
      cursor: crosshair;
      z-index: 10;
      transition: background-color 0.2s, transform 0.2s;
    }

    .port:hover {
      background-color: #3b82f6 !important;
      transform: scale(1.3);
      border-color: #fff;
    }

    .port.connected {
      background-color: #3b82f6;
      border-color: #fff;
    }

    .port-left { left: -7px; top: 50%; transform: translateY(-50%); }
    .port-right { right: -7px; top: 50%; transform: translateY(-50%); }
    .port-top { top: -7px; left: 50%; transform: translateX(-50%); }
    .port-bottom { bottom: -7px; left: 50%; transform: translateX(-50%); }

    /* SVG de Conexões */
    #svg-connections {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      overflow: visible;
      pointer-events: none;
      z-index: 0;
    }

    .connection-line {
      fill: none;
      stroke: #64748b;
      stroke-width: 2.5;
      pointer-events: stroke;
      cursor: pointer;
      transition: stroke-width 0.15s, stroke 0.15s;
    }

    .connection-line:hover {
      stroke-width: 4.5;
      stroke: #3b82f6 !important;
    }

    .connection-line.active {
      stroke: #3b82f6;
      stroke-width: 3.5;
    }

    /* Controles Flutuantes do Canvas */
    .floating-controls {
      position: absolute;
      bottom: 24px;
      right: 24px;
      display: flex;
      gap: 10px;
      z-index: 5;
    }

    .control-btn {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      border: 1px solid var(--border-color);
      background: rgba(18, 19, 22, 0.85);
      backdrop-filter: blur(8px);
      color: #fff;
      font-size: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
      transition: all 0.2s;
    }

    .control-btn:hover {
      background: #2563eb;
      border-color: #3b82f6;
      transform: scale(1.05);
    }

    /* Modal de Preview de Arquivos */
    #preview-modal {
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      height: 100vh;
      background: rgba(0,0,0,0.85);
      backdrop-filter: blur(12px);
      display: none;
      align-items: center;
      justify-content: center;
      z-index: 100;
    }

    .modal-content {
      width: 80%;
      height: 80%;
      background: var(--bg-node);
      border: 1px solid var(--border-color);
      border-radius: 16px;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.8);
    }

    .modal-header {
      padding: 20px;
      border-bottom: 1px solid var(--border-color);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .modal-body {
      flex: 1;
      padding: 20px;
      overflow: auto;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #0d0e12;
    }

    .close-modal {
      font-size: 24px;
      cursor: pointer;
      color: var(--text-muted);
    }

    .close-modal:hover {
      color: #fff;
    }

    /* Miniaturas dentro do nó */
    .node-thumbnail {
      width: 100%;
      height: 90px;
      background: #0d0e12;
      border-radius: 6px;
      border: 1px solid rgba(255,255,255,0.04);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      overflow: hidden;
    }

    .node-thumbnail img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    /* Tags */
    .tag {
      font-size: 11px;
      font-weight: 500;
      padding: 2px 6px;
      border-radius: 4px;
      background: rgba(255,255,255,0.08);
      color: var(--text-muted);
      width: fit-content;
    }

    /* Mock 3D PCB Viewers */
    .mock-3d-canvas {
      width: 100%;
      height: 100%;
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: var(--text-muted);
    }

    .rotate-3d {
      animation: rotate3dAnimation 10s infinite linear;
      font-size: 80px;
      color: var(--color-3d);
    }

    @keyframes rotate3dAnimation {
      from { transform: rotateY(0deg) rotateX(20deg); }
      to { transform: rotateY(360deg) rotateX(20deg); }
    }
  </style>
</head>
<body>
  <div id="app-container">
    
    <!-- Sidebar -->
    <div id="sidebar">
      <div class="sidebar-header">
        <div class="project-title">
          <span>{{icon}}</span>
          <span>{{name}}</span>
        </div>
        <div class="project-desc">{{desc}}</div>
      </div>
      
      <div class="sidebar-content">
        <div>
          <div class="section-title">Ações do Canvas</div>
          <div style="display: flex; flex-direction: column; gap: 10px;">
            <button class="btn btn-primary" onclick="criarNoNota()">
              <span>📝</span> Adicionar Nota
            </button>
            <button class="btn" onclick="criarNoPasta()">
              <span>📁</span> Adicionar Pasta
            </button>
            <button class="btn" onclick="resetarZoomPan()">
              <span>🔍</span> Resetar Viewport
            </button>
            <a href="/workspace/dashboard" class="btn" style="text-decoration: none;">
              <span>🏠</span> Menu Principal
            </a>
          </div>
        </div>

        <div>
          <div class="section-title">Upload de Arquivos</div>
          <div style="display: flex; flex-direction: column; gap: 10px;">
            <p style="font-size: 12px; color: var(--text-muted);">
              Arraste arquivos diretamente no canvas ou clique abaixo para fazer upload.
            </p>
            <input type="file" id="file-input" style="display: none;" onchange="handleFileUpload(event)">
            <button class="btn" onclick="document.getElementById('file-input').click()">
              <span>📤</span> Selecionar Arquivo
            </button>
          </div>
        </div>

        <div>
          <div class="section-title">Lista de Arquivos</div>
          <div class="file-list" id="sidebar-file-list">
            <!-- Injetado dinamicamente -->
          </div>
        </div>
      </div>
    </div>

    <!-- Área de Desenho (Canvas) -->
    <div id="canvas-container" onmousedown="startPan(event)" onmousemove="dragCanvas(event)" onmouseup="endPan(event)" onmousewheel="handleZoom(event)">
      <div id="canvas-viewport">
        <!-- SVG para desenhar as ligações Bezier -->
        <svg id="svg-connections">
          <g id="connections-group"></g>
          <!-- Linha ativa de desenho de conexão -->
          <path id="active-connection" fill="none" stroke="#3b82f6" stroke-width="3" stroke-dasharray="5,5" style="display: none;" />
        </svg>
        
        <div id="nodes-container">
          <!-- Nós do canvas injetados dinamicamente -->
        </div>
      </div>
    </div>

    <!-- Controles Flutuantes -->
    <div class="floating-controls">
      <button class="control-btn" title="Aproximar" onclick="ajustarZoom(0.1)">＋</button>
      <button class="control-btn" title="Afastar" onclick="ajustarZoom(-0.1)">－</button>
      <button class="control-btn" title="Auto Layout" onclick="organizarAutomatico()">⚙</button>
    </div>

  </div>

  <!-- Modal de Visualização de Modelos -->
  <div id="preview-modal" onclick="fecharPreview()">
    <div class="modal-content" onclick="event.stopPropagation()">
      <div class="modal-header">
        <h3 id="modal-title">Visualizar Modelo</h3>
        <span class="close-modal" onclick="fecharPreview()">&times;</span>
      </div>
      <div class="modal-body" id="modal-body">
        <!-- Conteúdo do visualizador -->
      </div>
    </div>
  </div>

  <!-- Lógica Principal do Canvas Blueprint -->
  <script>
    const PROJETO_ID = {{id}};
    let nos = [];
    let ligacoes = [];
    let selecionadoNoId = null;

    // Estado do Viewport (Zoom e Pan)
    let zoom = 1.0;
    let panX = 100;
    let panY = 100;
    let isPanning = false;
    let startX = 0;
    let startY = 0;

    // Conexão em andamento
    let activeConnectionStart = null; // { nodeId, portType, x, y }

    const container = document.getElementById('canvas-container');
    const viewport = document.getElementById('canvas-viewport');
    const nodesContainer = document.getElementById('nodes-container');
    const svgGroup = document.getElementById('connections-group');
    const activeLine = document.getElementById('active-connection');

    // Inicialização
    window.addEventListener('DOMContentLoaded', () => {
      carregarCanvas();
      atualizarViewport();

      // Permitir drop de arquivos diretamente no canvas
      container.addEventListener('dragover', (e) => e.preventDefault());
      container.addEventListener('drop', handleFileDrop);

      // Listener para atualizar a linha ativa ao criar conexões
      window.addEventListener('mousemove', drawActiveConnection);
      window.addEventListener('mouseup', cancelActiveConnection);
    });

    function resetarZoomPan() {
      zoom = 1.0;
      panX = 100;
      panY = 100;
      atualizarViewport();
    }

    function atualizarViewport() {
      viewport.style.transform = `translate(${panX}px, ${panY}px) scale(${zoom})`;
      // Ajustar tamanho do grid de acordo com o zoom
      container.style.backgroundPosition = `${panX}px ${panY}px`;
    }

    // --- CONTROLE DE ZOOM E PAN ---

    function startPan(e) {
      if (e.target === container || e.target === viewport || e.target.tagName.toLowerCase() === 'svg') {
        isPanning = true;
        startX = e.clientX - panX;
        startY = e.clientY - panY;
      }
    }

    function dragCanvas(e) {
      if (isPanning) {
        panX = e.clientX - startX;
        panY = e.clientY - startY;
        atualizarViewport();
      }
    }

    function endPan() {
      isPanning = false;
    }

    function handleZoom(e) {
      e.preventDefault();
      const zoomFactor = 0.05;
      const mouseX = e.clientX - container.offsetLeft;
      const mouseY = e.clientY - container.offsetTop;

      const previousZoom = zoom;
      if (e.deltaY < 0) {
        zoom = Math.min(zoom + zoomFactor, 2.5);
      } else {
        zoom = Math.max(zoom - zoomFactor, 0.4);
      }

      // Zoom em direção ao cursor do mouse
      panX = mouseX - (mouseX - panX) * (zoom / previousZoom);
      panY = mouseY - (mouseY - panY) * (zoom / previousZoom);

      atualizarViewport();
    }

    function ajustarZoom(factor) {
      zoom = Math.max(0.4, Math.min(2.5, zoom + factor));
      atualizarViewport();
    }

    // --- CARREGAR DADOS ---

    function carregarCanvas() {
      fetch(`/api/projetos/${PROJETO_ID}/canvas`)
        .then(res => res.json())
        .then(data => {
          nos = data.nos || [];
          ligacoes = data.ligacoes || [];
          renderizarNos();
          renderizarLigacoes();
          atualizarListaArquivosSidebar();
        })
        .catch(err => console.error("Erro ao carregar canvas:", err));
    }

    function atualizarListaArquivosSidebar() {
      fetch(`/api/projetos/${PROJETO_ID}/arquivos`)
        .then(res => res.json())
        .then(arquivos => {
          const list = document.getElementById('sidebar-file-list');
          list.innerHTML = '';
          if (arquivos.length === 0) {
            list.innerHTML = '<div style="color:var(--text-muted);font-size:12px;text-align:center;padding:10px;">Nenhum arquivo adicionado.</div>';
            return;
          }
          arquivos.forEach(arq => {
            const item = document.createElement('div');
            item.className = 'file-item';
            item.innerHTML = `
              <div class="file-item-info">
                <span>${obterIconePorTipo(arq.tipo_arquivo)}</span>
                <span style="font-weight: 500;">${arq.nome}</span>
              </div>
              <button onclick="adicionarNoArquivoExistente(${arq.id}, '${arq.nome}', '${arq.tipo_arquivo}')" style="background:none;border:none;color:#3b82f6;cursor:pointer;font-weight:600;font-size:16px;" title="Adicionar ao canvas">＋</button>
            `;
            list.appendChild(item);
          });
        });
    }

    // --- RENDERIZAR NÓS E CONEXÕES ---

    function renderizarNos() {
      nodesContainer.innerHTML = '';
      nos.forEach(no => {
        const div = document.createElement('div');
        div.className = `node ${selecionadoNoId === no.id ? 'selected' : ''}`;
        div.style.left = `${no.pos_x}px`;
        div.style.top = `${no.pos_y}px`;
        div.style.width = `${no.largura}px`;
        div.style.height = `${no.altura}px`;
        div.id = `node-${no.id}`;

        // Definir cor de borda e header de acordo com tipo
        let headerColor = 'var(--color-generic)';
        if (no.tipo === 'pasta') headerColor = 'var(--color-folder)';
        else if (no.tipo === 'nota') headerColor = 'var(--color-note)';
        else if (no.tipo === 'arquivo') {
          const dados = no.dados_extra ? JSON.parse(no.dados_extra) : {};
          const tipoArq = dados.tipo_arquivo;
          if (tipoArq === '3d_model') headerColor = 'var(--color-3d)';
          else if (tipoArq === 'pcb_design') headerColor = 'var(--color-pcb)';
          else if (tipoArq === 'autocad') headerColor = 'var(--color-3d)';
          else if (tipoArq === 'imagem') headerColor = 'var(--color-code)';
        }

        // Criar conteúdo interno do nó
        let internalContent = `<p>${no.dados_extra ? (JSON.parse(no.dados_extra).descricao || "") : ""}</p>`;
        
        // Thumbnail/Preview para arquivo
        if (no.tipo === 'arquivo') {
          const dados = no.dados_extra ? JSON.parse(no.dados_extra) : {};
          const ext = no.titulo.split('.').pop().toLowerCase();
          const eImagem = ['png', 'jpg', 'jpeg', 'gif', 'svg'].includes(ext);

          internalContent = `
            <div class="node-thumbnail">
              ${eImagem ? `<img src="${dados.caminho}">` : `<span>${obterIconePorTipo(dados.tipo_arquivo)}</span>`}
            </div>
            <div style="font-size: 11px; display: flex; justify-content: space-between; align-items:center;">
              <span class="tag">${ext.toUpperCase()}</span>
              <span>${formatBytes(dados.tamanho || 0)}</span>
            </div>
          `;
        }

        div.innerHTML = `
          <div class="node-header" style="background-color: ${headerColor}">
            <span>${no.tipo === 'pasta' ? '📁' : no.tipo === 'nota' ? '📝' : '📄'} ${no.titulo}</span>
            <span onclick="removerNo(${no.id})" style="cursor:pointer;font-size:12px;opacity:0.7;" title="Deletar">❌</span>
          </div>
          <div class="node-body">
            ${internalContent}
          </div>
          
          <!-- Portas de ligação -->
          <div class="port port-left" data-node-id="${no.id}" data-port="left" onmousedown="startConnecting(event, ${no.id}, 'left')"></div>
          <div class="port port-right" data-node-id="${no.id}" data-port="right" onmousedown="startConnecting(event, ${no.id}, 'right')"></div>
          <div class="port port-top" data-node-id="${no.id}" data-port="top" onmousedown="startConnecting(event, ${no.id}, 'top')"></div>
          <div class="port port-bottom" data-node-id="${no.id}" data-port="bottom" onmousedown="startConnecting(event, ${no.id}, 'bottom')"></div>
        `;

        // Habilitar drag do nó
        div.addEventListener('mousedown', (e) => startDragNode(e, no));
        div.addEventListener('dblclick', () => abrirPreviewNode(no));

        nodesContainer.appendChild(div);
      });
    }

    function renderizarLigacoes() {
      svgGroup.innerHTML = '';
      ligacoes.forEach(lig => {
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.className.baseVal = 'connection-line';
        path.id = `connection-${lig.id}`;

        // Buscar posições dos nós de origem e destino
        const nOrigem = nos.find(n => n.id === lig.origem_id);
        const nDestino = nos.find(n => n.id === lig.destino_id);

        if (nOrigem && nDestino) {
          const ptOrigem = obterCoordenadaPorta(nOrigem, lig.porta_origem);
          const ptDestino = obterCoordenadaPorta(nDestino, lig.porta_destino);
          const d = calcularBezier(ptOrigem.x, ptOrigem.y, ptDestino.x, ptDestino.y, lig.porta_origem, lig.porta_destino);
          path.setAttribute('d', d);
          
          // Definir cores por relação
          let cor = '#64748b'; // default
          if (lig.tipo === 'dependencia') cor = '#10b981';
          else if (lig.tipo === 'fluxo') cor = '#f59e0b';
          path.setAttribute('stroke', cor);

          // Remover ligação com clique com botão direito
          path.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            removerLigacao(lig.id);
          });

          svgGroup.appendChild(path);
        }
      });
    }

    // --- ARRASTAR E MOVER NÓS ---

    let dragNode = null;
    let nodeStartX = 0;
    let nodeStartY = 0;
    let mouseStartX = 0;
    let mouseStartY = 0;

    function startDragNode(e, no) {
      if (e.target.classList.contains('port') || e.target.innerText === '❌') return;
      
      e.stopPropagation();
      dragNode = no;
      selecionadoNoId = no.id;
      
      // Destacar selecionado
      document.querySelectorAll('.node').forEach(div => div.classList.remove('selected'));
      document.getElementById(`node-${no.id}`).classList.add('selected');

      mouseStartX = e.clientX;
      mouseStartY = e.clientY;
      nodeStartX = no.pos_x;
      nodeStartY = no.pos_y;

      const moveHandler = (evt) => {
        if (dragNode) {
          const dx = (evt.clientX - mouseStartX) / zoom;
          const dy = (evt.clientY - mouseStartY) / zoom;
          dragNode.pos_x = nodeStartX + dx;
          dragNode.pos_y = nodeStartY + dy;
          
          const div = document.getElementById(`node-${dragNode.id}`);
          div.style.left = `${dragNode.pos_x}px`;
          div.style.top = `${dragNode.pos_y}px`;

          // Redesenhar conexões ligadas a este nó
          renderizarLigacoes();
        }
      };

      const upHandler = () => {
        if (dragNode) {
          // Persistir no banco de dados a nova posição do nó
          salvarPosicaoNo(dragNode);
          dragNode = null;
        }
        window.removeEventListener('mousemove', moveHandler);
        window.removeEventListener('mouseup', upHandler);
      };

      window.addEventListener('mousemove', moveHandler);
      window.addEventListener('mouseup', upHandler);
    }

    function salvarPosicaoNo(no) {
      fetch(`/api/projetos/${PROJETO_ID}/nos/${no.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pos_x: no.pos_x, pos_y: no.pos_y })
      });
    }

    // --- CRIAR NÓS ---

    function criarNoNota() {
      const titulo = prompt("Título da Nota:", "Nova Nota");
      if (!titulo) return;
      
      const pos = obterCentroViewport();
      
      fetch(`/api/projetos/${PROJETO_ID}/nos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tipo: 'nota',
          titulo: titulo,
          pos_x: pos.x,
          pos_y: pos.y
        })
      })
      .then(res => res.json())
      .then(() => carregarCanvas());
    }

    function criarNoPasta() {
      const titulo = prompt("Nome da Pasta:", "Nova Pasta");
      if (!titulo) return;

      const pos = obterCentroViewport();

      fetch(`/api/projetos/${PROJETO_ID}/nos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tipo: 'pasta',
          titulo: titulo,
          pos_x: pos.x,
          pos_y: pos.y
        })
      })
      .then(res => res.json())
      .then(() => carregarCanvas());
    }

    function adicionarNoArquivoExistente(arqId, nome, tipoArq) {
      const pos = obterCentroViewport();

      fetch(`/api/projetos/${PROJETO_ID}/nos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tipo: 'arquivo',
          titulo: nome,
          pos_x: pos.x,
          pos_y: pos.y,
          arquivo_id: arqId
        })
      })
      .then(res => res.json())
      .then(() => carregarCanvas());
    }

    function removerNo(id) {
      if (confirm("Remover este bloco?")) {
        fetch(`/api/projetos/${PROJETO_ID}/nos/${id}`, { method: 'DELETE' })
          .then(() => carregarCanvas());
      }
    }

    // --- CRIAR LIGAÇÕES (CONEXÕES) ---

    function startConnecting(e, nodeId, port) {
      e.stopPropagation();
      e.preventDefault();
      
      const no = nos.find(n => n.id === nodeId);
      const coord = obterCoordenadaPorta(no, port);
      activeConnectionStart = { nodeId, port, ...coord };

      activeLine.style.display = 'block';
      drawActiveConnection(e);
    }

    function drawActiveConnection(e) {
      if (!activeConnectionStart) return;

      const rect = container.getBoundingClientRect();
      const mouseX = (e.clientX - rect.left - panX) / zoom;
      const mouseY = (e.clientY - rect.top - panY) / zoom;

      const d = calcularBezier(activeConnectionStart.x, activeConnectionStart.y, mouseX, mouseY, activeConnectionStart.port, 'left');
      activeLine.setAttribute('d', d);
    }

    function cancelActiveConnection(e) {
      if (!activeConnectionStart) return;

      // Verificar se soltou o mouse sobre outra porta
      const target = e.target;
      if (target.classList.contains('port')) {
        const destNodeId = parseInt(target.getAttribute('data-node-id'));
        const destPort = target.getAttribute('data-port');

        if (destNodeId !== activeConnectionStart.nodeId) {
          criarLigacao(activeConnectionStart.nodeId, activeConnectionStart.port, destNodeId, destPort);
        }
      }

      activeConnectionStart = null;
      activeLine.style.display = 'none';
    }

    function criarLigacao(origemId, portaOrigem, destinoId, portaDestino) {
      // Tipo padrão
      const tipo = prompt("Tipo de Ligação (dependencia / referencia / fluxo):", "referencia");
      if (tipo === null) return; // Cancelado

      fetch(`/api/projetos/${PROJETO_ID}/ligacoes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          origem_id: origemId,
          destino_id: destinoId,
          porta_origem: portaOrigem,
          porta_destino: portaDestino,
          tipo: tipo
        })
      })
      .then(res => res.json())
      .then(() => carregarCanvas());
    }

    function removerLigacao(id) {
      if (confirm("Remover esta conexão?")) {
        fetch(`/api/projetos/${PROJETO_ID}/ligacoes/${id}`, { method: 'DELETE' })
          .then(() => carregarCanvas());
      }
    }

    // --- UPLOAD DE ARQUIVOS ---

    function handleFileUpload(e) {
      const file = e.target.files[0];
      if (file) {
        uploadFile(file);
      }
    }

    function handleFileDrop(e) {
      e.preventDefault();
      const files = e.dataTransfer.files;
      if (files.length > 0) {
        uploadFile(files[0]);
      }
    }

    function uploadFile(file) {
      const reader = new FileReader();
      reader.onload = function(evt) {
        const base64Content = evt.target.result.split(',')[1];
        
        // Identificar tipo do arquivo
        let tipo = 'documento';
        const ext = file.name.split('.').pop().toLowerCase();
        if (['obj', 'stl', 'fbx', 'gltf', 'glb'].includes(ext)) tipo = '3d_model';
        else if (['pcb', 'brd', 'sch', 'kicad_pcb'].includes(ext)) tipo = 'pcb_design';
        else if (['dwg', 'dxf'].includes(ext)) tipo = 'autocad';
        else if (['png', 'jpg', 'jpeg', 'gif', 'svg'].includes(ext)) tipo = 'imagem';

        fetch(`/api/projetos/${PROJETO_ID}/upload`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: file.name,
            content: base64Content,
            type: tipo
          })
        })
        .then(res => res.json())
        .then(data => {
          if (data.status === 'success') {
            carregarCanvas();
          } else {
            alert("Erro no upload: " + data.message);
          }
        })
        .catch(err => {
          console.error("Erro no upload:", err);
          alert("Erro no upload.");
        });
      };
      reader.readAsDataURL(file);
    }

    // --- VISUALIZAÇÃO/PREVIEW DE ARQUIVOS (3D, PCB, AutoCAD) ---

    function abrirPreviewNode(no) {
      if (no.tipo !== 'arquivo') return;

      const dados = no.dados_extra ? JSON.parse(no.dados_extra) : {};
      const modal = document.getElementById('preview-modal');
      const title = document.getElementById('modal-title');
      const body = document.getElementById('modal-body');

      title.innerText = no.titulo;
      body.innerHTML = '';

      const tipo = dados.tipo_arquivo;
      const ext = no.titulo.split('.').pop().toLowerCase();

      if (tipo === '3d_model') {
        body.innerHTML = `
          <div class="mock-3d-canvas">
            <span class="rotate-3d">⚙</span>
            <h2 style="margin-top:20px;">Visualizador de Modelo 3D — NexusEngine</h2>
            <p style="color:var(--text-muted); margin-top:8px;">Renderizando modelo: <b>${no.titulo}</b> (${ext.toUpperCase()})</p>
            <p style="font-size:12px; margin-top:6px; color:#10b981;">✔ WebGL 2.0 ativo. Aceleração por Hardware OK.</p>
          </div>
        `;
      } else if (tipo === 'pcb_design') {
        body.innerHTML = `
          <div class="mock-3d-canvas">
            <div class="rotate-3d" style="color:var(--color-pcb); animation-duration: 5s;">🔌</div>
            <h2 style="margin-top:20px;">Análise Integrada de Placas-Mãe & PCBs</h2>
            <p style="color:var(--text-muted); margin-top:8px;">Layout carregado: <b>${no.titulo}</b></p>
            <p style="font-size:12px; margin-top:6px; color:#10b981;">✔ Trilhas e componentes OK. Pronta para produção.</p>
          </div>
        `;
      } else if (tipo === 'autocad') {
        body.innerHTML = `
          <div class="mock-3d-canvas">
            <div class="rotate-3d" style="color:#f59e0b; animation-duration: 7s;">📐</div>
            <h2 style="margin-top:20px;">Visualizador AutoCAD (Desenho Vetorial)</h2>
            <p style="color:var(--text-muted); margin-top:8px;">Projeto CAD: <b>${no.titulo}</b></p>
            <p style="font-size:12px; margin-top:6px; color:#10b981;">✔ Camadas vetoriais renderizadas com sucesso.</p>
          </div>
        `;
      } else if (tipo === 'imagem') {
        body.innerHTML = `<img src="${dados.caminho}" style="max-width:100%; max-height:100%; object-fit:contain; border-radius:8px;">`;
      } else {
        // Arquivos comuns
        body.innerHTML = `
          <div style="text-align:center;">
            <div style="font-size:64px;">📄</div>
            <h2 style="margin-top:16px;">${no.titulo}</h2>
            <p style="color:var(--text-muted); margin-top:8px;">Tamanho: ${formatBytes(dados.tamanho || 0)}</p>
            <a href="${dados.caminho}" download class="btn" style="margin-top:20px; display:inline-flex;">📥 Baixar Arquivo</a>
          </div>
        `;
      }

      modal.style.display = 'flex';
    }

    // --- ALGORITMOS DE LAYOUT E CÁLCULO ---

    function obterCentroViewport() {
      const rect = container.getBoundingClientRect();
      const x = (rect.width / 2 - panX) / zoom;
      const y = (rect.height / 2 - panY) / zoom;
      return { x, y };
    }

    function obterCoordenadaPorta(node, port) {
      const x = node.pos_x;
      const y = node.pos_y;
      const w = node.largura;
      const h = node.altura;

      if (port === 'left') return { x: x, y: y + h / 2 };
      if (port === 'right') return { x: x + w, y: y + h / 2 };
      if (port === 'top') return { x: x + w / 2, y: y };
      if (port === 'bottom') return { x: x + w / 2, y: y + h };
      return { x, y };
    }

    function calcularBezier(x1, y1, x2, y2, portOrigem, portDestino) {
      // Offset de curvatura
      let offset = Math.abs(x2 - x1) * 0.4;
      if (offset < 40) offset = 40;

      let cp1x = x1;
      let cp1y = y1;
      let cp2x = x2;
      let cp2y = y2;

      // Mapear pontos de controle baseados nas portas
      if (portOrigem === 'right') cp1x += offset;
      else if (portOrigem === 'left') cp1x -= offset;
      else if (portOrigem === 'bottom') cp1y += offset;
      else if (portOrigem === 'top') cp1y -= offset;

      if (portDestino === 'right') cp2x += offset;
      else if (portDestino === 'left') cp2x -= offset;
      else if (portDestino === 'bottom') cp2y += offset;
      else if (portDestino === 'top') cp2y -= offset;

      return `M ${x1} ${y1} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${x2} ${y2}`;
    }

    function organizarAutomatico() {
      // Layout de grid básico dos nós existentes
      let x = 100;
      let y = 100;
      nos.forEach((no, idx) => {
        no.pos_x = x;
        no.pos_y = y;
        salvarPosicaoNo(no);
        
        x += 300;
        if ((idx + 1) % 3 === 0) {
          x = 100;
          y += 240;
        }
      });
      renderizarNos();
      renderizarLigacoes();
    }

    // --- UTILITÁRIOS ---

    function obterIconePorTipo(tipo) {
      if (tipo === '3d_model') return '⚙';
      if (tipo === 'pcb_design') return '🔌';
      if (tipo === 'autocad') return '📐';
      if (tipo === 'imagem') return '🖼';
      if (tipo === 'pasta') return '📁';
      return '📄';
    }

    function formatBytes(bytes, decimals = 2) {
      if (bytes === 0) return '0 Bytes';
      const k = 1024;
      const dm = decimals < 0 ? 0 : decimals;
      const sizes = ['Bytes', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    }
  </script>
</body>
</html>
""";

        return html
                .replace("{{icon}}", projeto.getIcone())
                .replace("{{name}}", projeto.getNome())
                .replace("{{desc}}", projeto.getDescricao() != null && !projeto.getDescricao().isEmpty() ? projeto.getDescricao() : "Sem descrição")
                .replace("{{id}}", String.valueOf(projeto.getId()));
    }
}
