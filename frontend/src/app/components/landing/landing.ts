import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css'
})
export class LandingComponent {
  selectedTab = signal<'core' | 'saas'>('core');
  billingCycle = signal<'monthly' | 'annual'>('monthly');

  selectTab(tab: 'core' | 'saas'): void {
    this.selectedTab.set(tab);
  }

  toggleBilling(): void {
    this.billingCycle.update(c => c === 'monthly' ? 'annual' : 'monthly');
  }
}
