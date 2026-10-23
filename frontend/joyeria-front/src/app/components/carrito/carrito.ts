import { Component, OnInit, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { VentaService } from '../../services/venta';
import { JoyaService, Joya } from '../../services/joya';
import { ClienteService, Cliente } from '../../services/cliente';
import { CarritoStore, CarritoItem } from '../../services/carrito-store';
import { AuthService } from '../../services/auth';
import { ToastService } from '../../services/toast.service';
import { mensajeErrorApi } from '../../services/api-error';

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './carrito.html',
  styleUrl: './carrito.css',
})
export class CarritoComponent implements OnInit, OnDestroy {
  joyas: Joya[] = [];
  clientes: Cliente[] = [];
  carrito: CarritoItem[] = [];
  private sub?: Subscription;

  // Form: cliente
  clienteId: number | null = null;

  // Form: joya
  joyaId = 0;
  cantidad = 1;

  procesando = false;

  constructor(
    private ventas: VentaService,
    private joyaService: JoyaService,
    private clienteService: ClienteService,
    private authService: AuthService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
    public store: CarritoStore,
  ) {}

  private get vendedor(): string {
    return this.authService.getCurrentUser()?.username ?? 'admin';
  }

  ngOnInit(): void {
    this.sub = this.store.items$.subscribe((items) => {
      this.carrito = items;
      this.cdr.detectChanges();
    });
    this.joyaService.listar().subscribe({
      next: (d) => { this.joyas = d ?? []; this.cdr.detectChanges(); },
      error: () => this.toast.error('Error al cargar joyas'),
    });
    this.clienteService.listar().subscribe({
      next: (d) => { this.clientes = d ?? []; this.cdr.detectChanges(); },
      error: () => this.toast.error('Error al cargar clientes'),
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  etiquetaCliente(c: Cliente): string {
    return `${c.nombre} - DNI: ${c.dni ?? '—'}`;
  }

  // ===== Joya seleccionada =====
  get joyaSeleccionada(): Joya | undefined {
    return this.joyas.find((j) => j.id === Number(this.joyaId));
  }

  get precioUnitario(): number {
    return this.joyaSeleccionada?.price ?? 0;
  }

  get stockMax(): number {
    return this.joyaSeleccionada?.stock ?? 99;
  }

  get subtotalPreview(): number {
    return this.precioUnitario * (this.cantidad || 0);
  }

  onJoyaChange(): void {
    this.cantidad = 1;
  }

  get total(): number {
    return this.store.total();
  }

  nombreJoya(j: Joya): string {
    return j.descripcion || j.name || `Joya ${j.id ?? ''}`;
  }

  agregar() {
    const joya = this.joyaSeleccionada;
    if (!joya) { this.toast.warning('Elige una joya'); return; }
    if (this.cantidad < 1) { this.toast.warning('Cantidad mínima 1'); return; }
    if ((joya.stock ?? 0) < this.cantidad) { this.toast.warning('Stock insuficiente'); return; }
    this.store.add(joya, this.cantidad);
    this.toast.success('Producto agregado al carrito');
    this.joyaId = 0;
    this.cantidad = 1;
  }

  quitar(index: number) { this.store.remove(index); }
  vaciar() { this.store.clear(); }

  procesar() {
    if (!this.carrito.length) { this.toast.warning('Carrito vacío'); return; }
    if (this.clienteId == null) { this.toast.warning('Selecciona un cliente'); return; }
    const clienteId = this.clienteId;
    this.procesando = true;
    const items = this.carrito.map((i) => ({ productId: i.joya.id!, quantity: i.cantidad }));

    this.ventas.crearLote({ customerId: clienteId, vendedor: this.vendedor, items }).subscribe({
      next: (r) => {
        this.procesando = false;
        const cantidad = r.items?.length ?? items.length;
        this.toast.success(`Boleta ${r.numeroBoleta}: ${cantidad} producto(s), Total S/ ${(r.total ?? 0).toFixed(2)}`);
        this.vaciar();
        this.joyaService.listar().subscribe({ next: (d) => { this.joyas = d ?? []; this.cdr.detectChanges(); } });
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.procesando = false;
        this.toast.error(mensajeErrorApi(err, 'ventas-service (8082)'));
        this.cdr.detectChanges();
      },
    });
  }
}
