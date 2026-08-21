import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Navbar } from '../../shared/components/navbar/navbar';
import { Footer } from '../../shared/components/footer/footer';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, FormsModule, Navbar, Footer],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {
  trackingNumber = '';

  constructor(private router: Router) {}

  trackPackage() {
    if (this.trackingNumber.trim()) {
      this.router.navigate(['/customer/track'], { queryParams: { tn: this.trackingNumber } });
    }
  }
}