import { CommonModule } from '@angular/common';
import { Component, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DeliveryResponse, DeliveryStatus } from '../../../core/models/delivery.models';
import { TrackingEventResponse, DocumentResponse } from '../../../core/models/tracking.model';
import { DeliveryService } from '../../../core/services/delivery.service';
import { TrackingService } from '../../../core/services/tracking.service';

@Component({
  selector: 'app-delivery-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './delivery-detail.html',
  styleUrl: './delivery-detail.css'
})
export class DeliveryDetail implements OnInit {
  delivery = signal<DeliveryResponse | null>(null);
  errorMessage = signal('');
  
  timeline = this.trackingService.timelineSignal;
  documents = this.trackingService.documentsSignal;
  proof = this.trackingService.proofSignal;
  
  loading = signal(false);
  timelineLoading = this.trackingService.loadingSignal;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private deliveryService: DeliveryService,
    private trackingService: TrackingService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadDelivery(id);
      this.trackingService.getDocuments(id.toString()).subscribe();
      this.trackingService.getProof(id.toString()).subscribe();
    }
  }

  loadDelivery(id: number): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.deliveryService.getDeliveryById(id).subscribe({
      next: (delivery) => {
        this.delivery.set(delivery);
        this.loading.set(false);
        this.trackingService.getTimeline(delivery.trackingNumber).subscribe();
      },
      error: (err) => {
        this.errorMessage.set(this.getErrorMessage(err));
        this.loading.set(false);
      }
    });
  }

  uploadDocument(event: any): void {
    const file = event.target.files[0];
    const currentDelivery = this.delivery();
    if (file && currentDelivery) {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('deliveryId', String(currentDelivery.id));
      formData.append('documentType', 'OTHER');
      formData.append('uploadedBy', 'CUSTOMER');
      
      this.trackingService.uploadDocument(formData).subscribe({
        error: (err) => this.errorMessage.set('Failed to upload document')
      });
    }
  }

  getStatusClass(status: DeliveryStatus): string {
    return `status-${status.toLowerCase().replaceAll('_', '-')}`;
  }

  get fallbackTimeline(): TrackingEventResponse[] {
    const d = this.delivery();
    if (!d) return [];
    return [{
      id: d.id,
      deliveryId: String(d.id),
      trackingNumber: d.trackingNumber,
      status: d.status,
      location: `${d.senderAddress.city} to ${d.receiverAddress.city}`,
      description: `Current delivery status is ${d.status}.`,
      eventTime: d.updatedAt || d.createdAt,
      createdAt: d.createdAt
    }];
  }

  goBack(): void {
    this.router.navigate(['/customer/dashboard']);
  }

  private getErrorMessage(err: any): string {
    if (err?.error?.errors) return Object.values(err.error.errors).join(' ');
    return err?.error?.error || 'Failed to load delivery details.';
  }
}
