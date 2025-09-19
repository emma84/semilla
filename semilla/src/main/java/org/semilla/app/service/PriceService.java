package org.semilla.app.service;

import org.semilla.app.dao.CategoryDao;
import org.semilla.app.dao.ProductDao;
import org.semilla.app.model.Category;
import org.semilla.app.model.Product;

public class PriceService {
    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final String roundingRule;

    public PriceService(ProductDao productDao, CategoryDao categoryDao, String roundingRule) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
        this.roundingRule = roundingRule != null ? roundingRule : "none";
    }

    public double calcPrecioVenta(double costo, double margenPct) {
        double pv = costo * (1.0 + (margenPct / 100.0));
        return applyRounding(pv);
    }

    public void recalcAndPersist(int productId) throws Exception {
        Product p = productDao.findById(productId);
        if (p == null) throw new IllegalArgumentException("Producto no existe: " + productId);

        double margen = (p.margenPct != null && p.margenPct > 0) ? p.margenPct : 0;
        if (margen == 0 && p.categoriaId != null) {
            Category c = categoryDao.findById(p.categoriaId);
            if (c != null) margen = c.margenPctDefault;
        }
        double pv = calcPrecioVenta(p.precioCosto != null ? p.precioCosto : 0, margen);
        productDao.updatePrecioVenta(productId, pv);
    }

    private double applyRounding(double value) {
        switch (roundingRule) {
            case "ceil_peso":   return Math.ceil(value);
            case "ceil_10":     return Math.ceil(value / 10.0) * 10.0;
            case "round_2_dec": return Math.round(value * 100.0) / 100.0;
            case "none":
            default:            return value;
        }
    }
}
