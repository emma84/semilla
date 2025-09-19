package org.semilla.app.model;

public class Category {
    public Integer id;
    public String  nombre;
    public double  margenPctDefault;

    @Override public String toString() { return id + " - " + nombre; }
}
