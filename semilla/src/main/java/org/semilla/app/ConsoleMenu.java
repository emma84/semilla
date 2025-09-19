package org.semilla.app;

import org.semilla.app.dao.CategoryDao;
import org.semilla.app.dao.ProductDao;
import org.semilla.app.model.Category;
import org.semilla.app.model.Product;
import org.semilla.app.service.PriceService;

import java.sql.Connection;
import java.util.List;
import java.util.Scanner;

public class ConsoleMenu {
    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final PriceService priceService;

    public ConsoleMenu(ProductDao productDao, CategoryDao categoryDao, PriceService priceService) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
        this.priceService = priceService;
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== SEMILLA · MENÚ ===");
            System.out.println("1) Listar categorías");
            System.out.println("2) Listar productos");
            System.out.println("3) Crear producto");
            System.out.println("4) Recalcular PV de un producto");
            System.out.println("0) Salir");
            System.out.print("Opción: ");
            String opt = sc.nextLine().trim();

            try {
                switch (opt) {
                    case "1":
                        listCategorias();
                        break;
                    case "2":
                        listProductos();
                        break;
                    case "3":
                        createProducto(sc);
                        break;
                    case "4":
                        recalcProducto(sc);
                        break;
                    case "0":
                        System.out.println("Chau!");
                        return;
                    default:
                        System.out.println("Opción inválida.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
                ex.printStackTrace(System.out);
            }
        }
    }

    private void listCategorias() throws Exception {
        List<Category> cats = categoryDao.findAll();
        if (cats.isEmpty()) {
            System.out.println("(Sin categorías activas)");
            return;
        }
        System.out.println("\nID | Nombre              | Margen% default");
        System.out.println("-------------------------------------------");
        for (Category c : cats) {
            System.out.printf("%2d | %-19s | %.2f%n", c.id, c.nombre, c.margenPctDefault);
        }
    }

    private void listProductos() throws Exception {
        List<Product> ps = productDao.listAll();
        if (ps.isEmpty()) {
            System.out.println("(Sin productos)");
            return;
        }
        System.out.println("\nID | Nombre                       | Tipo    | Unidad | Costo  | Margen% | PV     | CatID");
        System.out.println("------------------------------------------------------------------------------------------");
        for (Product p : ps) {
            System.out.printf("%2d | %-28s | %-7s | %-6s | %-6.2f | %-7.2f | %-6.2f | %s%n",
                    p.id, p.nombre, p.tipo, p.unidadBase,
                    p.precioCosto != null ? p.precioCosto : 0.0,
                    p.margenPct != null ? p.margenPct   : 0.0,
                    p.precioVenta != null ? p.precioVenta : 0.0,
                    p.categoriaId != null ? p.categoriaId.toString() : "-");
        }
    }

    private void createProducto(Scanner sc) throws Exception {
        Product p = new Product();

        System.out.print("Nombre: ");
        p.nombre = sc.nextLine().trim();

        System.out.print("Tipo (GRANEL/UNIDAD): ");
        p.tipo = sc.nextLine().trim().toUpperCase();
        if (!"GRANEL".equals(p.tipo) && !"UNIDAD".equals(p.tipo)) {
            System.out.println("Tipo inválido.");
            return;
        }

        System.out.print("Unidad base (g/un): ");
        p.unidadBase = sc.nextLine().trim().toLowerCase();
        if (!"g".equals(p.unidadBase) && !"un".equals(p.unidadBase)) {
            System.out.println("Unidad inválida.");
            return;
        }

        System.out.print("Precio de costo: ");
        p.precioCosto = parseDoubleSafe(sc.nextLine());

        System.out.print("Margen% (0 para usar el de la categoría): ");
        p.margenPct = parseDoubleSafe(sc.nextLine());

        listCategorias();
        System.out.print("ID de categoría (vacío para ninguna): ");
        String cat = sc.nextLine().trim();
        if (!cat.isEmpty()) {
            try {
                p.categoriaId = Integer.parseInt(cat);
            } catch (NumberFormatException nfe) {
                System.out.println("CatID inválido; se deja sin categoría.");
            }
        }

        p.precioVenta = 0.0; // se recalcula

        int newId = productDao.insert(p);
        priceService.recalcAndPersist(newId);
        System.out.println("✔ Producto creado con ID=" + newId + " y PV recalculado.");
    }

    private void recalcProducto(Scanner sc) throws Exception {
        listProductos();
        System.out.print("ID del producto a recalcular: ");
        String s = sc.nextLine().trim();
        int id = Integer.parseInt(s);
        priceService.recalcAndPersist(id);
        System.out.println("✔ PV recalculado para ID=" + id);
    }

    private static double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.').trim());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
