import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, timeout } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { DeliveryProofResponse, DocumentResponse, TrackingEventRequest, TrackingEventResponse } from '../models/tracking.model';

@Injectable({ providedIn: 'root' })
export class TrackingService {
  private baseUrl = `${environment.apiUrl}/tracking`;
  private requestTimeout = 2500;

  public timelineSignal = signal<TrackingEventResponse[]>([]);
  public documentsSignal = signal<DocumentResponse[]>([]);
  public proofSignal = signal<DeliveryProofResponse | null>(null);
  public loadingSignal = signal(false);

  constructor(private http: HttpClient) {}

  getTimeline(trackingNumber: string): Observable<TrackingEventResponse[]> {
    this.loadingSignal.set(true);
    return this.http.get<TrackingEventResponse[]>(`${this.baseUrl}/${trackingNumber}`).pipe(
      timeout(this.requestTimeout),
      tap({
        next: (res) => {
          this.timelineSignal.set(res);
          this.loadingSignal.set(false);
        },
        error: () => {
          this.timelineSignal.set([]);
          this.loadingSignal.set(false);
        }
      })
    );
  }

  addEvent(data: TrackingEventRequest): Observable<TrackingEventResponse> {
    return this.http.post<TrackingEventResponse>(`${this.baseUrl}/events`, data).pipe(
      timeout(this.requestTimeout),
      tap((res) => {
        this.timelineSignal.update(events => [res, ...events]);
      })
    );
  }

  getLatest(trackingNumber: string): Observable<TrackingEventResponse> {
    return this.http.get<TrackingEventResponse>(`${this.baseUrl}/${trackingNumber}/latest`).pipe(timeout(this.requestTimeout));
  }

  getDocuments(deliveryId: string): Observable<DocumentResponse[]> {
    this.loadingSignal.set(true);
    return this.http.get<DocumentResponse[]>(`${this.baseUrl}/documents/${deliveryId}`).pipe(
      timeout(this.requestTimeout),
      tap({
        next: (res) => {
          this.documentsSignal.set(res);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  uploadDocument(formData: FormData): Observable<DocumentResponse> {
    return this.http.post<DocumentResponse>(`${this.baseUrl}/documents/upload`, formData).pipe(
      timeout(6000),
      tap((doc) => {
        this.documentsSignal.update(docs => [...docs, doc]);
      })
    );
  }

  submitProof(formData: FormData): Observable<DeliveryProofResponse> {
    return this.http.post<DeliveryProofResponse>(`${this.baseUrl}/proof`, formData).pipe(
      timeout(6000),
      tap((proof) => {
        this.proofSignal.set(proof);
      })
    );
  }

  getProof(deliveryId: string): Observable<DeliveryProofResponse> {
    this.loadingSignal.set(true);
    return this.http.get<DeliveryProofResponse>(`${this.baseUrl}/${deliveryId}/proof`).pipe(
      timeout(this.requestTimeout),
      tap({
        next: (res) => {
          this.proofSignal.set(res);
          this.loadingSignal.set(false);
        },
        error: () => {
          this.proofSignal.set(null);
          this.loadingSignal.set(false);
        }
      })
    );
  }
}
