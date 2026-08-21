import { CommonModule } from '@angular/common';
import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ExceptionResponse } from '../../../core/models/admin.model';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-exceptions',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './exceptions.html',
  styleUrl: './exceptions.css'
})
export class AdminExceptions implements OnInit {
  private adminService = inject(AdminService);
  private authService = inject(AuthService);

  errorMessage = signal('');
  successMessage = signal('');
  filter = signal<'ALL' | 'OPEN' | 'RESOLVED'>('ALL');

  exceptions = this.adminService.exceptionsSignal;
  loading = this.adminService.loadingSignal;

  filteredExceptions = computed(() => {
    const currentFilter = this.filter();
    const all = this.exceptions();
    if (currentFilter === 'ALL') return all;
    return all.filter(e => e.resolutionStatus === currentFilter);
  });

  openCount = computed(() => this.exceptions().filter(e => e.resolutionStatus === 'OPEN').length);
  resolvedCount = computed(() => this.exceptions().filter(e => e.resolutionStatus === 'RESOLVED').length);

  ngOnInit(): void { 
    this.loadExceptions(); 
  }

  loadExceptions(): void {
    this.errorMessage.set('');
    this.adminService.getExceptions().subscribe({
      error: (err) => this.errorMessage.set(err?.error?.error || 'Failed to load exceptions.')
    });
  }

  resolve(item: ExceptionResponse): void {
    const resolvedBy = this.authService.getEmail() || 'admin';
    this.adminService.resolveException(item.id, 'Resolved from admin panel.', resolvedBy).subscribe({
      next: () => {
        this.successMessage.set(`Exception #${item.id} resolved successfully.`);
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error || 'Failed to resolve exception.');
      }
    });
  }

  getTypeBadgeClass(type: string): string {
    const map: Record<string, string> = {
      'DELAYED': 'type-delayed',
      'FAILED': 'type-failed',
      'RETURNED': 'type-returned',
    };
    return map[type] || 'type-default';
  }
}