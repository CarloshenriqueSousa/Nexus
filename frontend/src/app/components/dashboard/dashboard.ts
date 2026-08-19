import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ConnectionService } from '../../services/connection.service';
import { TreeNodeComponent } from './tree-node';

@Component({
  selector: 'app-dashboard',
  imports: [FormsModule, RouterLink, TreeNodeComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  protected readonly connectionService = inject(ConnectionService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private get apiUrl(): string {
    return this.connectionService.getBaseUrl();
  }

  // States
  protected readonly projects = signal<any[]>([]);
  protected readonly selectedProject = signal<any | null>(null);
  protected readonly fileTree = signal<any[]>([]);
  protected readonly currentFolder = signal<any | null>(null);

  // Search & View
  protected readonly searchTerm = signal('');
  protected readonly viewMode = signal<'grid' | 'list'>('grid');

  // Create Project Modal
  protected readonly isCreateModalOpen = signal(false);

  // Modal de movimentação
  protected readonly isMoveModalOpen = signal(false);
  protected readonly itemToMove = signal<any | null>(null);
  protected readonly availableFolders = signal<any[]>([]);
  protected readonly targetFolderId = signal<number | null>(null);
  protected readonly moveModalError = signal<string | null>(null);

  // Form inputs
  protected readonly newProjectName = signal('');
  protected readonly newProjectDesc = signal('');
  protected readonly newFolderName = signal('');

  // Status/Logs
  protected readonly serverStatus = signal('Carregando...');
  protected readonly dbStatus = signal('Carregando...');
  protected readonly javaVersion = signal('-');
  protected readonly logs = signal<string[]>([]);
  protected readonly loading = signal(false);

  // Computed: filtered projects
  protected readonly filteredProjects = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const all = this.projects();
    if (!term) return all;
    return all.filter(p =>
      p.nome.toLowerCase().includes(term) ||
      (p.descricao && p.descricao.toLowerCase().includes(term))
    );
  });

  ngOnInit(): void {
    this.loadProjects();
    this.fetchSystemStatus();
    this.addLog('Área de trabalho inicializada.', 'info');
  }

  loadProjects(): void {
    this.loading.set(true);
    this.http.get<any[]>(`${this.apiUrl}/api/projetos`).subscribe({
      next: (data) => {
        this.projects.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.addLog('Erro ao carregar lista de projetos.', 'error');
        this.loading.set(false);
      }
    });
  }

  createProject(): void {
    const nome = this.newProjectName().trim();
    const descricao = this.newProjectDesc().trim();

    if (!nome) return;

    this.http.post<any>(`${this.apiUrl}/api/projetos`, { nome, descricao }).subscribe({
      next: (res) => {
        this.addLog(`Projeto '${nome}' criado com sucesso.`, 'success');
        this.newProjectName.set('');
        this.newProjectDesc.set('');
        this.isCreateModalOpen.set(false);
        this.loadProjects();
      },
      error: (err) => {
        this.addLog('Erro ao criar projeto.', 'error');
      }
    });
  }

  deleteProject(id: number, event: Event): void {
    event.stopPropagation();
    if (!confirm('Deseja realmente remover este projeto? Todos os arquivos e o canvas associados serão permanentemente deletados.')) {
      return;
    }

    this.http.delete(`${this.apiUrl}/api/projetos/${id}`).subscribe({
      next: () => {
        this.addLog('Projeto excluído.', 'info');
        if (this.selectedProject() && this.selectedProject().id === id) {
          this.selectedProject.set(null);
          this.fileTree.set([]);
          this.currentFolder.set(null);
        }
        this.loadProjects();
      },
      error: () => {
        this.addLog('Erro ao remover projeto.', 'error');
      }
    });
  }

  selectProject(project: any): void {
    this.router.navigate(['/canvas', project.id]);
  }

  loadFileTree(): void {
    const proj = this.selectedProject();
    if (!proj) return;

    this.http.get<any>(`${this.apiUrl}/api/projetos/${proj.id}/arvore`).subscribe({
      next: (data) => {
        this.fileTree.set(data.raiz || []);
      },
      error: () => {
        this.addLog('Erro ao carregar a árvore de arquivos.', 'error');
      }
    });
  }

  createFolder(): void {
    const proj = this.selectedProject();
    const nome = this.newFolderName().trim();
    if (!proj || !nome) return;

    const body: any = { nome };
    if (this.currentFolder()) {
      body.pasta_pai_id = this.currentFolder().id;
    }

    this.http.post(`${this.apiUrl}/api/projetos/${proj.id}/pastas`, body).subscribe({
      next: () => {
        this.addLog(`Pasta '${nome}' criada com sucesso.`, 'success');
        this.newFolderName.set('');
        this.loadFileTree();
      },
      error: () => {
        this.addLog('Erro ao criar pasta.', 'error');
      }
    });
  }

  handleDeleteNode(node: any): void {
    if (node.eh_pasta) {
      this.deleteFolder(node);
    } else {
      this.deleteFile(node);
    }
  }

  deleteFolder(folder: any): void {
    const proj = this.selectedProject();
    if (!proj) return;

    this.http.delete(`${this.apiUrl}/api/projetos/${proj.id}/pastas/${folder.id}`).subscribe({
      next: () => {
        this.addLog(`Pasta '${folder.nome}' excluída recursivamente.`, 'info');
        if (this.currentFolder() && this.currentFolder().id === folder.id) {
          this.currentFolder.set(null);
        }
        this.loadFileTree();
      },
      error: () => {
        this.addLog('Erro ao excluir pasta.', 'error');
      }
    });
  }

  deleteFile(file: any): void {
    const proj = this.selectedProject();
    if (!proj) return;

    this.http.delete(`${this.apiUrl}/api/projetos/${proj.id}/arquivos/${file.id}`).subscribe({
      next: () => {
        this.addLog(`Arquivo '${file.nome}' excluído com sucesso.`, 'info');
        this.loadFileTree();
      },
      error: () => {
        this.addLog('Erro ao excluir arquivo.', 'error');
      }
    });
  }

  handleRenameNode(payload: { node: any, newName: string }): void {
    if (payload.node.eh_pasta) {
      this.renameFolder(payload);
    } else {
      this.renameFile(payload);
    }
  }

  renameFolder(payload: { node: any, newName: string }): void {
    const proj = this.selectedProject();
    if (!proj) return;

    this.http.put(`${this.apiUrl}/api/projetos/${proj.id}/pastas/${payload.node.id}/renomear`, { nome: payload.newName }).subscribe({
      next: () => {
        this.addLog(`Pasta renomeada para '${payload.newName}'.`, 'info');
        this.loadFileTree();
      },
      error: () => {
        this.addLog('Erro ao renomear pasta.', 'error');
      }
    });
  }

  renameFile(payload: { node: any, newName: string }): void {
    const proj = this.selectedProject();
    if (!proj) return;

    this.http.put(`${this.apiUrl}/api/projetos/${proj.id}/arquivos/${payload.node.id}/renomear`, { nome: payload.newName }).subscribe({
      next: () => {
        this.addLog(`Arquivo renomeado para '${payload.newName}'.`, 'info');
        this.loadFileTree();
      },
      error: () => {
        this.addLog('Erro ao renomear arquivo.', 'error');
      }
    });
  }

  openMoveModal(node: any): void {
    this.itemToMove.set(node);

    const folders: any[] = [];
    const collect = (nodes: any[], prefix = '') => {
      for (const n of nodes) {
        if (n.eh_pasta) {
          if (n.id === node.id) {
            continue;
          }
          folders.push({
            id: n.id,
            nome: n.nome,
            caminhoExibicao: prefix + '/' + n.nome
          });
          if (n.filhos) {
            collect(n.filhos, prefix + '/' + n.nome);
          }
        }
      }
    };
    collect(this.fileTree());
    this.availableFolders.set(folders);

    this.targetFolderId.set(node.pasta_pai_id || null);
    this.moveModalError.set(null);
    this.isMoveModalOpen.set(true);
  }

  closeMoveModal(): void {
    this.isMoveModalOpen.set(false);
    this.itemToMove.set(null);
  }

  setTargetFolderId(val: string): void {
    if (val === 'raiz') {
      this.targetFolderId.set(null);
    } else {
      this.targetFolderId.set(parseInt(val, 10));
    }
  }

  confirmMoveItem(): void {
    const proj = this.selectedProject();
    const item = this.itemToMove();
    if (!proj || !item) return;

    const destFolderId = this.targetFolderId();

    this.http.put(`${this.apiUrl}/api/projetos/${proj.id}/arquivos/${item.id}/mover`, { nova_pasta_id: destFolderId }).subscribe({
      next: () => {
        this.addLog(`'${item.nome}' movido com sucesso.`, 'success');
        this.closeMoveModal();
        this.loadFileTree();
      },
      error: (err: any) => {
        if (err.error && err.error.error) {
          this.moveModalError.set(err.error.error);
        } else {
          this.moveModalError.set('Erro ao mover o item.');
        }
      }
    });
  }

  onFileUpload(event: Event): void {
    const proj = this.selectedProject();
    const input = event.target as HTMLInputElement;
    if (!proj || !input.files || input.files.length === 0) return;

    const file = input.files[0];
    const headers: any = {
      'X-File-Name': encodeURIComponent(file.name),
      'X-File-Type': file.type || 'application/octet-stream'
    };

    if (this.currentFolder()) {
      headers['X-Folder-Id'] = this.currentFolder().id.toString();
    }

    this.addLog(`Fazendo upload de '${file.name}'...`, 'info');

    this.http.post<any>(`${this.apiUrl}/api/projetos/${proj.id}/upload`, file, { headers }).subscribe({
      next: (res) => {
        this.addLog(`Arquivo '${file.name}' enviado com sucesso.`, 'success');
        this.loadFileTree();
        input.value = '';
      },
      error: () => {
        this.addLog(`Erro ao enviar o arquivo '${file.name}'.`, 'error');
      }
    });
  }

  selectFolderForUpload(folder: any): void {
    this.currentFolder.set(folder);
    this.addLog(`Pasta '${folder.nome}' selecionada como destino para uploads.`, 'info');
  }

  clearUploadDestination(): void {
    this.currentFolder.set(null);
    this.addLog('Destino de upload redefinido para a Raiz.', 'info');
  }

  fetchSystemStatus(): void {
    this.http.get<any>(`${this.apiUrl}/api/health`).subscribe({
      next: (data) => {
        this.serverStatus.set(data.status === 'ok' ? 'Online' : 'Degradado');
        this.dbStatus.set(data.db ? 'Conectado' : 'Desconectado');
      },
      error: () => {
        this.serverStatus.set('Offline');
        this.dbStatus.set('Erro');
      }
    });

    this.http.get<any>(`${this.apiUrl}/api/info-server`).subscribe({
      next: (data) => {
        this.javaVersion.set(data.java || '-');
      },
      error: () => {}
    });
  }

  addLog(message: string, type: 'info' | 'success' | 'error' = 'info'): void {
    const time = new Date().toLocaleTimeString();
    const formatted = `[${time}] ${message}`;
    this.logs.update(current => [formatted, ...current].slice(0, 50));
  }

  logout(): void {
    this.authService.logout();
  }
}
