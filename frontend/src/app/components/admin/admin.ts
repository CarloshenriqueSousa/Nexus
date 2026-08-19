import { Component, inject, signal, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ConnectionService } from '../../services/connection.service';

interface User {
  id: number;
  username: string;
  cargo: string;
  cargo_nome: string;
  ativo: boolean;
}

interface ShellLine {
  text: string;
  type: 'cmd' | 'output' | 'error' | 'info';
}

@Component({
  selector: 'app-admin',
  imports: [FormsModule, RouterLink],
  templateUrl: './admin.html',
  styleUrl: './admin.css'
})
export class AdminComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  protected readonly connectionService = inject(ConnectionService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private get apiUrl(): string {
    return this.connectionService.getBaseUrl();
  }

  // States
  protected readonly activeTab = signal<'users' | 'sandbox'>('users');
  protected readonly users = signal<User[]>([]);
  protected readonly loading = signal(false);

  // User form states
  protected readonly newUsername = signal('');
  protected readonly newPassword = signal('');
  protected readonly newCargo = signal('VISUALIZADOR');
  protected readonly formError = signal<string | null>(null);

  // Sandbox states
  protected readonly shellCommand = signal('');
  protected readonly shellHistory = signal<ShellLine[]>([
    { text: 'Console de Segurança Isolada (Nexus Sandbox) v1.0.0', type: 'info' },
    { text: 'Digite "help" para ver os comandos disponíveis.', type: 'info' }
  ]);

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.http.get<User[]>(`${this.apiUrl}/api/admin/usuarios`).subscribe({
      next: (data) => {
        this.users.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        alert('Erro ao carregar usuários. Verifique se você tem permissão.');
      }
    });
  }

  createUser(): void {
    const username = this.newUsername().trim();
    const senha = this.newPassword().trim();
    const cargo = this.newCargo();

    if (!username || !senha) {
      this.formError.set('Campos Usuário e Senha são obrigatórios.');
      return;
    }

    this.formError.set(null);
    this.http.post<any>(`${this.apiUrl}/api/admin/usuarios`, { username, senha, cargo }).subscribe({
      next: (res) => {
        this.newUsername.set('');
        this.newPassword.set('');
        this.newCargo.set('VISUALIZADOR');
        this.loadUsers();
      },
      error: (err) => {
        if (err.error && err.error.error) {
          this.formError.set(err.error.error);
        } else {
          this.formError.set('Erro ao criar usuário.');
        }
      }
    });
  }

  deleteUser(id: number): void {
    if (id === 1) {
      alert('Não é possível remover o administrador principal.');
      return;
    }
    if (!confirm('Deseja realmente excluir este usuário?')) {
      return;
    }

    this.http.delete(`${this.apiUrl}/api/admin/usuarios?id=${id}`).subscribe({
      next: () => {
        this.loadUsers();
      },
      error: (err) => {
        alert('Erro ao remover usuário.');
      }
    });
  }

  executeSandboxCommand(): void {
    const cmd = this.shellCommand().trim();
    if (!cmd) return;

    this.shellHistory.update(history => [...history, { text: `> ${cmd}`, type: 'cmd' }]);
    this.shellCommand.set('');

    if (cmd.toLowerCase() === 'clear') {
      this.shellHistory.set([]);
      return;
    }

    this.http.post<any>(`${this.apiUrl}/api/admin/sandbox/exec`, { comando: cmd }).subscribe({
      next: (res) => {
        if (res.status === 'success') {
          // Os logs do backend podem vir com \n, quebrando as linhas
          const lines = res.output.split('\\n');
          const toAdd = lines.map((l: string) => ({ text: l, type: 'output' as const }));
          this.shellHistory.update(history => [...history, ...toAdd]);
        } else {
          this.shellHistory.update(history => [...history, { text: res.error || 'Erro no sandbox', type: 'error' }]);
        }
      },
      error: (err) => {
        this.shellHistory.update(history => [...history, { text: 'Falha na comunicação com o sandbox.', type: 'error' }]);
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
