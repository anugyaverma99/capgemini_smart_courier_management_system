import { DeliveryStatus } from './delivery.models';

export interface DashboardResponse {
  totalDeliveries: number;
  exceptions: number;
  deliveredToday: number;
  inTransit: number;
  outForDelivery: number;
  activeHubs: number;
  liveDeliveryCount: number;
  totalTrackingEvents: number;
}

export interface DeliveryMonitorResponse {
  deliveryId: string;
  trackingNumber: string;
  customerName: string;
  senderCity: string;
  receiverCity: string;
  currentStatus: DeliveryStatus;
  assignedHub: string;
  lastUpdated: string;
  liveSenderName?: string;
  liveReceiverName?: string;
  latestTrackingStatus?: string;
  latestTrackingLocation?: string;
}

export interface ExceptionResponse {
  id: number;
  deliveryId: string;
  trackingNumber: string;
  exceptionStatus: DeliveryStatus;
  resolutionStatus: string;
  reason: string;
  remarks?: string;
  raisedAt: string;
  resolvedAt?: string;
  resolvedBy?: string;
}

export interface HubRequest {
  name: string;
  city: string;
  state: string;
  pincode: string;
  contactNumber: string;
}

export interface HubResponse {
  id: number;
  name: string;
  city: string;
  state: string;
  pincode: string;
  contactNumber: string;
  active: boolean;
  createdAt?: string;
}

export interface ReportResponse {
  id: number;
  reportType: string;
  fromDate: string;
  toDate: string;
  totalDeliveries: number;
  deliveredCount: number;
  failedCount: number;
  delayedCount: number;
  returnedCount: number;
  liveDeliveryCount: number;
  totalTrackingEvents: number;
  generatedBy: string;
  generatedAt: string;
}
