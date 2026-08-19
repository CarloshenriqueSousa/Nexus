import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';
import { ConnectionService } from '../../services/connection.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.css'
})
export class ShellComponent {
  protected readonly authService = inject(AuthService);
  protected readonly themeService = inject(ThemeService);
  protected readonly connectionService = inject(ConnectionService);
  private readonly router = inject(Router);

  sidebarCollapsed = signal(false);
  notificationsOpen = signal(false);
  unreadNotificationsCount = signal(3);

  toggleSidebar(): void {
    this.sidebarCollapsed.update(v => !v);
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  toggleNotifications(): void {
    this.notificationsOpen.update(v => !v);
    if (this.notificationsOpen()) {
      this.unreadNotificationsCount.set(0);
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
