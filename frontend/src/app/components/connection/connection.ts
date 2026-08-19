import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ConnectionService, ServerConfig } from '../../services/connection.service';

@Component({
  selector: 'app-connection',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './connection.html',
  styleUrl: './connection.css'
})
export class ConnectionComponent {
  private readonly connectionService = inject(ConnectionService);
  private readonly router = inject(Router);

  mode = signal<'local' | 'cloud' | 'custom'>(this.connectionService.config().mode);
  customUrl = signal<string>(this.connectionService.config().url);
  isTesting = signal<boolean>(false);
  testStatus = signal<{ success: boolean; message: string } | null>(null);

  selectMode(selectedMode: 'local' | 'cloud' | 'custom'): void {
    this.mode.set(selectedMode);
    this.testStatus.set(null);
    if (selectedMode === 'local') {
      this.customUrl.set('http://localhost:8081');
    } else if (selectedMode === 'cloud') {
      this.customUrl.set('https://api.nexus-vaultra.com');
    }
  }

  testConnection(): void {
    this.isTesting.set(true);
    this.testStatus.set(null);

    const url = this.customUrl();
    this.connectionService.checkHealth(url).subscribe(success => {
      this.isTesting.set(false);
      if (success) {
        this.testStatus.set({
          success: true,
          message: 'Conexão estabelecida com sucesso com o servidor Nexus!'
        });
      } else {
        this.testStatus.set({
          success: false,
          message: 'Não foi possível se conectar ao servidor. Verifique o IP/porta ou sua conexão de rede.'
        });
      }
    });
  }

  confirmAndProceed(): void {
    const config: ServerConfig = {
      mode: this.mode(),
      url: this.customUrl()
    };
    this.connectionService.saveConfig(config);
    this.router.navigate(['/login']);
  }
}
