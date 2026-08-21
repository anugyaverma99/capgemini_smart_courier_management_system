import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class AdminDashboard implements OnInit {
  errorMessage = signal('');

  dashboard = this.adminService.dashboardSignal;
  allDeliveries = this.adminService.deliveriesSignal;
  loading = this.adminService.loadingSignal;
  
  deliveries = computed(() => this.allDeliveries().slice(0, 5));

  constructor(
    private adminService: AdminService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.errorMessage.set('');
    this.adminService.getDashboard().subscribe({
      error: (err) => this.errorMessage.set(this.getErrorMessage(err))
    });
    this.adminService.getDeliveries().subscribe({
      error: (err) => console.error(err)
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private getErrorMessage(err: any): string {
    if (err?.error?.errors) return Object.values(err.error.errors).join(' ');
    return err?.error?.error || 'Failed to load admin dashboard.';
  }
}
