export type DeliveryStatus =
  | 'DRAFT' | 'BOOKED' | 'PICKED_UP' | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'DELAYED' | 'FAILED' | 'RETURNED';

export type ServiceType = 'DOMESTIC' | 'EXPRESS' | 'INTERNATIONAL';

export interface AddressDto {
  name: string;
  email?: string;
  phone: string;
  street: string;
  city: string;
  state: string;
  pincode: string;
}

export interface PackageDto {
  description: string;
  weight: number;
  length: number;
  breadth: number;
  height: number;
  serviceType: ServiceType;
}

export interface CreateDeliveryRequest {
  senderAddress: AddressDto;
  receiverAddress: AddressDto;
  packageDetails: PackageDto;
  pickupDate: string;
}

export interface DeliveryResponse {
  id: number;
  trackingNumber: string;
  customerId: string;
  status: DeliveryStatus;
  courierCharge: number;
  pickupDate: string;
  createdAt: string;
  updatedAt: string;
  senderAddress: AddressDto;
  receiverAddress: AddressDto;
  packageDetails: PackageDto;
}

export interface UpdateStatusRequest {
  status: DeliveryStatus;
}
