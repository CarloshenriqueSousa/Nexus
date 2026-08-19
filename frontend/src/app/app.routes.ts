import { Routes } from '@angular/router';
import { LandingComponent } from './components/landing/landing';
import { ConnectionComponent } from './components/connection/connection';
import { LoginComponent } from './components/login/login';
import { ShellComponent } from './components/shell/shell';
import { DashboardComponent } from './components/dashboard/dashboard';
import { CanvasComponent } from './components/canvas/canvas';
import { AdminComponent } from './components/admin/admin';
import { SettingsComponent } from './components/settings/settings';
import { TrocaSenhaComponent } from './components/troca-senha/troca-senha';
import { authGuard, adminGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'landing', component: LandingComponent },
  { path: 'connection', component: ConnectionComponent },
  { path: 'login', component: LoginComponent },
  { path: 'troca-senha', component: TrocaSenhaComponent, canActivate: [authGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'canvas/:id', component: CanvasComponent },
      { path: 'admin', component: AdminComponent, canActivate: [adminGuard] },
      { path: 'settings', component: SettingsComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];


