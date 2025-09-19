package org.semilla.app.model;

public class Product {
    public Integer id;
    public String  nombre;
    public String  tipo;        // GRANEL / UNIDAD
    public String  unidadBase;  // g / un
    public String  codigoBarra; // editable (pistola)
    public Double  precioCosto;
    public Double  margenPct;   // si 0 -> usa categoría
    public Double  precioVenta;
    public Integer categoriaId;
    public Integer proveedorId; // <-- NUEVO
}

