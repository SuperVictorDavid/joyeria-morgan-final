import { agruparVentas, Venta } from './venta';

describe('agruparVentas', () => {
  it('junta en una sola boleta las filas de un checkout con varios productos', () => {
    const ventas: Venta[] = [
      {
        saleId: 1,
        numeroBoleta: 'B001-000001',
        productId: 2,
        quantity: 1,
        customerId: 3,
        vendedor: 'admin',
        total: 850,
        fechaVenta: '2026-10-22T20:00:00',
      },
      {
        saleId: 2,
        numeroBoleta: 'B001-000001',
        productId: 4,
        quantity: 2,
        customerId: 3,
        vendedor: 'admin',
        total: 1000,
        fechaVenta: '2026-10-22T20:00:00',
      },
      {
        saleId: 3,
        numeroBoleta: 'B001-000002',
        productId: 10,
        quantity: 1,
        customerId: 4,
        vendedor: 'vendedor',
        total: 120,
        fechaVenta: '2026-10-22T20:01:00',
      },
    ];

    const grupos = agruparVentas(ventas);

    expect(grupos).toHaveLength(2);
    expect(grupos[0].numeroBoleta).toBe('B001-000001');
    expect(grupos[0].items).toHaveLength(2);
    expect(grupos[0].total).toBe(1850);
    expect(grupos[0].customerId).toBe(3);
    expect(grupos[1].numeroBoleta).toBe('B001-000002');
    expect(grupos[1].items).toHaveLength(1);
    expect(grupos[1].total).toBe(120);
  });

  it('mantiene separadas las ventas con boletas distintas', () => {
    const grupos = agruparVentas([
      { saleId: 1, numeroBoleta: 'B001-000010', productId: 2, quantity: 1, customerId: 3, total: 850 },
      { saleId: 2, numeroBoleta: 'B001-000011', productId: 4, quantity: 1, customerId: 3, total: 500 },
    ]);

    expect(grupos).toHaveLength(2);
    expect(grupos[0].total).toBe(850);
    expect(grupos[1].total).toBe(500);
  });
});
