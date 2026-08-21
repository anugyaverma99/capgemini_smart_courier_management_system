import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, timeout } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { CreateDeliveryRequest, DeliveryResponse, DeliveryStatus, UpdateStatusRequest } from '../models/delivery.models';

@Injectable({ providedIn: 'root' })
export class DeliveryService {
  private baseUrl = `${environment.apiUrl}/deliveries`;
  private requestTimeout = 3000;

  public deliveriesSignal = signal<DeliveryResponse[]>([]);
  public loadingSignal = signal(false);
  public activeDeliveriesSignal = computed(() => 
    this.deliveriesSignal().filter(d => !['DELIVERED', 'FAILED', 'RETURNED'].includes(d.status))
  );

  constructor(private http: HttpClient) {}

  getMyDeliveries(): Observable<DeliveryResponse[]> {
    this.loadingSignal.set(true);
    return this.http.get<DeliveryResponse[]>(`${this.baseUrl}/my`).pipe(
      timeout(this.requestTimeout),
      tap({
        next: (deliveries) => {
          this.deliveriesSignal.set(deliveries);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  getDeliveryById(id: number): Observable<DeliveryResponse> {
    return this.http.get<DeliveryResponse>(`${this.baseUrl}/${id}`).pipe(timeout(this.requestTimeout));
  }

  trackByNumber(trackingNumber: string): Observable<DeliveryResponse> {
    return this.http.get<DeliveryResponse>(`${this.baseUrl}/track/${trackingNumber}`).pipe(timeout(this.requestTimeout));
  }

  createDelivery(data: CreateDeliveryRequest): Observable<DeliveryResponse> {
    return this.http.post<DeliveryResponse>(this.baseUrl, data).pipe(
      timeout(this.requestTimeout),
      tap((newDelivery) => {
        this.deliveriesSignal.update(deliveries => [...deliveries, newDelivery]);
      })
    );
  }

  getAllDeliveries(): Observable<DeliveryResponse[]> {
    this.loadingSignal.set(true);
    return this.http.get<DeliveryResponse[]>(this.baseUrl).pipe(
      timeout(this.requestTimeout),
      tap({
        next: (deliveries) => {
          this.deliveriesSignal.set(deliveries);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  updateStatus(id: number, status: DeliveryStatus): Observable<DeliveryResponse> {
    const payload: UpdateStatusRequest = { status };
    return this.http.put<DeliveryResponse>(`${this.baseUrl}/${id}/status`, payload).pipe(
      timeout(this.requestTimeout),
      tap((updatedDelivery) => {
        this.deliveriesSignal.update(deliveries => 
          deliveries.map(d => d.id === updatedDelivery.id ? updatedDelivery : d)
        );
      })
    );
  }
}
