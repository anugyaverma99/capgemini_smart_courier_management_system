import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DeliveryMonitorResponse } from '../../../core/models/admin.model';
import { DeliveryResponse, DeliveryStatus } from '../../../core/models/delivery.models';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { DeliveryService } from '../../../core/services/delivery.service';
import { TrackingService } from '../../../core/services/tracking.service';
import { timeout } from 'rxjs/operators';

@Component({
  selector: 'app-admin-deliveries',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './deliveries.html',
  styleUrl: './deliveries.css'
})
export class AdminDeliveries implements OnInit {
  deliveries: DeliveryMonitorResponse[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  filter: 'ALL' | DeliveryStatus = 'ALL';
  selectedStatus: Record<string, DeliveryStatus> = {};
  statuses: ('ALL' | DeliveryStatus)[] = ['ALL', 'BOOKED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'DELAYED', 'FAILED', 'RETURNED'];

  constructor(
    private adminService: AdminService,
    private trackingService: TrackingService,
    private deliveryService: DeliveryService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDeliveries();
  }

  get filteredDeliveries(): DeliveryMonitorResponse[] {
    if (this.filter === 'ALL') return this.deliveries;
    return this.deliveries.filter(d => d.currentStatus === this.filter);
  }

  loadDeliveries(): void {
    this.loading = true;
    this.errorMessage = '';
    this.adminService.getDeliveries().subscribe({
      next: (data) => {
        this.deliveries = data;
        data.forEach(d => this.selectedStatus[d.deliveryId] = d.currentStatus);
        this.loading = false;
      },
      error: (err) => {
        this.loadDeliveriesFallback(err);
      }
    });
  }

  private loadDeliveriesFallback(originalError: any): void {
    this.deliveryService.getAllDeliveries().pipe(timeout(2500)).subscribe({
      next: (data) => {
        this.deliveries = data.map(delivery => this.toMonitorDelivery(delivery));
        this.deliveries.forEach(d => this.selectedStatus[d.deliveryId] = d.currentStatus);
        this.errorMessage = '';
        this.loading = false;
      },
      error: () => {
        this.errorMessage = originalError?.name === 'TimeoutError'
          ? 'Admin delivery service is taking too long. Please check gateway/admin service logs.'
          : originalError?.error?.error || 'Failed to load deliveries.';
        this.loading = false;
      }
    });
  }

  private toMonitorDelivery(delivery: DeliveryResponse): DeliveryMonitorResponse {
    return {
      deliveryId: String(delivery.id),
      trackingNumber: delivery.trackingNumber,
      customerName: `Customer ID: ${delivery.customerId}`,
      senderCity: delivery.senderAddress?.city || 'Unknown',
      receiverCity: delivery.receiverAddress?.city || 'Unknown',
      currentStatus: delivery.status,
      assignedHub: 'Unassigned',
      lastUpdated: delivery.updatedAt || delivery.createdAt
    };
  }

  updateStatus(delivery: DeliveryMonitorResponse): void {
    const status = this.selectedStatus[delivery.deliveryId];
    this.adminService.updateDeliveryStatus(delivery.deliveryId, status).subscribe({
      next: () => {
        this.addTrackingEvent(delivery, status);
      },
      error: (err) => this.errorMessage = err?.error?.error || 'Failed to update status.'
    });
  }

  private addTrackingEvent(delivery: DeliveryMonitorResponse, status: DeliveryStatus): void {
    this.trackingService.addEvent({
      deliveryId: delivery.deliveryId,
      trackingNumber: delivery.trackingNumber,
      status,
      location: delivery.receiverCity || delivery.senderCity || 'SmartCourier Hub',
      remarks: `Delivery status updated to ${status}.`,
      updatedBy: this.authService.getEmail() || 'admin'
    }).subscribe({
      next: () => {
        this.successMessage = 'Delivery status and tracking timeline updated.';
        this.loadDeliveries();
      },
      error: () => {
        this.successMessage = 'Delivery status updated. Tracking event could not be saved.';
        this.loadDeliveries();
      }
    });
  }

  getAllowedStatuses(currentStatus: DeliveryStatus): DeliveryStatus[] {
    const transitions: Record<string, DeliveryStatus[]> = {
      'DRAFT': ['BOOKED'],
      'BOOKED': ['PICKED_UP', 'DELAYED'],
      'PICKED_UP': ['IN_TRANSIT', 'DELAYED'],
      'IN_TRANSIT': ['OUT_FOR_DELIVERY', 'DELAYED'],
      'OUT_FOR_DELIVERY': ['DELIVERED', 'FAILED', 'DELAYED'],
      'DELIVERED': [],
      'DELAYED': ['PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'FAILED'],
      'FAILED': ['RETURNED'],
      'RETURNED': []
    };
    return transitions[currentStatus] || [];
  }
}
