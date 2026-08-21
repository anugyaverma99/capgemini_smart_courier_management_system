import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { DeliveryStatus } from '../../../core/models/delivery.models';
import { AuthService } from '../../../core/services/auth.service';
import { DeliveryService } from '../../../core/services/delivery.service';

@Component({
  selector: 'app-customer-deliveries',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './deliveries.html',
  styleUrl: './deliveries.css'
})
export class CustomerDeliveries implements OnInit {
  selectedStatus = signal<'ALL' | DeliveryStatus>('ALL');
  errorMessage = signal('');

  deliveries = this.deliveryService.deliveriesSignal;
  loading = this.deliveryService.loadingSignal;

  filteredDeliveries = computed(() => {
    const status = this.selectedStatus();
    const all = this.deliveries();
    if (status === 'ALL') return all;
    return all.filter(delivery => delivery.status === status);
  });

  statuses: ('ALL' | DeliveryStatus)[] = ['ALL', 'BOOKED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'DELAYED', 'FAILED', 'RETURNED'];

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
        this.errorMessage.set(err?.error?.error || 'Failed to load deliveries.');
      }
    });
  }

  trackDelivery(trackingNumber: string): void {
    this.router.navigate(['/track', trackingNumber]);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
