import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-troca-senha',
  imports: [FormsModule],
  templateUrl: './troca-senha.html',
  styleUrl: './troca-senha.css'
})
export class TrocaSenhaComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected readonly senhaAtual = signal('');
  protected readonly novaSenha = signal('');
  protected readonly confirmarSenha = signal('');
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly loading = signal(false);

  onSubmit(): void {
    this.error.set(null);
    this.success.set(null);

    if (!this.senhaAtual() || !this.novaSenha() || !this.confirmarSenha()) {
      this.error.set('Preencha todos os campos.');
      return;
    }

    if (this.novaSenha().length < 6) {
      this.error.set('A nova senha deve ter no mínimo 6 caracteres.');
      return;
    }

    if (this.novaSenha() !== this.confirmarSenha()) {
      this.error.set('As senhas não coincidem.');
      return;
    }

    if (this.senhaAtual() === this.novaSenha()) {
      this.error.set('A nova senha não pode ser igual à atual.');
      return;
    }

    this.loading.set(true);

    this.http.post<any>('/api/troca-senha', {
      senha_atual: this.senhaAtual(),
      nova_senha: this.novaSenha()
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set('Senha alterada com sucesso! Redirecionando...');

        // Recarregar dados do usuário (deveTrocarSenha agora é false)
        this.authService.loadCurrentUser().subscribe({
          next: () => {
            setTimeout(() => this.router.navigate(['/dashboard']), 1500);
          },
          error: () => {
            setTimeout(() => this.router.navigate(['/dashboard']), 1500);
          }
        });
      },
      error: (err) => {
        this.loading.set(false);
        if (err.error?.error) {
          this.error.set(err.error.error);
        } else if (err.status === 401) {
          this.error.set('Senha atual incorreta.');
        } else {
          this.error.set('Erro ao alterar a senha. Tente novamente.');
        }
      }
    });
  }
}
