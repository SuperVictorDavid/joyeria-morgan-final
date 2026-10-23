import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { VentaService, Venta } from '../../services/venta';
import { JoyaService, Joya } from '../../services/joya';
import { ClienteService, Cliente } from '../../services/cliente';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth';
import { mensajeErrorApi } from '../../services/api-error';

@Component({
  selector: 'app-ventas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './ventas.html',
  styleUrl: './ventas.css',
})
export class VentasComponent implements OnInit {
  ventas: Venta[] = [];
  filtradas: Venta[] = [];
  joyas: Joya[] = [];
  clientesInfo: Cliente[] = [];
  detalle: any = null;

  // Filtros por rango de fecha y cliente
  fechaDesde = '';
  fechaHasta = '';
  filtroCliente = 'Todos';

  // Paginación: 6 por página
  pagina = 1;
  porPagina = 6;

  constructor(
    private ventaService: VentaService,
    private joyaService: JoyaService,
    private clienteService: ClienteService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
    public auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.joyaService.listar().subscribe({ next: (d) => (this.joyas = d ?? []) });
    this.clienteService.listar().subscribe({ next: (d) => { this.clientesInfo = d ?? []; this.cdr.detectChanges(); } });
  }

  cargar() {
    this.ventaService.listar().subscribe({
      next: (d) => {
        this.ventas = d ?? [];
        this.filtradas = [...this.ventas];
        this.pagina = 1;
        this.cdr.detectChanges();
      },
      error: () => this.toast.error('Error al cargar ventas'),
    });
  }

  // ===== Stats =====
  // Fecha local YYYY-MM-DD (evita el desfase UTC)
  private hoyLocal(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  get ventasHoy(): number {
    const hoy = this.hoyLocal();
    return this.ventas.filter((v) => v.fechaVenta?.slice(0, 10) === hoy).length;
  }

  get totalFiltradas(): number {
    return this.filtradas.length;
  }

  get montoTotal(): number {
    return this.filtradas.reduce((a, v) => a + (v.total ?? 0), 0);
  }

  get promedio(): number {
    return this.totalFiltradas ? this.montoTotal / this.totalFiltradas : 0;
  }

  // ===== Paginación =====
  get totalPaginas(): number { return Math.max(1, Math.ceil(this.filtradas.length / this.porPagina)); }
  get paginas(): number[] { return Array.from({ length: this.totalPaginas }, (_, i) => i + 1); }
  get paginadas(): Venta[] {
    const ini = (this.pagina - 1) * this.porPagina;
    return this.filtradas.slice(ini, ini + this.porPagina);
  }
  irA(p: number) { this.pagina = Math.min(Math.max(1, p), this.totalPaginas); }
  anterior() { this.irA(this.pagina - 1); }
  siguiente() { this.irA(this.pagina + 1); }

  get clienteIds(): number[] {
    const set = new Set<number>();
    this.ventas.forEach((v) => {
      if (v.customerId != null) set.add(v.customerId);
    });
    return [...set].sort((a, b) => a - b);
  }

  // ===== Filtros =====
  filtrar() {
    this.filtradas = this.ventas.filter((v) => {
      const matchCliente = this.filtroCliente === 'Todos' || String(v.customerId) === this.filtroCliente;
      const fv = v.fechaVenta ? v.fechaVenta.slice(0, 10) : '';
      const matchDesde = !this.fechaDesde || (!!fv && fv >= this.fechaDesde);
      const matchHasta = !this.fechaHasta || (!!fv && fv <= this.fechaHasta);
      return matchCliente && matchDesde && matchHasta;
    });
    this.pagina = 1;
    this.cdr.detectChanges();
  }

  limpiar() {
    this.fechaDesde = '';
    this.fechaHasta = '';
    this.filtroCliente = 'Todos';
    this.filtradas = [...this.ventas];
    this.pagina = 1;
    this.cdr.detectChanges();
  }

  nombreJoya(productId?: number): string {
    const j = this.joyas.find((x) => x.id === productId);
    if (!j) return `Joya ${productId ?? '—'}`;
    return j.descripcion || j.name || `Joya ${productId ?? '—'}`;
  }

  nombreCliente(customerId?: number | null): string {
    if (customerId == null) return '—';
    const c = this.clientesInfo.find((x) => x.customerId === customerId);
    if (!c) return `Cliente ${customerId}`;
    return c.dni ? `${c.nombre} (DNI: ${c.dni})` : c.nombre;
  }

  verDetalle(id?: number) {
    if (!id) return;
    this.ventaService.detalle(id).subscribe({
      next: (d) => {
        this.detalle = d;
        this.cdr.detectChanges();
      },
      error: (err) => this.toast.error(mensajeErrorApi(err, 'ventas-service (8082)')),
    });
  }

  cerrarDetalle() {
    this.detalle = null;
  }

  imprimir() {
    window.print();
  }

  eliminar(id?: number) {
    if (!id) return;
    if (confirm('¿Eliminar venta?')) {
      this.ventaService.eliminar(id).subscribe({
        next: () => {
          this.toast.success('Venta eliminada');
          this.cargar();
        },
        error: (err) => this.toast.error('No se pudo eliminar: ' + mensajeErrorApi(err, 'ventas-service (8082)')),
      });
    }
  }
}
