import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClienteService, Cliente } from '../../services/cliente';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clientes.html',
  styleUrl: './clientes.css',
})
export class ClientesComponent implements OnInit {
  clientes: Cliente[] = [];
  filtro = '';
  isModalOpen = false;
  isEditMode = false;
  ver: Cliente | null = null;
  form: Cliente = { nombre: '', dni: '', telefono: '', direccion: '', email: '' };

  // Paginación: 6 por página
  pagina = 1;
  porPagina = 6;

  constructor(private clienteService: ClienteService, private toast: ToastService, private cdr: ChangeDetectorRef, public auth: AuthService) {}

  ngOnInit(): void { this.cargar(); }

  cargar() {
    this.clienteService.listar().subscribe({
      next: (d) => { this.clientes = d; this.cdr.detectChanges(); },
      error: () => this.toast.error('Error al cargar clientes (¿auth-service 8083 apagado?)'),
    });
  }

  get filtrados(): Cliente[] {
    if (!this.filtro.trim()) return this.clientes;
    const f = this.filtro.toLowerCase();
    return this.clientes.filter((c) =>
      c.nombre.toLowerCase().includes(f) ||
      (c.dni ?? '').includes(f) ||
      c.email.toLowerCase().includes(f) ||
      (c.telefono ?? '').includes(f));
  }

  get total(): number { return this.clientes.length; }
  get activos(): number { return this.clientes.filter((c) => c.status === 'CONFIRMADO').length; }
  get conEmail(): number { return this.clientes.filter((c) => !!c.email).length; }
  get conTelefono(): number { return this.clientes.filter((c) => !!c.telefono).length; }

  // ===== Paginación =====
  get totalPaginas(): number { return Math.max(1, Math.ceil(this.filtrados.length / this.porPagina)); }
  get paginas(): number[] { return Array.from({ length: this.totalPaginas }, (_, i) => i + 1); }
  get paginados(): Cliente[] {
    const ini = (this.pagina - 1) * this.porPagina;
    return this.filtrados.slice(ini, ini + this.porPagina);
  }
  irA(p: number) { this.pagina = Math.min(Math.max(1, p), this.totalPaginas); }
  anterior() { this.irA(this.pagina - 1); }
  siguiente() { this.irA(this.pagina + 1); }

  abrir(c?: Cliente) {
    this.isModalOpen = true;
    this.ver = null;
    if (c) { this.isEditMode = true; this.form = { ...c }; }
    else { this.isEditMode = false; this.form = { nombre: '', dni: '', telefono: '', direccion: '', email: '' }; }
  }
  cerrar() { this.isModalOpen = false; this.ver = null; }

  // Solo dígitos y longitud máxima (DNI 8, Teléfono 9)
  soloNumeros(ev: Event, campo: 'dni' | 'telefono', max: number) {
    const input = ev.target as HTMLInputElement;
    const limpio = input.value.replace(/\D/g, '').slice(0, max);
    input.value = limpio;
    this.form[campo] = limpio;
  }

  guardar() {
    if (!this.form.nombre.trim() || !this.form.email.trim()) { this.toast.warning('Nombre y email obligatorios'); return; }
    if (this.form.dni && !/^\d{8}$/.test(this.form.dni)) { this.toast.warning('DNI debe tener 8 dígitos'); return; }
    if (this.form.telefono && !/^\d{9}$/.test(this.form.telefono)) { this.toast.warning('El teléfono debe tener 9 dígitos'); return; }
    if (this.isEditMode && this.form.customerId != null) {
      this.clienteService.actualizar(this.form.customerId, this.form).subscribe({
        next: () => { this.toast.success('Cliente actualizado'); this.cargar(); this.cerrar(); },
        error: (e) => { console.error(e); this.toast.error('Error al actualizar (email duplicado?)'); },
      });
    } else {
      this.clienteService.crear(this.form).subscribe({
        next: () => { this.toast.success('Cliente registrado'); this.cargar(); this.cerrar(); },
        error: (e) => { console.error(e); this.toast.error('Error al registrar (email duplicado?)'); },
      });
    }
  }

  eliminar(id?: number) {
    if (!id) return;
    if (confirm('¿Eliminar cliente?')) {
      this.clienteService.eliminar(id).subscribe({
        next: () => { this.toast.success('Cliente eliminado'); this.cargar(); },
        error: () => this.toast.error('No se pudo eliminar'),
      });
    }
  }
}
