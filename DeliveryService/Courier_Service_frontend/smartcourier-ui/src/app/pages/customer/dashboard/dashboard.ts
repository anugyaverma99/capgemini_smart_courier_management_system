import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { DeliveryStatus } from '../../../core/models/delivery.models';
import { AuthService } from '../../../core/services/auth.service';
import { DeliveryService } from '../../../core/services/delivery.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  selectedStatus = signal<'ALL' | DeliveryStatus>('ALL');
  errorMessage = signal('');
  
  deliveries = this.deliveryService.deliveriesSignal;
  loading = this.deliveryService.loadingSignal;
  fullName = computed(() => this.authService.userSignal()?.fullName || 'Customer');

  filteredDeliveries = computed(() => {
    const status = this.selectedStatus();
    const all = this.deliveries();
    if (status === 'ALL') return all;
    return all.filter(d => d.status === status);
  });

  activeCount = computed(() => 
    this.deliveries().filter(d => !['DELIVERED', 'FAILED', 'RETURNED'].includes(d.status)).length
  );

  deliveredCount = computed(() => 
    this.deliveries().filter(d => d.status === 'DELIVERED').length
  );

  statuses: ('ALL' | DeliveryStatus)[] = ['ALL', 'BOOKED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED'];

  constructor(
    private deliveryService: DeliveryService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadDeliveries();
  }

  loadDeliveries(): void {
    this.errorMessage.set('');
    this.deliveryService.getMyDeliveries().subscribe({
      error: (err) => {
        this.errorMessage.set(this.getErrorMessage(err));
      }
    });
  }

  getStatusClass(status: DeliveryStatus): string {
    return `status-${status.toLowerCase().replaceAll('_', '-')}`;
  }

  trackDelivery(trackingNumber: string): void {
    this.router.navigate(['/track', trackingNumber]);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private getErrorMessage(err: any): string {
    if (err?.error?.errors) return Object.values(err.error.errors).join(' ');
    return err?.error?.error || 'Failed to load deliveries.';
  }
}
