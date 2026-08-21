import { CommonModule } from '@angular/common';
import { Component, OnInit, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HubRequest, HubResponse } from '../../../core/models/admin.model';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin-hubs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './hubs.html',
  styleUrl: './hubs.css'
})
export class AdminHubs implements OnInit {
  hubs = this.adminService.hubsSignal;
  loading = this.adminService.loadingSignal;

  form = signal<HubRequest>({ name: '', city: '', state: '', pincode: '', contactNumber: '' });
  errorMessage = signal('');
  successMessage = signal('');

  activeHubsCount = computed(() => this.hubs().filter(h => h.active).length);

  constructor(private adminService: AdminService) {}

  ngOnInit(): void { 
    this.loadHubs(); 
  }

  loadHubs(): void {
    this.errorMessage.set('');
    this.adminService.getHubs().subscribe({
      error: (err) => this.errorMessage.set(err?.error?.error || 'Failed to load hubs.')
    });
  }

  createHub(): void {
    const f = this.form();
    if (!f.name || !f.city || !f.state || !f.pincode || !f.contactNumber) {
      this.errorMessage.set('Please fill all hub fields.');
      return;
    }
    this.adminService.createHub(f).subscribe({
      next: () => {
        this.successMessage.set('Hub created successfully.');
        this.form.set({ name: '', city: '', state: '', pincode: '', contactNumber: '' });
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err) => this.errorMessage.set(err?.error?.error || 'Failed to create hub.')
    });
  }

  deactivate(hub: HubResponse): void {
    this.adminService.deactivateHub(hub.id).subscribe({
      next: () => { 
        this.successMessage.set('Hub deactivated.'); 
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err) => this.errorMessage.set(err?.error?.error || 'Failed to deactivate hub.')
    });
  }

  updateForm(field: keyof HubRequest, value: string) {
    this.form.update(f => ({ ...f, [field]: value }));
  }
}
