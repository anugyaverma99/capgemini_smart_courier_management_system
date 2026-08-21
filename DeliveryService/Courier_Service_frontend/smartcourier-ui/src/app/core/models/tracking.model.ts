import { DeliveryStatus } from './delivery.models';

export interface TrackingEventRequest {
  deliveryId: string;
  trackingNumber: string;
  status: DeliveryStatus;
  location: string;
  description?: string;
  remarks?: string;
  updatedBy?: string;
}

export interface TrackingEventResponse {
  id: number;
  deliveryId: string;
  trackingNumber: string;
  status: DeliveryStatus;
  location: string;
  description?: string;
  remarks?: string;
  updatedBy?: string;
  eventTime: string;
  createdAt: string;
}

export interface DocumentResponse {
  id: number;
  deliveryId: string;
  fileName: string;
  fileType: string;
  uploadedAt: string;
}

export interface DeliveryProofResponse {
  id: number;
  deliveryId: string;
  receiverName: string;
  notes: string;
  proofImageUrl: string;
  deliveredAt: string;
}
