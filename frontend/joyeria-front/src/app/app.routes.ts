import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login';
import { LayoutComponent } from './components/layout/layout';
import { ReportesComponent } from './components/reportes/reportes';
import { ClientesComponent } from './components/clientes/clientes';
import { InventarioComponent } from './components/inventario/inventario';
import { VentasComponent } from './components/ventas/ventas';
import { CarritoComponent } from './components/carrito/carrito';
import { DashboardComponent } from './components/dashboard/dashboard';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/role.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'inicio', pathMatch: 'full' },
      { path: 'inicio', component: DashboardComponent },
      { path: 'productos', component: InventarioComponent },
      { path: 'inventario', component: InventarioComponent },
      { path: 'clientes', component: ClientesComponent },
      { path: 'ventas', component: VentasComponent },
      { path: 'historial-ventas', component: VentasComponent },
      { path: 'carrito', component: CarritoComponent },
      { path: 'reportes', component: ReportesComponent, canActivate: [adminGuard] },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
