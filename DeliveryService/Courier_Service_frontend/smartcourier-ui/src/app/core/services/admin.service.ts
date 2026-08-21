import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, timeout } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  DashboardResponse,
  DeliveryMonitorResponse,
  ExceptionResponse,
  HubRequest,
  HubResponse,
  ReportResponse
} from '../models/admin.model';
import { DeliveryStatus } from '../models/delivery.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private baseUrl = `${environment.apiUrl}/admin`;
  private requestTimeout = 2500;

  public dashboardSignal = signal<DashboardResponse | null>(null);
  public deliveriesSignal = signal<DeliveryMonitorResponse[]>([]);
  public exceptionsSignal = signal<ExceptionResponse[]>([]);
  public hubsSignal = signal<HubResponse[]>([]);
  public reportsSignal = signal<ReportResponse[]>([]);
  public loadingSignal = signal(false);

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<DashboardResponse> {
    this.loadingSignal.set(true);
    return this.http.get<DashboardResponse>(`${this.baseUrl}/dashboard`).pipe(
      timeout(this.requestTimeout),
      tap({
        next: (res) => {
          this.dashboardSignal.set(res);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  getDeliveries(): Observable<DeliveryMonitorResponse[]> {
    this.loadingSignal.set(true);
    return this.http.get<DeliveryMonitorResponse[]>(`${this.baseUrl}/deliveries`).pipe(
      timeout(this.requestTimeout),
      tap({
        next: (res) => {
          this.deliveriesSignal.set(res);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  updateDeliveryStatus(id: string, status: DeliveryStatus): Observable<DeliveryMonitorResponse> {
    return this.http.put<DeliveryMonitorResponse>(`${this.baseUrl}/deliveries/${id}/status?status=${status}`, {}).pipe(
      timeout(this.requestTimeout),
      tap((updated) => {
        this.deliveriesSignal.update(deliveries => 
          deliveries.map(d => d.deliveryId === updated.deliveryId ? updated : d)
        );
      })
    );
  }

  getExceptions(): Observable<ExceptionResponse[]> {
    this.loadingSignal.set(true);
    return this.http.get<ExceptionResponse[]>(`${this.baseUrl}/exceptions/all`).pipe(
      timeout(this.requestTimeout),
      tap({
        next: (res) => {
          this.exceptionsSignal.set(res);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  resolveException(id: number, remarks: string, resolvedBy: string): Observable<ExceptionResponse> {
    return this.http.put<ExceptionResponse>(`${this.baseUrl}/exceptions/${id}/resolve`, { remarks, resolvedBy }).pipe(
      timeout(this.requestTimeout),
      tap((resolved) => {
        this.exceptionsSignal.update(exceptions => 
          exceptions.map(e => e.id === resolved.id ? resolved : e)
        );
      })
    );
  }

  getHubs(): Observable<HubResponse[]> {
    this.loadingSignal.set(true);
    return this.http.get<HubResponse[]>(`${this.baseUrl}/hubs/all`).pipe(
      timeout(this.requestTimeout),
      tap({
        next: (res) => {
          this.hubsSignal.set(res);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }

  createHub(data: HubRequest): Observable<HubResponse> {
    return this.http.post<HubResponse>(`${this.baseUrl}/hubs`, data).pipe(
      timeout(this.requestTimeout),
      tap((hub) => {
        this.hubsSignal.update(hubs => [...hubs, hub]);
      })
    );
  }

  deactivateHub(id: number): Observable<HubResponse> {
    return this.http.delete<HubResponse>(`${this.baseUrl}/hubs/${id}`).pipe(
      timeout(this.requestTimeout),
      tap((hub) => {
        this.hubsSignal.update(hubs => 
          hubs.map(h => h.id === hub.id ? hub : h)
        );
      })
    );
  }

  generateReport(fromDate: string, toDate: string, reportType: string, generatedBy: string): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(
      `${this.baseUrl}/reports/generate?fromDate=${fromDate}&toDate=${toDate}&reportType=${reportType}&generatedBy=${encodeURIComponent(generatedBy)}`,
      {}
    ).pipe(
      timeout(this.requestTimeout),
      tap((report) => {
        this.reportsSignal.update(reports => [report, ...reports]);
      })
    );
  }

  getReports(reportType: string): Observable<ReportResponse[]> {
    this.loadingSignal.set(true);
    return this.http.get<ReportResponse[]>(`${this.baseUrl}/reports?reportType=${reportType}`).pipe(
      timeout(this.requestTimeout),
      tap({
        next: (res) => {
          this.reportsSignal.set(res);
          this.loadingSignal.set(false);
        },
        error: () => this.loadingSignal.set(false)
      })
    );
  }
}
