import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { ConnectionService } from './connection.service';

export interface Permissao {
  caminho: string;
  ver: boolean;
  editar: boolean;
}

export interface UserProfile {
  id: number;
  username: string;
  cargo: string;
  cargo_nome: string;
  cargo_desc: string;
  ativo: boolean;
  permissoes: Permissao[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly connectionService = inject(ConnectionService);

  readonly currentUser = signal<UserProfile | null>(null);
  readonly token = signal<string | null>(typeof window !== 'undefined' ? localStorage.getItem('session_token') : null);

  private get apiUrl(): string {
    return this.connectionService.getBaseUrl();
  }

  constructor() {
    if (this.token()) {
      this.loadCurrentUser().subscribe({
        error: () => this.logout()
      });
    }
  }

  login(credentials: { username?: string; password?: string }): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/api/login`, credentials).pipe(
      tap(res => {
        if (res.token) {
          if (typeof window !== 'undefined') {
            localStorage.setItem('session_token', res.token);
          }
          this.token.set(res.token);
          this.loadCurrentUser().subscribe();
        }
      })
    );
  }

  loadCurrentUser(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/api/me`).pipe(
      tap(user => {
        this.currentUser.set(user);
      })
    );
  }

  logout(): void {
    this.http.post(`${this.apiUrl}/api/logout`, {}).subscribe({
      next: () => this.clearSession(),
      error: () => this.clearSession()
    });
  }

  private clearSession(): void {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('session_token');
    }
    this.token.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return this.token() !== null;
  }

  isAdmin(): boolean {
    const user = this.currentUser();
    return user !== null && user.cargo === 'ADMIN';
  }
}
