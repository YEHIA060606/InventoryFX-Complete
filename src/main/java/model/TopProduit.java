package model;

public class TopProduit {

    private String nom;
    private int quantite;
    private double valeurStock;

    public TopProduit(String nom, int quantite, double valeurStock) {
        this.nom = nom;
        this.quantite = quantite;
        this.valeurStock = valeurStock;
    }

    public String getNom() {
        return nom;
    }

    public int getQuantite() {
        return quantite;
    }

    public double getValeurStock() {
        return valeurStock;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public void setValeurStock(double valeurStock) {
        this.valeurStock = valeurStock;
    }

    @Override
    public String toString() {
        return nom + " (" + quantite + ")";
    }
}
