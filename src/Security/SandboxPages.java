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
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Nexus Sandbox — {{name}}</title>
  <meta name="description" content="Nexus Sandbox — Plataforma profissional de gestão de projetos técnicos">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="/css/vaultra.css">
  <link rel="stylesheet" href="/css/canvas.css">
</head>
<body>
  <div id="app-container">

    <!-- ==================== SIDEBAR ==================== -->
    <div id="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="#5b8def" stroke="#5b8def" stroke-width="1.5"/>
            <path d="M2 17L12 22L22 17" stroke="#5b8def" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M2 12L12 17L22 12" stroke="#5b8def" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <span class="sidebar-logo-text"><span>Nexus</span> Sandbox</span>
        </div>
        <div class="project-title">{{name}}</div>
        <div class="project-desc">{{desc}}</div>
      </div>

      <div class="breadcrumbs" id="breadcrumbs">
        <span class="breadcrumb-item active" data-id="null" onclick="navegarPasta(null)">Raiz</span>
      </div>

      <div class="sidebar-search">
        <div class="search-input-wrapper">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          <input class="search-input" type="text" placeholder="Buscar arquivos..." id="search-input" oninput="filtrarArquivos(this.value)">
        </div>
      </div>

      <div class="tree-actions">
        <button class="tree-action-btn primary" onclick="criarNovaPasta()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>
          Nova Pasta
        </button>
        <button class="tree-action-btn" onclick="document.getElementById('file-input').click()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          Upload
        </button>
        <input type="file" id="file-input" multiple style="display:none;" onchange="handleFileUpload(event)">
      </div>

      <div class="sidebar-tree" id="file-tree">
        <div class="tree-empty">Carregando...</div>
      </div>

      <div class="sidebar-footer">
        <a href="/workspace/dashboard" class="sidebar-nav-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          Menu Principal
        </a>
      </div>
    </div>

    <!-- ==================== CANVAS ==================== -->
    <div id="canvas-container" onmousedown="startPan(event)" onmousemove="dragCanvas(event)" onmouseup="endPan(event)" onwheel="handleZoom(event)">
      <!-- Floating Toolbar -->
      <div class="toolbar" id="toolbar">
        <button class="toolbar-btn" onclick="criarNoNota()" title="Adicionar Nota">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          Nota
        </button>
        <button class="toolbar-btn" onclick="criarNoPasta()" title="Adicionar Pasta no Canvas">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
          Pasta
        </button>
        <div class="toolbar-sep"></div>
        <button class="toolbar-btn" onclick="ajustarZoom(0.15)" title="Zoom In">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/><path d="M11 8v6M8 11h6"/></svg>
        </button>
        <span class="toolbar-zoom" id="zoom-display">100%</span>
        <button class="toolbar-btn" onclick="ajustarZoom(-0.15)" title="Zoom Out">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/><path d="M8 11h6"/></svg>
        </button>
        <div class="toolbar-sep"></div>
        <button class="toolbar-btn" onclick="resetarZoomPan()" title="Resetar Viewport">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"/></svg>
        </button>
        <button class="toolbar-btn" onclick="organizarAutomatico()" title="Auto Layout">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
        </button>
      </div>

      <!-- Drop overlay for file drag -->
      <div class="drop-overlay" id="drop-overlay">
        <div class="drop-overlay-text">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align:middle;margin-right:8px;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          Solte para enviar arquivos
        </div>
      </div>

      <div id="canvas-viewport">
        <svg id="svg-connections">
          <g id="connections-group"></g>
          <path id="active-connection" fill="none" stroke="#5b8def" stroke-width="2.5" stroke-dasharray="6,4" style="display:none;" />
        </svg>
        <div id="nodes-container"></div>
      </div>

      <!-- Minimap -->
      <div class="minimap" id="minimap">
        <div class="minimap-viewport" id="minimap-viewport"></div>
      </div>

      <!-- Upload progress -->
      <div class="upload-progress" id="upload-progress">
        <div class="upload-progress-title">Enviando arquivos...</div>
        <div class="upload-progress-bar"><div class="upload-progress-fill" id="upload-progress-fill"></div></div>
        <div class="upload-progress-text" id="upload-progress-text">0 / 0</div>
      </div>
    </div>
  </div>

  <!-- ==================== CONTEXT MENU ==================== -->
  <div class="context-menu" id="context-menu">
    <div class="context-menu-item" onclick="editarNomeNo()">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
      Editar Nome
    </div>
    <div class="context-menu-item" onclick="moverNoParaPasta()">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
      Mover para Pasta
    </div>
    <div class="context-menu-sep"></div>
    <div class="context-menu-item danger" onclick="removerNoContexto()">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
      Deletar
    </div>
  </div>

  <!-- ==================== PREVIEW MODAL ==================== -->
  <div id="preview-modal" onclick="fecharPreview()">
    <div class="modal-content" onclick="event.stopPropagation()">
      <div class="modal-header">
        <div class="modal-header-info">
          <span id="modal-icon"></span>
          <h3 id="modal-title">Visualizar</h3>
        </div>
        <button class="modal-close" onclick="fecharPreview()">&times;</button>
      </div>
      <div class="modal-body" id="modal-body"></div>
      <div class="modal-footer">
        <div class="modal-footer-info" id="modal-footer-info"></div>
        <a class="modal-download-btn" id="modal-download" href="#" download>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          Download
        </a>
      </div>
    </div>
  </div>

  <!-- ==================== TOAST CONTAINER ==================== -->
  <div class="toast-container" id="toast-container"></div>

  <!-- ==================== Three.js CDN ==================== -->
  <script type="importmap">
  {
    "imports": {
      "three": "https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.min.js",
      "three/addons/": "https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/"
    }
  }
  </script>

  <!-- ==================== MAIN SCRIPT ==================== -->
  <script type="module">
    import * as THREE from 'three';
    import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
    import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
    import { STLLoader } from 'three/addons/loaders/STLLoader.js';
    import { OBJLoader } from 'three/addons/loaders/OBJLoader.js';

    // Expose Three.js to global scope for viewer functions
    window.THREE = THREE;
    window.OrbitControls = OrbitControls;
    window.GLTFLoader = GLTFLoader;
    window.STLLoader = STLLoader;
    window.OBJLoader = OBJLoader;
  </script>

  <script>
    // ==================== STATE ====================
    const PROJETO_ID = {{id}};
    let nos = [];
    let ligacoes = [];
    let arquivosArvore = [];
    let pastaAtualId = null;
    let caminhoAtual = []; // [{id, nome}]
    let selecionadoNoId = null;
    let contextMenuNoId = null;
    let searchTerm = '';

    // Viewport state
    let zoom = 1.0;
    let panX = 100, panY = 100;
    let isPanning = false;
    let startX = 0, startY = 0;

    // Connection state
    let activeConnectionStart = null;

    // Node drag state
    let dragNode = null;
    let nodeStartX = 0, nodeStartY = 0;
    let mouseStartX = 0, mouseStartY = 0;

    // Elements
    const container = document.getElementById('canvas-container');
    const viewport = document.getElementById('canvas-viewport');
    const nodesContainer = document.getElementById('nodes-container');
    const svgGroup = document.getElementById('connections-group');
    const activeLine = document.getElementById('active-connection');

    // ==================== SVG ICONS ====================
    const ICONS = {
      folder: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>',
      file: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>',
      cube3d: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>',
      pcb: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="4" width="16" height="16" rx="2"/><path d="M9 1v3M15 1v3M9 20v3M15 20v3M1 9h3M1 15h3M20 9h3M20 15h3"/><circle cx="9" cy="9" r="1"/><circle cx="15" cy="15" r="1"/></svg>',
      autocad: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 20h20"/><path d="M5 20V8l7-5 7 5v12"/><path d="M10 20v-6h4v6"/></svg>',
      image: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>',
      note: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>',
      chevron: '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>',
      delete: '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>'
    };

    function getIconByType(tipo) {
      switch(tipo) {
        case '3d_model': return ICONS.cube3d;
        case 'pcb_design': return ICONS.pcb;
        case 'autocad': return ICONS.autocad;
        case 'imagem': return ICONS.image;
        case 'pasta': return ICONS.folder;
        default: return ICONS.file;
      }
    }

    function getColorByType(tipo) {
      switch(tipo) {
        case '3d_model': return 'var(--color-3d)';
        case 'pcb_design': return 'var(--color-pcb)';
        case 'autocad': return 'var(--color-autocad)';
        case 'imagem': return 'var(--color-image)';
        case 'pasta': return 'var(--color-folder)';
        case 'nota': return 'var(--color-note)';
        default: return 'var(--color-doc)';
      }
    }

    // ==================== TOAST SYSTEM ====================
    function showToast(message, type = 'info') {
      const container = document.getElementById('toast-container');
      const toast = document.createElement('div');
      toast.className = `toast ${type}`;
      const iconSymbol = type === 'success' ? '✓' : type === 'error' ? '!' : type === 'warning' ? '⚠' : 'i';
      toast.innerHTML = `
        <div class="toast-icon">${iconSymbol}</div>
        <span class="toast-message">${message}</span>
        <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
      `;
      container.appendChild(toast);
      setTimeout(() => {
        toast.classList.add('removing');
        setTimeout(() => toast.remove(), 250);
      }, 4000);
    }

    // ==================== INITIALIZATION ====================
    window.addEventListener('DOMContentLoaded', () => {
      carregarCanvas();
      carregarArvore();
      atualizarViewport();

      // File drop on canvas
      container.addEventListener('dragover', (e) => {
        e.preventDefault();
        document.getElementById('drop-overlay').classList.add('visible');
      });
      container.addEventListener('dragleave', (e) => {
        if (!container.contains(e.relatedTarget)) {
          document.getElementById('drop-overlay').classList.remove('visible');
        }
      });
      container.addEventListener('drop', handleFileDrop);

      // Connection drawing
      window.addEventListener('mousemove', drawActiveConnection);
      window.addEventListener('mouseup', cancelActiveConnection);

      // Close context menu on click
      window.addEventListener('click', () => {
        document.getElementById('context-menu').classList.remove('visible');
      });

      // Keyboard shortcuts
      window.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
          fecharPreview();
          document.getElementById('context-menu').classList.remove('visible');
        }
      });
    });

    // ==================== VIEWPORT ====================
    function resetarZoomPan() {
      zoom = 1.0; panX = 100; panY = 100;
      atualizarViewport();
    }

    function atualizarViewport() {
      viewport.style.transform = `translate(${panX}px, ${panY}px) scale(${zoom})`;
      container.style.backgroundPosition = `${panX}px ${panY}px`;
      document.getElementById('zoom-display').textContent = Math.round(zoom * 100) + '%';
      atualizarMinimap();
    }

    function startPan(e) {
      if (e.target === container || e.target === viewport || e.target.tagName === 'svg' || e.target.tagName === 'SVG') {
        isPanning = true;
        startX = e.clientX - panX;
        startY = e.clientY - panY;
        // Deselect
        selecionadoNoId = null;
        document.querySelectorAll('.node').forEach(d => d.classList.remove('selected'));
      }
    }

    function dragCanvas(e) {
      if (isPanning) {
        panX = e.clientX - startX;
        panY = e.clientY - startY;
        atualizarViewport();
      }
    }

    function endPan() { isPanning = false; }

    function handleZoom(e) {
      e.preventDefault();
      const factor = 0.08;
      const rect = container.getBoundingClientRect();
      const mouseX = e.clientX - rect.left;
      const mouseY = e.clientY - rect.top;
      const prevZoom = zoom;

      zoom = e.deltaY < 0
        ? Math.min(zoom + factor, 3)
        : Math.max(zoom - factor, 0.3);

      panX = mouseX - (mouseX - panX) * (zoom / prevZoom);
      panY = mouseY - (mouseY - panY) * (zoom / prevZoom);
      atualizarViewport();
    }

    function ajustarZoom(f) {
      zoom = Math.max(0.3, Math.min(3, zoom + f));
      atualizarViewport();
    }

    // ==================== MINIMAP ====================
    function atualizarMinimap() {
      const minimap = document.getElementById('minimap');
      const mmViewport = document.getElementById('minimap-viewport');
      if (nos.length === 0) { minimap.innerHTML = '<div class="minimap-viewport" id="minimap-viewport"></div>'; return; }

      let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
      nos.forEach(n => {
        minX = Math.min(minX, n.pos_x);
        minY = Math.min(minY, n.pos_y);
        maxX = Math.max(maxX, n.pos_x + (n.largura || 240));
        maxY = Math.max(maxY, n.pos_y + (n.altura || 150));
      });

      const pad = 100;
      minX -= pad; minY -= pad; maxX += pad; maxY += pad;
      const worldW = maxX - minX;
      const worldH = maxY - minY;
      const mmW = 180, mmH = 120;
      const scale = Math.min(mmW / worldW, mmH / worldH);

      // Remove old nodes
      minimap.querySelectorAll('.minimap-node').forEach(n => n.remove());

      nos.forEach(n => {
        const dot = document.createElement('div');
        dot.className = 'minimap-node';
        const tipo = n.tipo || 'arquivo';
        const dados = n.dados_extra ? JSON.parse(n.dados_extra) : {};
        let color = getColorByType(tipo === 'arquivo' ? (dados.tipo_arquivo || 'doc') : tipo);
        dot.style.cssText = `left:${(n.pos_x - minX) * scale}px;top:${(n.pos_y - minY) * scale}px;width:${Math.max(4, (n.largura || 240) * scale)}px;height:${Math.max(3, (n.altura || 150) * scale)}px;background:${color};opacity:0.7;`;
        minimap.appendChild(dot);
      });

      // Viewport rect
      const rect = container.getBoundingClientRect();
      const vpLeft = (-panX / zoom - minX) * scale;
      const vpTop = (-panY / zoom - minY) * scale;
      const vpW = (rect.width / zoom) * scale;
      const vpH = (rect.height / zoom) * scale;
      mmViewport.style.cssText = `left:${vpLeft}px;top:${vpTop}px;width:${vpW}px;height:${vpH}px;`;
    }

    // ==================== DATA LOADING ====================
    function carregarCanvas() {
      fetch(`/api/projetos/${PROJETO_ID}/canvas`)
        .then(r => r.json())
        .then(data => {
          nos = data.nos || [];
          ligacoes = data.ligacoes || [];
          renderizarNos();
          renderizarLigacoes();
          atualizarMinimap();
        })
        .catch(err => console.error("Erro ao carregar canvas:", err));
    }

    function carregarArvore() {
      fetch(`/api/projetos/${PROJETO_ID}/arvore`)
        .then(r => r.json())
        .then(data => {
          arquivosArvore = data.raiz || [];
          renderizarArvore();
        })
        .catch(err => console.error("Erro ao carregar árvore:", err));
    }

    // ==================== FILE TREE ====================
    function renderizarArvore() {
      const tree = document.getElementById('file-tree');
      const items = obterItensAtuais();

      if (items.length === 0) {
        tree.innerHTML = '<div class="tree-empty">Nenhum arquivo nesta pasta.</div>';
        return;
      }

      tree.innerHTML = '';
      // Sort: folders first, then alphabetical
      const sorted = [...items].sort((a, b) => {
        if (a.eh_pasta && !b.eh_pasta) return -1;
        if (!a.eh_pasta && b.eh_pasta) return 1;
        return a.nome.localeCompare(b.nome);
      });

      sorted.forEach(item => {
        if (searchTerm && !item.nome.toLowerCase().includes(searchTerm.toLowerCase())) return;
        tree.appendChild(criarTreeItem(item));
      });

      atualizarBreadcrumbs();
    }

    function obterItensAtuais() {
      if (pastaAtualId === null) return arquivosArvore;
      // Find pasta in tree recursively
      const pasta = encontrarPastaRecursiva(arquivosArvore, pastaAtualId);
      return pasta ? (pasta.filhos || []) : [];
    }

    function encontrarPastaRecursiva(items, id) {
      for (const item of items) {
        if (item.id === id) return item;
        if (item.eh_pasta && item.filhos) {
          const found = encontrarPastaRecursiva(item.filhos, id);
          if (found) return found;
        }
      }
      return null;
    }

    function criarTreeItem(item) {
      const div = document.createElement('div');

      if (item.eh_pasta) {
        const wrapper = document.createElement('div');
        const row = document.createElement('div');
        row.className = 'tree-item';
        const childCount = (item.filhos || []).length;
        row.innerHTML = `
          <span class="tree-folder-toggle expanded" onclick="event.stopPropagation();toggleFolder(this)">${ICONS.chevron}</span>
          <span class="tree-item-icon" style="color:var(--color-folder)">${ICONS.folder}</span>
          <span class="tree-item-name">${item.nome}</span>
          ${childCount > 0 ? `<span class="tree-item-count">${childCount}</span>` : ''}
        `;
        row.addEventListener('dblclick', () => navegarPasta(item.id, item.nome));

        wrapper.appendChild(row);

        if (item.filhos && item.filhos.length > 0) {
          const children = document.createElement('div');
          children.className = 'tree-children';
          item.filhos.forEach(f => children.appendChild(criarTreeItem(f)));
          wrapper.appendChild(children);
        }

        return wrapper;
      } else {
        div.className = 'tree-item';
        const tipo = item.tipo_arquivo || 'documento';
        div.innerHTML = `
          <span class="tree-item-icon" style="color:${getColorByType(tipo)}">${getIconByType(tipo)}</span>
          <span class="tree-item-name">${item.nome}</span>
          <span class="tree-item-count">${formatBytes(item.tamanho_bytes || 0)}</span>
        `;
        div.addEventListener('dblclick', () => {
          // Find corresponding node and open preview
          const no = nos.find(n => n.arquivo_id === item.id || (n.dados_extra && JSON.parse(n.dados_extra).caminho === item.caminho));
          if (no) abrirPreviewNode(no);
        });
      }

      return div;
    }

    function toggleFolder(el) {
      el.classList.toggle('expanded');
      const children = el.closest('.tree-item').nextElementSibling;
      if (children && children.classList.contains('tree-children')) {
        children.classList.toggle('collapsed');
      }
    }

    function navegarPasta(id, nome) {
      if (id === null) {
        pastaAtualId = null;
        caminhoAtual = [];
      } else {
        pastaAtualId = id;
        // Build path
        const idx = caminhoAtual.findIndex(c => c.id === id);
        if (idx >= 0) {
          caminhoAtual = caminhoAtual.slice(0, idx + 1);
        } else {
          caminhoAtual.push({ id, nome });
        }
      }
      renderizarArvore();
    }

    function atualizarBreadcrumbs() {
      const bc = document.getElementById('breadcrumbs');
      bc.innerHTML = `<span class="breadcrumb-item ${pastaAtualId === null ? 'active' : ''}" onclick="navegarPasta(null)">Raiz</span>`;
      caminhoAtual.forEach((item, i) => {
        bc.innerHTML += `<span class="breadcrumb-sep">›</span>`;
        const isLast = i === caminhoAtual.length - 1;
        bc.innerHTML += `<span class="breadcrumb-item ${isLast ? 'active' : ''}" onclick="navegarPasta(${item.id}, '${item.nome}')">${item.nome}</span>`;
      });
    }

    function filtrarArquivos(term) {
      searchTerm = term;
      renderizarArvore();
    }

    function criarNovaPasta() {
      const nome = prompt("Nome da nova pasta:", "Nova Pasta");
      if (!nome || !nome.trim()) return;

      fetch(`/api/projetos/${PROJETO_ID}/pastas`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nome: nome.trim(), pasta_pai_id: pastaAtualId })
      })
      .then(r => r.json())
      .then(data => {
        if (data.status === 'success') {
          showToast(`Pasta "${nome}" criada com sucesso.`, 'success');
          carregarArvore();
        }
      });
    }

    // ==================== RENDER NODES ====================
    function renderizarNos() {
      nodesContainer.innerHTML = '';
      nos.forEach(no => {
        const div = document.createElement('div');
        div.className = `node ${selecionadoNoId === no.id ? 'selected' : ''}`;
        div.style.left = `${no.pos_x}px`;
        div.style.top = `${no.pos_y}px`;
        div.style.width = `${no.largura || 240}px`;
        div.id = `node-${no.id}`;

        const dados = no.dados_extra ? JSON.parse(no.dados_extra) : {};
        let tipo = no.tipo;
        let tipoArq = tipo === 'arquivo' ? (dados.tipo_arquivo || 'documento') : tipo;
        let headerColor = getColorByType(tipoArq);
        let icon = tipo === 'nota' ? ICONS.note : tipo === 'pasta' ? ICONS.folder : getIconByType(tipoArq);

        let bodyContent = '';
        if (tipo === 'arquivo') {
          const ext = no.titulo ? no.titulo.split('.').pop().toLowerCase() : '';
          const isImage = ['png','jpg','jpeg','gif','svg','webp'].includes(ext);
          bodyContent = `
            <div class="node-thumbnail">
              ${isImage && dados.caminho ? `<img src="${dados.caminho}" loading="lazy">` : `<span style="color:${headerColor};opacity:0.5">${getIconByType(tipoArq).replace('width="16"','width="28"').replace('height="16"','height="28"')}</span>`}
            </div>
            <div class="node-meta">
              <span class="node-tag">${ext.toUpperCase()}</span>
              <span>${formatBytes(dados.tamanho || 0)}</span>
            </div>
          `;
        } else if (tipo === 'nota') {
          bodyContent = `<p style="font-style:italic;color:var(--text-muted);">${dados.descricao || 'Nota vazia'}</p>`;
        } else if (tipo === 'pasta') {
          bodyContent = `<p style="color:var(--text-muted);">Pasta de organização</p>`;
        }

        div.innerHTML = `
          <div class="node-header" style="background:${headerColor}">
            <span class="node-header-title">${icon} ${no.titulo || 'Sem título'}</span>
            <span class="node-delete" onclick="event.stopPropagation();removerNo(${no.id})">${ICONS.delete}</span>
          </div>
          <div class="node-body">${bodyContent}</div>
          <div class="port port-left" data-node-id="${no.id}" data-port="left" onmousedown="startConnecting(event, ${no.id}, 'left')"></div>
          <div class="port port-right" data-node-id="${no.id}" data-port="right" onmousedown="startConnecting(event, ${no.id}, 'right')"></div>
          <div class="port port-top" data-node-id="${no.id}" data-port="top" onmousedown="startConnecting(event, ${no.id}, 'top')"></div>
          <div class="port port-bottom" data-node-id="${no.id}" data-port="bottom" onmousedown="startConnecting(event, ${no.id}, 'bottom')"></div>
        `;

        div.addEventListener('mousedown', (e) => startDragNode(e, no));
        div.addEventListener('dblclick', () => abrirPreviewNode(no));
        div.addEventListener('contextmenu', (e) => { e.preventDefault(); mostrarContextMenu(e, no.id); });

        nodesContainer.appendChild(div);
      });
    }

    function renderizarLigacoes() {
      svgGroup.innerHTML = '';
      ligacoes.forEach(lig => {
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.className.baseVal = 'connection-line';
        path.id = `connection-${lig.id}`;

        const nOrigem = nos.find(n => n.id === lig.origem_id);
        const nDestino = nos.find(n => n.id === lig.destino_id);

        if (nOrigem && nDestino) {
          const ptO = obterCoordenadaPorta(nOrigem, lig.porta_origem);
          const ptD = obterCoordenadaPorta(nDestino, lig.porta_destino);
          path.setAttribute('d', calcularBezier(ptO.x, ptO.y, ptD.x, ptD.y, lig.porta_origem, lig.porta_destino));

          let cor = '#475569';
          if (lig.tipo === 'dependencia') cor = 'var(--color-code)';
          else if (lig.tipo === 'fluxo') cor = 'var(--color-note)';
          path.setAttribute('stroke', cor);

          path.addEventListener('contextmenu', (e) => { e.preventDefault(); removerLigacao(lig.id); });
          svgGroup.appendChild(path);
        }
      });
    }

    // ==================== NODE DRAG ====================
    function startDragNode(e, no) {
      if (e.target.classList.contains('port') || e.target.closest('.node-delete')) return;
      e.stopPropagation();
      dragNode = no;
      selecionadoNoId = no.id;
      document.querySelectorAll('.node').forEach(d => d.classList.remove('selected'));
      document.getElementById(`node-${no.id}`)?.classList.add('selected');

      mouseStartX = e.clientX; mouseStartY = e.clientY;
      nodeStartX = no.pos_x; nodeStartY = no.pos_y;

      const moveH = (evt) => {
        if (!dragNode) return;
        dragNode.pos_x = nodeStartX + (evt.clientX - mouseStartX) / zoom;
        dragNode.pos_y = nodeStartY + (evt.clientY - mouseStartY) / zoom;
        const div = document.getElementById(`node-${dragNode.id}`);
        if (div) { div.style.left = `${dragNode.pos_x}px`; div.style.top = `${dragNode.pos_y}px`; }
        renderizarLigacoes();
      };

      const upH = () => {
        if (dragNode) { salvarPosicaoNo(dragNode); dragNode = null; }
        window.removeEventListener('mousemove', moveH);
        window.removeEventListener('mouseup', upH);
        atualizarMinimap();
      };

      window.addEventListener('mousemove', moveH);
      window.addEventListener('mouseup', upH);
    }

    function salvarPosicaoNo(no) {
      fetch(`/api/projetos/${PROJETO_ID}/nos/${no.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pos_x: no.pos_x, pos_y: no.pos_y })
      });
    }

    // ==================== CREATE NODES ====================
    function obterCentroViewport() {
      const rect = container.getBoundingClientRect();
      return { x: (rect.width / 2 - panX) / zoom, y: (rect.height / 2 - panY) / zoom };
    }

    function criarNoNota() {
      const titulo = prompt("Título da Nota:", "Nova Nota");
      if (!titulo) return;
      const pos = obterCentroViewport();
      fetch(`/api/projetos/${PROJETO_ID}/nos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tipo: 'nota', titulo, pos_x: pos.x, pos_y: pos.y })
      }).then(r => r.json()).then(() => { carregarCanvas(); showToast('Nota criada.', 'success'); });
    }

    function criarNoPasta() {
      const titulo = prompt("Nome da Pasta:", "Nova Pasta");
      if (!titulo) return;
      const pos = obterCentroViewport();
      fetch(`/api/projetos/${PROJETO_ID}/nos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tipo: 'pasta', titulo, pos_x: pos.x, pos_y: pos.y })
      }).then(r => r.json()).then(() => { carregarCanvas(); showToast('Pasta criada no canvas.', 'success'); });
    }

    function removerNo(id) {
      if (!confirm("Remover este bloco do canvas?")) return;
      fetch(`/api/projetos/${PROJETO_ID}/nos/${id}`, { method: 'DELETE' })
        .then(() => { carregarCanvas(); showToast('Bloco removido.', 'info'); });
    }

    // ==================== CONNECTIONS ====================
    function startConnecting(e, nodeId, port) {
      e.stopPropagation(); e.preventDefault();
      const no = nos.find(n => n.id === nodeId);
      const coord = obterCoordenadaPorta(no, port);
      activeConnectionStart = { nodeId, port, ...coord };
      activeLine.style.display = 'block';
    }

    function drawActiveConnection(e) {
      if (!activeConnectionStart) return;
      const rect = container.getBoundingClientRect();
      const mx = (e.clientX - rect.left - panX) / zoom;
      const my = (e.clientY - rect.top - panY) / zoom;
      activeLine.setAttribute('d', calcularBezier(activeConnectionStart.x, activeConnectionStart.y, mx, my, activeConnectionStart.port, 'left'));
    }

    function cancelActiveConnection(e) {
      if (!activeConnectionStart) return;
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
      const tipo = prompt("Tipo (dependencia / referencia / fluxo):", "referencia");
      if (tipo === null) return;
      fetch(`/api/projetos/${PROJETO_ID}/ligacoes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ origem_id: origemId, destino_id: destinoId, porta_origem: portaOrigem, porta_destino: portaDestino, tipo })
      }).then(r => r.json()).then(() => { carregarCanvas(); showToast('Conexão criada.', 'success'); });
    }

    function removerLigacao(id) {
      if (!confirm("Remover esta conexão?")) return;
      fetch(`/api/projetos/${PROJETO_ID}/ligacoes/${id}`, { method: 'DELETE' })
        .then(() => { carregarCanvas(); showToast('Conexão removida.', 'info'); });
    }

    // ==================== CONTEXT MENU ====================
    function mostrarContextMenu(e, nodeId) {
      contextMenuNoId = nodeId;
      const menu = document.getElementById('context-menu');
      menu.style.left = e.clientX + 'px';
      menu.style.top = e.clientY + 'px';
      menu.classList.add('visible');
    }

    function editarNomeNo() {
      const no = nos.find(n => n.id === contextMenuNoId);
      if (!no) return;
      const novoTitulo = prompt("Novo título:", no.titulo);
      if (!novoTitulo) return;
      fetch(`/api/projetos/${PROJETO_ID}/nos/${no.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ titulo: novoTitulo })
      }).then(() => { carregarCanvas(); showToast('Nome atualizado.', 'success'); });
    }

    function moverNoParaPasta() {
      showToast('Use drag & drop na sidebar para mover arquivos.', 'info');
    }

    function removerNoContexto() {
      if (contextMenuNoId) removerNo(contextMenuNoId);
    }

    // ==================== UPLOAD ====================
    function handleFileUpload(e) {
      const files = Array.from(e.target.files);
      if (files.length > 0) uploadFiles(files);
      e.target.value = '';
    }

    function handleFileDrop(e) {
      e.preventDefault();
      document.getElementById('drop-overlay').classList.remove('visible');
      const files = Array.from(e.dataTransfer.files);
      if (files.length > 0) uploadFiles(files);
    }

    async function uploadFiles(files) {
      const progressEl = document.getElementById('upload-progress');
      const fillEl = document.getElementById('upload-progress-fill');
      const textEl = document.getElementById('upload-progress-text');
      progressEl.classList.add('visible');

      let done = 0;
      for (const file of files) {
        textEl.textContent = `${done + 1} / ${files.length} — ${file.name}`;
        fillEl.style.width = `${(done / files.length) * 100}%`;

        try {
          const base64 = await readFileAsBase64(file);
          let tipo = 'documento';
          const ext = file.name.split('.').pop().toLowerCase();
          if (['obj','stl','fbx','gltf','glb'].includes(ext)) tipo = '3d_model';
          else if (['pcb','brd','sch','kicad_pcb'].includes(ext)) tipo = 'pcb_design';
          else if (['dwg','dxf'].includes(ext)) tipo = 'autocad';
          else if (['png','jpg','jpeg','gif','svg','webp'].includes(ext)) tipo = 'imagem';

          const body = { name: file.name, content: base64, type: tipo };
          if (pastaAtualId) body.pasta_id = pastaAtualId;

          const res = await fetch(`/api/projetos/${PROJETO_ID}/upload`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
          });
          const data = await res.json();
          if (data.status === 'success') {
            showToast(`"${file.name}" enviado com sucesso.`, 'success');
          } else {
            showToast(`Erro ao enviar "${file.name}".`, 'error');
          }
        } catch(err) {
          showToast(`Erro ao enviar "${file.name}".`, 'error');
        }
        done++;
        fillEl.style.width = `${(done / files.length) * 100}%`;
      }

      textEl.textContent = `${done} / ${files.length} concluído`;
      setTimeout(() => progressEl.classList.remove('visible'), 2000);
      carregarCanvas();
      carregarArvore();
    }

    function readFileAsBase64(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result.split(',')[1]);
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });
    }

    // ==================== PREVIEW MODAL ====================
    function fecharPreview() {
      const modal = document.getElementById('preview-modal');
      modal.style.display = 'none';
      // Clean up any Three.js renderers
      const body = document.getElementById('modal-body');
      body.innerHTML = '';
    }

    function abrirPreviewNode(no) {
      if (no.tipo !== 'arquivo') return;
      const dados = no.dados_extra ? JSON.parse(no.dados_extra) : {};
      const modal = document.getElementById('preview-modal');
      const title = document.getElementById('modal-title');
      const iconEl = document.getElementById('modal-icon');
      const body = document.getElementById('modal-body');
      const footerInfo = document.getElementById('modal-footer-info');
      const downloadBtn = document.getElementById('modal-download');

      const tipo = dados.tipo_arquivo || 'documento';
      const ext = no.titulo ? no.titulo.split('.').pop().toLowerCase() : '';

      title.textContent = no.titulo;
      iconEl.innerHTML = getIconByType(tipo);
      body.innerHTML = '';

      // Footer info
      footerInfo.innerHTML = `
        <span>${no.titulo}</span>
        <span>${formatBytes(dados.tamanho || 0)}</span>
        <span>${ext.toUpperCase()}</span>
      `;

      if (dados.caminho) {
        downloadBtn.href = dados.caminho;
        downloadBtn.style.display = 'flex';
      } else {
        downloadBtn.style.display = 'none';
      }

      if (tipo === '3d_model' && dados.caminho) {
        renderizar3D(body, dados.caminho, ext, dados.tamanho || 0);
      } else if (tipo === 'imagem' && dados.caminho) {
        renderizarImagem(body, dados.caminho, no.titulo);
      } else if (tipo === 'pcb_design') {
        renderizarMetadados(body, no.titulo, ext, dados.tamanho || 0, 'pcb');
      } else if (tipo === 'autocad') {
        renderizarMetadados(body, no.titulo, ext, dados.tamanho || 0, 'autocad');
      } else {
        renderizarMetadados(body, no.titulo, ext, dados.tamanho || 0, 'generic');
      }

      modal.style.display = 'flex';
    }

    // ==================== 3D VIEWER (Three.js) ====================
    function renderizar3D(container, caminho, ext, tamanho) {
      const canvas3d = document.createElement('canvas');
      canvas3d.style.cssText = 'width:100%;height:100%;display:block;';
      container.appendChild(canvas3d);

      const scene = new THREE.Scene();
      scene.background = new THREE.Color(0x060810);

      const camera = new THREE.PerspectiveCamera(50, container.clientWidth / container.clientHeight, 0.1, 2000);
      camera.position.set(3, 2, 3);

      const renderer = new THREE.WebGLRenderer({ canvas: canvas3d, antialias: true });
      renderer.setSize(container.clientWidth, container.clientHeight);
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
      renderer.toneMapping = THREE.ACESFilmicToneMapping;

      const controls = new OrbitControls(camera, renderer.domElement);
      controls.enableDamping = true;
      controls.dampingFactor = 0.08;

      // Lighting
      scene.add(new THREE.AmbientLight(0xcccccc, 0.6));
      const dirLight = new THREE.DirectionalLight(0xffffff, 1.0);
      dirLight.position.set(5, 10, 7);
      scene.add(dirLight);

      // Grid
      const grid = new THREE.GridHelper(20, 20, 0x1a1f30, 0x111525);
      scene.add(grid);

      // Info panel
      const infoPanel = document.createElement('div');
      infoPanel.className = 'viewer-3d-info';
      infoPanel.innerHTML = `<span>Carregando modelo...</span>`;
      container.appendChild(infoPanel);

      // Controls bar
      const controlsBar = document.createElement('div');
      controlsBar.className = 'viewer-3d-controls';
      let wireframeMode = false;
      let gridVisible = true;
      controlsBar.innerHTML = `
        <button class="viewer-3d-btn" onclick="this.closest('.modal-body').querySelector('canvas').__resetCam?.()">Reset Camera</button>
        <button class="viewer-3d-btn" onclick="this.closest('.modal-body').querySelector('canvas').__toggleWire?.()">Wireframe</button>
        <button class="viewer-3d-btn" onclick="this.closest('.modal-body').querySelector('canvas').__toggleGrid?.()">Grid</button>
      `;
      container.appendChild(controlsBar);

      // Load model
      function onModelLoaded(object) {
        const box = new THREE.Box3().setFromObject(object);
        const size = new THREE.Vector3();
        box.getSize(size);
        const center = new THREE.Vector3();
        box.getCenter(center);

        object.position.sub(center);
        const maxDim = Math.max(size.x, size.y, size.z);
        const scale = 3 / maxDim;
        object.scale.multiplyScalar(scale);

        scene.add(object);

        camera.position.set(size.x * scale * 1.2, size.y * scale * 1.2, size.z * scale * 1.5);
        controls.target.set(0, 0, 0);
        controls.update();

        // Count geometry info
        let vertices = 0, faces = 0;
        object.traverse(child => {
          if (child.isMesh && child.geometry) {
            const geo = child.geometry;
            vertices += geo.attributes.position ? geo.attributes.position.count : 0;
            faces += geo.index ? geo.index.count / 3 : (geo.attributes.position ? geo.attributes.position.count / 3 : 0);
          }
        });

        infoPanel.innerHTML = `
          <span>Vértices <strong>${vertices.toLocaleString()}</strong></span>
          <span>Faces <strong>${Math.round(faces).toLocaleString()}</strong></span>
          <span>Tamanho <strong>${formatBytes(tamanho)}</strong></span>
          <span>Dimensão <strong>${size.x.toFixed(1)} × ${size.y.toFixed(1)} × ${size.z.toFixed(1)}</strong></span>
        `;

        canvas3d.__resetCam = () => { camera.position.set(3, 2, 3); controls.target.set(0,0,0); controls.update(); };
        canvas3d.__toggleWire = () => {
          wireframeMode = !wireframeMode;
          object.traverse(c => { if (c.isMesh) c.material.wireframe = wireframeMode; });
        };
        canvas3d.__toggleGrid = () => { gridVisible = !gridVisible; grid.visible = gridVisible; };
      }

      function onError(err) {
        infoPanel.innerHTML = `<span style="color:var(--color-error)">Erro ao carregar modelo</span>`;
        console.error('3D load error:', err);
      }

      if (['gltf', 'glb'].includes(ext)) {
        new GLTFLoader().load(caminho, (gltf) => onModelLoaded(gltf.scene), undefined, onError);
      } else if (ext === 'stl') {
        new STLLoader().load(caminho, (geometry) => {
          const material = new THREE.MeshStandardMaterial({ color: 0x8888cc, metalness: 0.3, roughness: 0.6 });
          onModelLoaded(new THREE.Mesh(geometry, material));
        }, undefined, onError);
      } else if (ext === 'obj') {
        new OBJLoader().load(caminho, onModelLoaded, undefined, onError);
      } else {
        infoPanel.innerHTML = `<span>Formato 3D não suportado para preview</span>`;
      }

      // Animation loop
      let animId;
      function animate() {
        animId = requestAnimationFrame(animate);
        controls.update();
        renderer.render(scene, camera);
      }
      animate();

      // Cleanup observer
      const observer = new MutationObserver(() => {
        if (!container.contains(canvas3d)) {
          cancelAnimationFrame(animId);
          renderer.dispose();
          observer.disconnect();
        }
      });
      observer.observe(container, { childList: true });
    }

    // ==================== IMAGE VIEWER ====================
    function renderizarImagem(container, caminho, titulo) {
      container.innerHTML = `
        <div class="image-viewer" id="img-viewer">
          <img src="${caminho}" id="img-preview" style="transition: transform 0.1s ease;">
        </div>
      `;

      const viewer = document.getElementById('img-viewer');
      const img = document.getElementById('img-preview');
      let imgZoom = 1, imgPanX = 0, imgPanY = 0, imgDragging = false, imgSX = 0, imgSY = 0;

      img.onload = () => {
        const vw = viewer.clientWidth, vh = viewer.clientHeight;
        const scale = Math.min(vw / img.naturalWidth, vh / img.naturalHeight, 1) * 0.9;
        imgZoom = scale;
        imgPanX = (vw - img.naturalWidth * scale) / 2;
        imgPanY = (vh - img.naturalHeight * scale) / 2;
        updateImgTransform();
      };

      function updateImgTransform() {
        img.style.left = imgPanX + 'px';
        img.style.top = imgPanY + 'px';
        img.style.width = (img.naturalWidth * imgZoom) + 'px';
        img.style.height = (img.naturalHeight * imgZoom) + 'px';
      }

      viewer.addEventListener('wheel', (e) => {
        e.preventDefault();
        const f = 0.1;
        imgZoom = e.deltaY < 0 ? imgZoom * (1 + f) : imgZoom * (1 - f);
        imgZoom = Math.max(0.1, Math.min(10, imgZoom));
        updateImgTransform();
      });

      viewer.addEventListener('mousedown', (e) => { imgDragging = true; imgSX = e.clientX - imgPanX; imgSY = e.clientY - imgPanY; });
      viewer.addEventListener('mousemove', (e) => { if (imgDragging) { imgPanX = e.clientX - imgSX; imgPanY = e.clientY - imgSY; updateImgTransform(); } });
      viewer.addEventListener('mouseup', () => imgDragging = false);
      viewer.addEventListener('mouseleave', () => imgDragging = false);
    }

    // ==================== METADATA VIEWER ====================
    function renderizarMetadados(container, titulo, ext, tamanho, tipo) {
      let iconSvg = '';
      let title = '';
      let color = '';

      if (tipo === 'pcb') {
        color = 'var(--color-pcb)';
        title = 'Schematic / PCB Design';
        iconSvg = ICONS.pcb.replace('width="16"','width="56"').replace('height="16"','height="56"');
      } else if (tipo === 'autocad') {
        color = 'var(--color-autocad)';
        title = 'AutoCAD Drawing';
        iconSvg = ICONS.autocad.replace('width="16"','width="56"').replace('height="16"','height="56"');
      } else {
        color = 'var(--color-doc)';
        title = 'Document';
        iconSvg = ICONS.file.replace('width="16"','width="56"').replace('height="16"','height="56"');
      }

      container.innerHTML = `
        <div class="metadata-panel">
          <div class="metadata-icon" style="color:${color};border-color:${color}33">${iconSvg}</div>
          <h2 style="font-size:20px;font-weight:600;color:var(--text-primary);margin-top:4px;">${titulo}</h2>
          <p style="color:var(--text-muted);font-size:14px;">${title}</p>
          <div class="metadata-details">
            <div class="metadata-item">
              <div class="metadata-item-label">Formato</div>
              <div class="metadata-item-value">${ext.toUpperCase()}</div>
            </div>
            <div class="metadata-item">
              <div class="metadata-item-label">Tamanho</div>
              <div class="metadata-item-value">${formatBytes(tamanho)}</div>
            </div>
            <div class="metadata-item">
              <div class="metadata-item-label">Tipo</div>
              <div class="metadata-item-value">${title}</div>
            </div>
            <div class="metadata-item">
              <div class="metadata-item-label">Status</div>
              <div class="metadata-item-value" style="color:var(--color-success)">Pronto</div>
            </div>
          </div>
        </div>
      `;
    }

    // ==================== ALGORITHMS ====================
    function obterCoordenadaPorta(node, port) {
      const x = node.pos_x, y = node.pos_y;
      const w = node.largura || 240, h = node.altura || 150;
      if (port === 'left') return { x, y: y + h / 2 };
      if (port === 'right') return { x: x + w, y: y + h / 2 };
      if (port === 'top') return { x: x + w / 2, y };
      if (port === 'bottom') return { x: x + w / 2, y: y + h };
      return { x, y };
    }

    function calcularBezier(x1, y1, x2, y2, pO, pD) {
      let off = Math.max(40, Math.abs(x2 - x1) * 0.4);
      let cp1x = x1, cp1y = y1, cp2x = x2, cp2y = y2;
      if (pO === 'right') cp1x += off; else if (pO === 'left') cp1x -= off;
      else if (pO === 'bottom') cp1y += off; else if (pO === 'top') cp1y -= off;
      if (pD === 'right') cp2x += off; else if (pD === 'left') cp2x -= off;
      else if (pD === 'bottom') cp2y += off; else if (pD === 'top') cp2y -= off;
      return `M ${x1} ${y1} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${x2} ${y2}`;
    }

    function organizarAutomatico() {
      let x = 100, y = 100;
      nos.forEach((no, idx) => {
        no.pos_x = x; no.pos_y = y;
        salvarPosicaoNo(no);
        x += 300;
        if ((idx + 1) % 3 === 0) { x = 100; y += 240; }
      });
      renderizarNos();
      renderizarLigacoes();
      atualizarMinimap();
      showToast('Layout organizado automaticamente.', 'success');
    }

    // ==================== UTILITIES ====================
    function formatBytes(bytes, decimals = 1) {
      if (!bytes || bytes === 0) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(decimals)) + ' ' + sizes[i];
    }

    // Make functions globally accessible
    window.startPan = startPan;
    window.dragCanvas = dragCanvas;
    window.endPan = endPan;
    window.handleZoom = handleZoom;
    window.ajustarZoom = ajustarZoom;
    window.resetarZoomPan = resetarZoomPan;
    window.criarNoNota = criarNoNota;
    window.criarNoPasta = criarNoPasta;
    window.criarNovaPasta = criarNovaPasta;
    window.organizarAutomatico = organizarAutomatico;
    window.removerNo = removerNo;
    window.startConnecting = startConnecting;
    window.fecharPreview = fecharPreview;
    window.handleFileUpload = handleFileUpload;
    window.filtrarArquivos = filtrarArquivos;
    window.navegarPasta = navegarPasta;
    window.toggleFolder = toggleFolder;
    window.mostrarContextMenu = mostrarContextMenu;
    window.editarNomeNo = editarNomeNo;
    window.moverNoParaPasta = moverNoParaPasta;
    window.removerNoContexto = removerNoContexto;
    window.showToast = showToast;
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
