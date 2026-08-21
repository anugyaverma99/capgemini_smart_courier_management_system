import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reports.html',
  styleUrl: './reports.css'
})
export class AdminReports implements OnInit {
  fromDate = signal('');
  toDate = signal('');
  reportType = signal('DAILY');
  
  errorMessage = signal('');
  successMessage = signal('');

  reports = this.adminService.reportsSignal;
  loading = this.adminService.loadingSignal;

  constructor(
    private adminService: AdminService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const today = new Date().toISOString().slice(0, 10);
    this.fromDate.set(today);
    this.toDate.set(today);
    this.loadReports();
  }

  loadReports(): void {
    this.errorMessage.set('');
    this.adminService.getReports(this.reportType()).subscribe({
      error: (err) => this.errorMessage.set(err?.error?.error || 'Failed to load reports.')
    });
  }

  generate(): void {
    const generatedBy = this.authService.getEmail() || 'admin';
    this.adminService.generateReport(this.fromDate(), this.toDate(), this.reportType(), generatedBy).subscribe({
      next: () => { 
        this.successMessage.set('Report generated successfully.'); 
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err) => this.errorMessage.set(err?.error?.error || 'Failed to generate report.')
    });
  }

  updateFromDate(val: string) { this.fromDate.set(val); }
  updateToDate(val: string) { this.toDate.set(val); }
  updateReportType(val: string) { 
    this.reportType.set(val); 
    this.loadReports();
  }
}
