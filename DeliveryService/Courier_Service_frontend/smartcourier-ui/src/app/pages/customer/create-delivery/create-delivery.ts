import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { DeliveryService } from '../../../core/services/delivery.service';
import { CreateDeliveryRequest } from '../../../core/models/delivery.models';

@Component({
  selector: 'app-create-delivery',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './create-delivery.html',
  styleUrl: './create-delivery.css'
})
export class CreateDelivery {
  loading = false;
  errorMessage = '';
  successMessage = '';
  currentStep = 1;
  totalSteps = 3;

  delivery: CreateDeliveryRequest = {
    senderAddress: { name: '', email: '', phone: '', street: '', city: '', state: '', pincode: '' },
    receiverAddress: { name: '', email: '', phone: '', street: '', city: '', state: '', pincode: '' },
    packageDetails: {
      description: '',
      weight: 1,
      length: 1,
      breadth: 1,
      height: 1,
      serviceType: 'DOMESTIC'
    },
    pickupDate: ''
  };

  constructor(
    private deliveryService: DeliveryService,
    private router: Router
  ) {}

  nextStep(): void {
    this.errorMessage = '';
    if (!this.validateStep()) return;
    if (this.currentStep < this.totalSteps) this.currentStep++;
  }

  prevStep(): void {
    if (this.currentStep > 1) this.currentStep--;
  }

  submit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (!this.validateStep()) return;

    this.loading = true;
    this.deliveryService.createDelivery(this.buildPayload()).subscribe({
      next: (res) => {
        this.loading = false;
        this.successMessage = `Delivery created successfully. Tracking number: ${res.trackingNumber}`;
        setTimeout(() => this.router.navigate(['/customer/dashboard']), 1200);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = this.getErrorMessage(err);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/customer/dashboard']);
  }

  private buildPayload(): CreateDeliveryRequest {
    return {
      senderAddress: {
        name: this.clean(this.delivery.senderAddress.name),
        email: this.clean(this.delivery.senderAddress.email!),
        phone: this.digits(this.delivery.senderAddress.phone),
        street: this.clean(this.delivery.senderAddress.street),
        city: this.clean(this.delivery.senderAddress.city),
        state: this.clean(this.delivery.senderAddress.state),
        pincode: this.digits(this.delivery.senderAddress.pincode)
      },
      receiverAddress: {
        name: this.clean(this.delivery.receiverAddress.name),
        email: this.clean(this.delivery.receiverAddress.email!),
        phone: this.digits(this.delivery.receiverAddress.phone),
        street: this.clean(this.delivery.receiverAddress.street),
        city: this.clean(this.delivery.receiverAddress.city),
        state: this.clean(this.delivery.receiverAddress.state),
        pincode: this.digits(this.delivery.receiverAddress.pincode)
      },
      packageDetails: {
        description: this.clean(this.delivery.packageDetails.description),
        weight: Number(this.delivery.packageDetails.weight),
        length: Number(this.delivery.packageDetails.length),
        breadth: Number(this.delivery.packageDetails.breadth),
        height: Number(this.delivery.packageDetails.height),
        serviceType: this.delivery.packageDetails.serviceType
      },
      pickupDate: this.delivery.pickupDate
    };
  }

  private validateStep(): boolean {
    const sender = this.delivery.senderAddress;
    const receiver = this.delivery.receiverAddress;
    const pkg = this.delivery.packageDetails;

    if (this.currentStep === 1) {
      if (!sender.name || !sender.email || !sender.phone || !sender.street || !sender.city || !sender.state || !sender.pincode) {
        this.errorMessage = 'Please fill all sender details.';
        return false;
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.clean(sender.email!))) {
        this.errorMessage = 'Sender email is invalid.';
        return false;
      }
      if (!/^[0-9]{10}$/.test(this.digits(sender.phone))) {
        this.errorMessage = 'Sender phone must be 10 digits.';
        return false;
      }
      if (!/^[0-9]{6}$/.test(this.digits(sender.pincode))) {
        this.errorMessage = 'Sender pincode must be 6 digits.';
        return false;
      }
    }

    if (this.currentStep === 2) {
      if (!receiver.name || !receiver.email || !receiver.phone || !receiver.street || !receiver.city || !receiver.state || !receiver.pincode) {
        this.errorMessage = 'Please fill all receiver details.';
        return false;
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.clean(receiver.email!))) {
        this.errorMessage = 'Receiver email is invalid.';
        return false;
      }
      if (!/^[0-9]{10}$/.test(this.digits(receiver.phone))) {
        this.errorMessage = 'Receiver phone must be 10 digits.';
        return false;
      }
      if (!/^[0-9]{6}$/.test(this.digits(receiver.pincode))) {
        this.errorMessage = 'Receiver pincode must be 6 digits.';
        return false;
      }
    }

    if (this.currentStep === 3) {
      if (!pkg.description || !pkg.serviceType || !this.delivery.pickupDate) {
        this.errorMessage = 'Please complete package details and pickup date.';
        return false;
      }
      if (pkg.weight <= 0 || pkg.length <= 0 || pkg.breadth <= 0 || pkg.height <= 0) {
        this.errorMessage = 'Weight and dimensions must be greater than 0.';
        return false;
      }
    }

    return true;
  }

  private getErrorMessage(err: any): string {
    if (err?.error?.errors) {
      return Object.values(err.error.errors).join(' ');
    }
    return err?.error?.error || 'Failed to create delivery.';
  }

  private clean(value: string): string {
    return (value || '').trim();
  }

  private digits(value: string): string {
    return (value || '').replace(/\D/g, '');
  }
}
