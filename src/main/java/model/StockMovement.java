package model;

import java.time.LocalDate;

public class StockMovement {
    private int id;
    private int productId;
    private String type; // ENTREE / SORTIE
    private int quantite;
    private LocalDate dateMouvement;

    public StockMovement(int id, int productId, String type, int quantite, LocalDate dateMouvement) {
        this.id = id;
        this.productId = productId;
        this.type = type;
        this.quantite = quantite;
        this.dateMouvement = dateMouvement;
    }

    public StockMovement(int productId, String type, int quantite, LocalDate dateMouvement) {
        this.productId = productId;
        this.type = type;
        this.quantite = quantite;
        this.dateMouvement = dateMouvement;
    }

    public int getId() { return id; }
    public int getProductId() { return productId; }
    public String getType() { return type; }
    public int getQuantite() { return quantite; }
    public LocalDate getDateMouvement() { return dateMouvement; }

    public void setId(int id) { this.id = id; }
}
