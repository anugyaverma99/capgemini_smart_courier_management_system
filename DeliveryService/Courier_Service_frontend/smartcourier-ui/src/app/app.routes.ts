import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', loadComponent: () => import('./pages/auth/login/login').then(m => m.Login) },
  { path: 'signup', loadComponent: () => import('./pages/auth/signup/signup').then(m => m.Signup) },
  { path: 'auth/login', redirectTo: 'login' },
  { path: 'auth/signup', redirectTo: 'signup' },
  { path: 'track', loadComponent: () => import('./pages/customer/track-delivery/track-delivery').then(m => m.TrackDelivery) },
  { path: 'track/:trackingNumber', loadComponent: () => import('./pages/customer/track-delivery/track-delivery').then(m => m.TrackDelivery) },
  {
    path: 'customer',
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./pages/customer/dashboard/dashboard').then(m => m.Dashboard) },
      { path: 'deliveries', loadComponent: () => import('./pages/customer/deliveries/deliveries').then(m => m.CustomerDeliveries) },
      { path: 'create-delivery', loadComponent: () => import('./pages/customer/create-delivery/create-delivery').then(m => m.CreateDelivery) },
      { path: 'delivery/:id', loadComponent: () => import('./pages/customer/delivery-detail/delivery-detail').then(m => m.DeliveryDetail) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./pages/admin/dashboard/dashboard').then(m => m.AdminDashboard) },
      { path: 'deliveries', loadComponent: () => import('./pages/admin/deliveries/deliveries').then(m => m.AdminDeliveries) },
      { path: 'exceptions', loadComponent: () => import('./pages/admin/exceptions/exceptions').then(m => m.AdminExceptions) },
      { path: 'hubs', loadComponent: () => import('./pages/admin/hubs/hubs').then(m => m.AdminHubs) },
      { path: 'reports', loadComponent: () => import('./pages/admin/reports/reports').then(m => m.AdminReports) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '' }
];
