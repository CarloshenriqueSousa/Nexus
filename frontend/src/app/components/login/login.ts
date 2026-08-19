import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly username = signal('');
  protected readonly password = signal('');
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  onSubmit(): void {
    if (!this.username() || !this.password()) {
      this.error.set('Por favor, preencha todos os campos.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.authService.login({
      username: this.username(),
      password: this.password()
    }).subscribe({
      next: (res) => {
        this.loading.set(false);
        // Se o backend indicou troca de senha obrigatória, redirecionar
        if (res.deve_trocar_senha) {
          this.router.navigate(['/troca-senha']);
        } else {
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 401) {
          this.error.set('Usuário ou senha incorretos.');
        } else if (err.status === 403) {
          this.error.set('Sua conta está inativa. Contate o administrador.');
        } else {
          this.error.set('Ocorreu um erro de autenticação. Verifique sua conexão.');
        }
      }
    });
  }
}
