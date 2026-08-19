import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, timeout } from 'rxjs';

export interface ServerConfig {
  mode: 'local' | 'cloud' | 'custom';
  url: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConnectionService {
  private readonly http = inject(HttpClient);

  readonly config = signal<ServerConfig>(this.loadConfig());
  readonly isConnected = signal<boolean>(false);
  readonly serverInfo = signal<any>(null);

  constructor() {
    this.checkHealth().subscribe();
  }

  private loadConfig(): ServerConfig {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('nexus_server_config');
      if (saved) {
        try {
          return JSON.parse(saved);
        } catch (e) {
          // ignore
        }
      }
    }
    return {
      mode: 'local',
      url: 'http://localhost:8081'
    };
  }

  saveConfig(newConfig: ServerConfig): void {
    this.config.set(newConfig);
    if (typeof window !== 'undefined') {
      localStorage.setItem('nexus_server_config', JSON.stringify(newConfig));
    }
    this.checkHealth().subscribe();
  }

  getBaseUrl(): string {
    return this.config().url;
  }

  checkHealth(url?: string): Observable<boolean> {
    const targetUrl = url || this.getBaseUrl();
    return this.http.get<any>(`${targetUrl}/api/health`).pipe(
      timeout(4000),
      map(res => {
        this.isConnected.set(true);
        this.serverInfo.set(res);
        return true;
      }),
      catchError(() => {
        if (!url) {
          this.isConnected.set(false);
          this.serverInfo.set(null);
        }
        return of(false);
      })
    );
  }
}
