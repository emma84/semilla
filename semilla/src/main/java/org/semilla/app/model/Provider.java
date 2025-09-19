package org.semilla.app.model;

public class Provider {
    public Integer id;
    public String  nombre;
    public String  cuit;
    public String  telefono;
    public String  email;
    public String  direccion;
    public Integer activo; // 1/0

    // --- GETTERS que usa PropertyValueFactory ---
    public Integer getId()        { return id; }
    public String  getNombre()    { return nombre; }
    public String  getCuit()      { return cuit; }
    public String  getTelefono()  { return telefono; }
    public String  getEmail()     { return email; }
    public String  getDireccion() { return direccion; }
    public Integer getActivo()    { return activo; }

    @Override public String toString() { return nombre; }
}
