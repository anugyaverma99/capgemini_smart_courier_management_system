import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DeliveryResponse, DeliveryStatus } from '../../../core/models/delivery.models';
import { TrackingEventResponse } from '../../../core/models/tracking.model';
import { DeliveryService } from '../../../core/services/delivery.service';
import { TrackingService } from '../../../core/services/tracking.service';

@Component({
  selector: 'app-track-delivery',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './track-delivery.html',
  styleUrl: './track-delivery.css'
})
export class TrackDelivery implements OnInit {
  trackingNumber = '';
  delivery: DeliveryResponse | null = null;
  trackingEvents: TrackingEventResponse[] = [];
  loading = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private deliveryService: DeliveryService,
    private trackingService: TrackingService
  ) {}

  ngOnInit(): void {
    const trackingNumber = this.route.snapshot.paramMap.get('trackingNumber');
    if (trackingNumber) {
      this.trackingNumber = trackingNumber;
      this.track();
    }
  }

  track(): void {
    if (!this.trackingNumber.trim()) {
      this.errorMessage = 'Please enter a tracking number.';
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.delivery = null;
    this.trackingEvents = [];

    this.deliveryService.trackByNumber(this.trackingNumber.trim()).subscribe({
      next: (delivery) => {
        this.delivery = delivery;
        this.loadEvents(delivery.trackingNumber);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.error || 'No delivery found for this tracking number.';
      }
    });
  }

  loadEvents(trackingNumber: string): void {
    this.trackingService.getTimeline(trackingNumber).subscribe({
      next: (events) => {
        this.trackingEvents = events.length ? events : this.fallbackEvents();
        this.loading = false;
      },
      error: () => {
        this.trackingEvents = this.fallbackEvents();
        this.loading = false;
      }
    });
  }

  getStatusClass(status: DeliveryStatus): string {
    return `status-${status.toLowerCase().replaceAll('_', '-')}`;
  }

  private createDeliveryStatusEvent(delivery: DeliveryResponse): TrackingEventResponse {
    return {
      id: delivery.id,
      deliveryId: String(delivery.id),
      trackingNumber: delivery.trackingNumber,
      status: delivery.status,
      location: `${delivery.senderAddress.city} to ${delivery.receiverAddress.city}`,
      description: `Current delivery status is ${delivery.status}.`,
      eventTime: delivery.updatedAt || delivery.createdAt,
      createdAt: delivery.createdAt
    };
  }

  private fallbackEvents(): TrackingEventResponse[] {
    return this.delivery ? [this.createDeliveryStatusEvent(this.delivery)] : [];
  }

  clear(): void {
    this.trackingNumber = '';
    this.delivery = null;
    this.trackingEvents = [];
    this.errorMessage = '';
  }
}
