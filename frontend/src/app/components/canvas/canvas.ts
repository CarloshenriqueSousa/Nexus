import { Component, inject, signal, OnInit, HostListener, ElementRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ConnectionService } from '../../services/connection.service';

interface CanvasNode {
  id: number;
  projeto_id: number;
  arquivo_id: number | null;
  tipo: string;
  titulo: string;
  pos_x: number;
  pos_y: number;
  largura: number;
  altura: number;
  cor: string;
  dados_extra?: string;
}

interface CanvasLink {
  id: number;
  projeto_id: number;
  origem_id: number;
  destino_id: number;
  tipo: string;
  cor: string;
  label: string;
  porta_origem: string;
  porta_destino: string;
}

@Component({
  selector: 'app-canvas',
  imports: [FormsModule, RouterLink],
  templateUrl: './canvas.html',
  styleUrl: './canvas.css'
})
export class CanvasComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly authService = inject(AuthService);
  protected readonly connectionService = inject(ConnectionService);

  private get apiUrl(): string {
    return this.connectionService.getBaseUrl();
  }

  @ViewChild('richEditorRef') richEditorRef?: ElementRef<HTMLDivElement>;

  // States
  protected readonly projectId = signal<number | null>(null);
  protected readonly projectName = signal('Carregando Projeto...');
  protected readonly nodes = signal<CanvasNode[]>([]);
  protected readonly links = signal<CanvasLink[]>([]);
  protected readonly projectTree = signal<any[]>([]);

  // Interaction States
  protected readonly connectionMode = signal(false);
  protected readonly selectedNodeForLink = signal<CanvasNode | null>(null);

  // Zoom and Pan states
  protected readonly zoomLevel = signal<number>(100);
  protected readonly panX = signal<number>(0);
  protected readonly panY = signal<number>(0);
  protected readonly syncStatus = signal<'synced' | 'saving' | 'error'>('synced');

  // Dragging states
  private activeDraggingNode: CanvasNode | null = null;
  private dragStartX = 0;
  private dragStartY = 0;
  
  // Panning states
  private isPanning = false;
  private panStartX = 0;
  private panStartY = 0;

  // Subfolder & Folder Filtering
  protected readonly expandedFolderIds = signal<Set<number>>(new Set());
  protected readonly expandedSubfolderIds = signal<Set<number>>(new Set());
  protected readonly activeFolderId = signal<number | null>(null);
  protected readonly activeFolderName = signal<string | null>(null);

  // Universal File Modal & Rich Text Editor (Google Docs style)
  protected readonly activeFileModal = signal<any | null>(null);
  protected readonly fileContent = signal<string>('');
  protected readonly fileMimeType = signal<string>('');
  protected readonly filePath = signal<string>('');
  protected readonly fileViewMode = signal<'rich' | 'code'>('rich');
  protected readonly isSavingFile = signal<boolean>(false);
  protected readonly saveMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      if (id) {
        this.projectId.set(id);
        this.loadProjectDetails(id);
        this.loadCanvasData(id);
        this.loadProjectTree(id);
      }
    });
  }

  loadProjectDetails(id: number): void {
    this.http.get<any>(`${this.apiUrl}/api/projetos/${id}`).subscribe({
      next: (data) => {
        this.projectName.set(data.nome);
      },
      error: () => {
        this.projectName.set('Projeto Desconhecido');
      }
    });
  }

  loadCanvasData(projId: number): void {
    this.syncStatus.set('saving');
    this.http.get<any>(`${this.apiUrl}/api/projetos/${projId}/canvas`).subscribe({
      next: (data) => {
        this.nodes.set(data.nos || []);
        this.links.set(data.ligacoes || []);
        this.syncStatus.set('synced');
      },
      error: () => {
        this.syncStatus.set('error');
        alert('Erro ao carregar os dados do Canvas.');
      }
    });
  }

  loadProjectTree(projId: number): void {
    this.http.get<any>(`${this.apiUrl}/api/projetos/${projId}/arvore`).subscribe({
      next: (data) => {
        this.projectTree.set(data.raiz || []);
      },
      error: () => {}
    });
  }

  addNode(tipo: string, parentFolderId?: number): void {
    const projId = this.projectId();
    if (!projId) return;

    const titulo = tipo === 'pasta' ? 'Nova Pasta' : tipo === 'arquivo' ? 'Novo Arquivo' : 'Nova Nota';
    const body: any = {
      tipo,
      titulo,
      pos_x: 200 + Math.random() * 150,
      pos_y: 150 + Math.random() * 150
    };

    if (parentFolderId) {
      body.dados_extra = JSON.stringify({ pasta_pai_id: parentFolderId });
    }

    this.syncStatus.set('saving');
    this.http.post<any>(`${this.apiUrl}/api/projetos/${projId}/nos`, body).subscribe({
      next: (res) => {
        if (res.status === 'success') {
          this.loadCanvasData(projId);
          this.loadProjectTree(projId);
        }
      },
      error: () => {
        this.syncStatus.set('error');
      }
    });
  }

  startDragging(node: CanvasNode, event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.closest('.node-action') || target.closest('.subfolder-container') || target.closest('.rich-editor') || this.connectionMode()) return;

    event.preventDefault();
    event.stopPropagation();
    this.activeDraggingNode = node;
    const zoom = this.zoomLevel() / 100;
    this.dragStartX = event.clientX - node.pos_x * zoom;
    this.dragStartY = event.clientY - node.pos_y * zoom;
  }

  startPanning(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.closest('.canvas-node') || target.closest('.canvas-toolbar') || target.closest('.canvas-header') || target.closest('.sandbox-panel') || target.closest('.file-modal-card')) return;

    this.isPanning = true;
    this.panStartX = event.clientX - this.panX();
    this.panStartY = event.clientY - this.panY();
  }

  onWheelZoom(event: WheelEvent): void {
    if (event.ctrlKey) {
      event.preventDefault();
      const zoomFactor = event.deltaY < 0 ? 5 : -5;
      const newZoom = Math.min(200, Math.max(50, this.zoomLevel() + zoomFactor));
      this.zoomLevel.set(newZoom);
    }
  }

  @HostListener('window:mousemove', ['$event'])
  onMouseMove(event: MouseEvent): void {
    if (this.activeDraggingNode) {
      const zoom = this.zoomLevel() / 100;
      const newX = Math.max(0, (event.clientX - this.dragStartX) / zoom);
      const newY = Math.max(0, (event.clientY - this.dragStartY) / zoom);

      const nodeToUpdate = this.activeDraggingNode;
      this.nodes.update(list => list.map(n => {
        if (n.id === nodeToUpdate.id) {
          return { ...n, pos_x: newX, pos_y: newY };
        }
        return n;
      }));
    } else if (this.isPanning) {
      const dx = event.clientX - this.panStartX;
      const dy = event.clientY - this.panStartY;
      this.panX.set(dx);
      this.panY.set(dy);
    }
  }

  @HostListener('window:mouseup', ['$event'])
  onMouseUp(event: MouseEvent): void {
    if (this.activeDraggingNode) {
      const node = this.activeDraggingNode;
      this.activeDraggingNode = null;

      const localUpdated = this.nodes().find(n => n.id === node.id);
      if (!localUpdated) return;

      this.syncStatus.set('saving');
      this.http.put(`${this.apiUrl}/api/projetos/${localUpdated.projeto_id}/nos/${localUpdated.id}`, {
        pos_x: localUpdated.pos_x,
        pos_y: localUpdated.pos_y
      }).subscribe({
        next: () => {
          this.syncStatus.set('synced');
        },
        error: () => {
          this.syncStatus.set('error');
          this.loadCanvasData(localUpdated.projeto_id);
        }
      });
    } else if (this.isPanning) {
      this.isPanning = false;
    }
  }

  selectNodeForAction(node: CanvasNode): void {
    if (this.connectionMode()) {
      const selected = this.selectedNodeForLink();
      if (!selected) {
        this.selectedNodeForLink.set(node);
      } else {
        if (selected.id === node.id) {
          this.selectedNodeForLink.set(null);
          return;
        }
        this.createLink(selected.id, node.id);
      }
    }
  }

  createLink(origemId: number, destinoId: number): void {
    const projId = this.projectId();
    if (!projId) return;

    this.http.post<any>(`${this.apiUrl}/api/projetos/${projId}/ligacoes`, {
      origem_id: origemId,
      destino_id: destinoId,
      tipo: 'conexao',
      porta_origem: 'saida',
      porta_destino: 'entrada'
    }).subscribe({
      next: () => {
        this.loadCanvasData(projId);
        this.toggleConnectionMode(false);
      },
      error: () => {
        alert('Erro ao ligar os nós.');
        this.toggleConnectionMode(false);
      }
    });
  }

  toggleConnectionMode(state?: boolean): void {
    const active = state !== undefined ? state : !this.connectionMode();
    this.connectionMode.set(active);
    this.selectedNodeForLink.set(null);
  }

  deleteNode(node: CanvasNode, event: Event): void {
    event.stopPropagation();
    if (!confirm(`Deseja realmente remover o nó '${node.titulo}'?`)) return;

    this.http.delete(`${this.apiUrl}/api/projetos/${node.projeto_id}/nos/${node.id}`).subscribe({
      next: () => {
        this.loadCanvasData(node.projeto_id);
        this.loadProjectTree(node.projeto_id);
      }
    });
  }

  deleteLink(linkId: number, event: Event): void {
    event.stopPropagation();
    if (!confirm('Remover esta ligação?')) return;

    const projId = this.projectId();
    if (!projId) return;

    this.http.delete(`${this.apiUrl}/api/projetos/${projId}/ligacoes/${linkId}`).subscribe({
      next: () => {
        this.loadCanvasData(projId);
      }
    });
  }

  renameNode(node: CanvasNode, event: Event): void {
    event.stopPropagation();
    const newName = prompt('Novo título para o bloco:', node.titulo);
    if (newName && newName.trim()) {
      this.http.put(`${this.apiUrl}/api/projetos/${node.projeto_id}/nos/${node.id}`, {
        titulo: newName.trim()
      }).subscribe({
        next: () => {
          this.loadCanvasData(node.projeto_id);
        }
      });
    }
  }

  // ==========================================
  // RECURSIVE SUBPASTAS & PASTA PAI NO CANVAS
  // ==========================================
  toggleFolderExpansion(nodeId: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.expandedFolderIds.update(set => {
      const newSet = new Set(set);
      if (newSet.has(nodeId)) {
        newSet.delete(nodeId);
      } else {
        newSet.add(nodeId);
      }
      return newSet;
    });
  }

  isFolderExpanded(nodeId: number): boolean {
    return this.expandedFolderIds().has(nodeId);
  }

  toggleSubfolderExpansion(folderId: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.expandedSubfolderIds.update(set => {
      const newSet = new Set(set);
      if (newSet.has(folderId)) {
        newSet.delete(folderId);
      } else {
        newSet.add(folderId);
      }
      return newSet;
    });
  }

  isSubfolderExpanded(folderId: number): boolean {
    return this.expandedSubfolderIds().has(folderId);
  }

  getFolderChildren(folderNode: CanvasNode): any[] {
    const tree = this.projectTree();
    if (folderNode.arquivo_id) {
      const found = this.findTreeItemById(tree, folderNode.arquivo_id);
      if (found && found.filhos) return found.filhos;
    }
    const foundByName = this.findTreeItemByName(tree, folderNode.titulo);
    if (foundByName && foundByName.filhos) return foundByName.filhos;

    return [];
  }

  private findTreeItemById(items: any[], id: number): any | null {
    for (const item of items) {
      if (item.id === id) return item;
      if (item.filhos && item.filhos.length > 0) {
        const sub = this.findTreeItemById(item.filhos, id);
        if (sub) return sub;
      }
    }
    return null;
  }

  private findTreeItemByName(items: any[], name: string): any | null {
    for (const item of items) {
      if (item.nome.toLowerCase() === name.toLowerCase()) return item;
      if (item.filhos && item.filhos.length > 0) {
        const sub = this.findTreeItemByName(item.filhos, name);
        if (sub) return sub;
      }
    }
    return null;
  }

  createSubfolderInFolder(parentFolderId: number, event: Event): void {
    event.stopPropagation();
    const projId = this.projectId();
    if (!projId) return;

    const nome = prompt('Nome da nova subpasta:');
    if (!nome || !nome.trim()) return;

    this.http.post<any>(`${this.apiUrl}/api/projetos/${projId}/pastas`, {
      nome: nome.trim(),
      pasta_pai_id: parentFolderId
    }).subscribe({
      next: () => {
        this.loadProjectTree(projId);
        this.loadCanvasData(projId);
      },
      error: () => {
        alert('Erro ao criar subpasta.');
      }
    });
  }

  uploadFileToParentFolder(parentFolderId: number, event: Event): void {
    event.stopPropagation();
    const projId = this.projectId();
    if (!projId) return;

    const input = document.createElement('input');
    input.type = 'file';
    input.onchange = (e: any) => {
      const file = e.target.files?.[0];
      if (!file) return;

      const headers: any = {
        'X-File-Name': encodeURIComponent(file.name),
        'X-File-Type': file.type || 'application/octet-stream',
        'X-Folder-Id': parentFolderId.toString()
      };

      this.http.post<any>(`${this.apiUrl}/api/projetos/${projId}/upload`, file, { headers }).subscribe({
        next: () => {
          this.loadProjectTree(projId);
          this.loadCanvasData(projId);
        },
        error: () => {
          alert('Erro ao enviar arquivo para a pasta.');
        }
      });
    };
    input.click();
  }

  filterCanvasByFolder(folderNode: CanvasNode | null, event?: Event): void {
    if (event) event.stopPropagation();
    if (!folderNode) {
      this.activeFolderId.set(null);
      this.activeFolderName.set(null);
    } else {
      this.activeFolderId.set(folderNode.id);
      this.activeFolderName.set(folderNode.titulo);
    }
  }

  // ==========================================
  // VISUALIZADOR UNIVERSAL & EDITOR RICO (GOOGLE DOCS)
  // ==========================================
  openFileModal(node: any, event?: Event): void {
    if (event) event.stopPropagation();
    this.activeFileModal.set(node);
    this.saveMessage.set(null);

    // Se veio um item direto da árvore (ArquivoProjeto)
    const isTreeItem = !node.projeto_id && node.id;
    const projId = this.projectId();

    const title = node.titulo || node.nome;
    const aid = node.arquivo_id || (isTreeItem ? node.id : null);
    const extra = this.parseDadosExtra(node);
    this.filePath.set(extra.caminho || node.caminho || '');
    this.fileMimeType.set(extra.tipo_mime || node.tipo_mime || '');

    if (node.tipo === 'nota') {
      this.fileContent.set(extra.conteudo || title);
      this.fileViewMode.set('rich');
      setTimeout(() => this.updateRichEditorDOM(), 50);
    } else if (aid && projId) {
      const category = this.getMediaCategory(node);
      if (category === 'text') {
        this.http.get<any>(`${this.apiUrl}/api/projetos/${projId}/arquivos/${aid}/conteudo`).subscribe({
          next: (res) => {
            this.fileContent.set(res.conteudo || '');
            this.fileViewMode.set('rich');
            setTimeout(() => this.updateRichEditorDOM(), 50);
          },
          error: () => {
            this.fileContent.set('Erro ao carregar conteúdo do arquivo.');
          }
        });
      }
    }
  }

  closeFileModal(): void {
    this.activeFileModal.set(null);
    this.fileContent.set('');
    this.saveMessage.set(null);
  }

  saveFileModalContent(): void {
    const item = this.activeFileModal();
    const projId = this.projectId();
    if (!item || !projId) return;

    this.isSavingFile.set(true);
    let newContent = this.fileContent();

    if (this.fileViewMode() === 'rich' && this.richEditorRef) {
      newContent = this.richEditorRef.nativeElement.innerHTML;
      this.fileContent.set(newContent);
    }

    if (item.tipo === 'nota') {
      const extra = this.parseDadosExtra(item);
      extra.conteudo = newContent;
      const updatedExtra = JSON.stringify(extra);

      this.http.put(`${this.apiUrl}/api/projetos/${projId}/nos/${item.id}`, {
        dados_extra: updatedExtra
      }).subscribe({
        next: () => {
          this.isSavingFile.set(false);
          this.saveMessage.set('Nota salva com sucesso!');
          this.loadCanvasData(projId);
          setTimeout(() => this.saveMessage.set(null), 3000);
        },
        error: () => {
          this.isSavingFile.set(false);
          this.saveMessage.set('Erro ao salvar nota.');
        }
      });
    } else {
      const aid = item.arquivo_id || item.id;
      if (aid) {
        this.http.put(`${this.apiUrl}/api/projetos/${projId}/arquivos/${aid}/conteudo`, {
          conteudo: newContent
        }).subscribe({
          next: () => {
            this.isSavingFile.set(false);
            this.saveMessage.set('Arquivo salvo com sucesso!');
            this.loadCanvasData(projId);
            this.loadProjectTree(projId);
            setTimeout(() => this.saveMessage.set(null), 3000);
          },
          error: () => {
            this.isSavingFile.set(false);
            this.saveMessage.set('Erro ao salvar arquivo.');
          }
        });
      }
    }
  }

  execEditorCommand(command: string, value: string = ''): void {
    document.execCommand(command, false, value);
    if (this.richEditorRef) {
      this.fileContent.set(this.richEditorRef.nativeElement.innerHTML);
    }
  }

  private updateRichEditorDOM(): void {
    if (this.richEditorRef) {
      this.richEditorRef.nativeElement.innerHTML = this.fileContent();
    }
  }

  onRichEditorInput(): void {
    if (this.richEditorRef) {
      this.fileContent.set(this.richEditorRef.nativeElement.innerHTML);
    }
  }

  getMediaCategory(node: any): 'image' | 'pdf' | 'audio' | 'video' | 'text' | 'binary' {
    if (node.tipo === 'nota') return 'text';
    const type = this.getFileType(node).toLowerCase();
    const name = (node.titulo || node.nome || '').toLowerCase();

    if (['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'imagem'].includes(type) || name.match(/\.(png|jpg|jpeg|gif|svg|webp)$/)) {
      return 'image';
    }
    if (type === 'pdf' || name.endsWith('.pdf')) {
      return 'pdf';
    }
    if (['mp3', 'wav', 'ogg', 'audio'].includes(type) || name.match(/\.(mp3|wav|ogg)$/)) {
      return 'audio';
    }
    if (['mp4', 'webm', 'video'].includes(type) || name.match(/\.(mp4|webm)$/)) {
      return 'video';
    }
    if (['txt', 'md', 'html', 'css', 'js', 'ts', 'java', 'json', 'xml', 'py', 'sql', 'codigo', 'documento'].includes(type) || name.match(/\.(txt|md|html|css|js|ts|java|json|xml|py|sql|doc|docx)$/)) {
      return 'text';
    }
    return 'binary';
  }

  getFileType(node: any): string {
    if (node.tipo_arquivo) return node.tipo_arquivo;
    if (!node.dados_extra) return 'documento';
    try {
      const dados = JSON.parse(node.dados_extra);
      return dados.tipo_arquivo || 'documento';
    } catch {
      return 'documento';
    }
  }

  private parseDadosExtra(node: any): any {
    if (!node.dados_extra) return {};
    try {
      return JSON.parse(node.dados_extra);
    } catch {
      return {};
    }
  }

  getLinkPath(link: CanvasLink): string {
    const origin = this.nodes().find(n => n.id === link.origem_id);
    const dest = this.nodes().find(n => n.id === link.destino_id);
    if (!origin || !dest) return '';

    const x1 = origin.pos_x + 94;
    const y1 = origin.pos_y + 48;
    const x2 = dest.pos_x + 94;
    const y2 = dest.pos_y + 48;

    const dx = Math.abs(x2 - x1) * 0.55 + 40;
    return `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`;
  }

  getLinkCenter(link: CanvasLink): { x: number; y: number } {
    const origin = this.nodes().find(n => n.id === link.origem_id);
    const dest = this.nodes().find(n => n.id === link.destino_id);
    if (!origin || !dest) return { x: 0, y: 0 };
    return {
      x: (origin.pos_x + dest.pos_x) / 2 + 94,
      y: (origin.pos_y + dest.pos_y) / 2 + 48,
    };
  }

  // ==========================================
  // LÓGICA DO SANDBOX INTEGRADO
  // ==========================================
  sandboxOpen = signal<boolean>(false);
  shellCommand = signal('');
  shellHistory = signal<{ text: string, type: string }[]>([
    { text: 'Nexus Vaultra Sandbox Console v2.0', type: 'system' },
    { text: 'Sistema Operacional Seguro Isolado (SOSI)', type: 'system' }
  ]);

  toggleSandbox() {
    this.sandboxOpen.set(!this.sandboxOpen());
  }

  executeSandboxCommand() {
    const cmd = this.shellCommand().trim();
    if (!cmd) return;

    this.shellHistory.update(history => [...history, { text: `> ${cmd}`, type: 'cmd' }]);
    this.shellCommand.set('');

    if (cmd.toLowerCase() === 'clear') {
      this.shellHistory.set([
        { text: 'Nexus Vaultra Sandbox Console v2.0', type: 'system' },
        { text: 'Console limpo.', type: 'system' }
      ]);
      return;
    }

    this.http.post<any>(`${this.apiUrl}/api/admin/sandbox/exec`, { comando: cmd }).subscribe({
      next: (res) => {
        if (res.status === 'success') {
          const lines = (res.output || '').split(/\\\\n/);
          const newEntries = lines.map((l: string) => ({ text: l, type: 'sandbox-line' }));
          this.shellHistory.update(history => [...history, ...newEntries]);
        } else {
          this.shellHistory.update(history => [...history, { text: res.error || 'Erro no comando', type: 'err' }]);
        }
      },
      error: () => {
        this.shellHistory.update(history => [...history, { text: 'Falha na comunicação com o sandbox (Verifique se possui cargo ADMIN).', type: 'err' }]);
      }
    });
  }
}