package az.inci.wmsclient.model;

import androidx.annotation.NonNull;

import java.util.Objects;

import lombok.Data;

@Data
public class Inventory {
    private String invCode;
    private String invName;
    private String barcode;
    private String invBrand;
    private String internalCount;
    private String defaultUomCode;
    private double price;
    private double whsQty;

    public static Inventory parseFromTrx(Trx trx) {
        Inventory inventory = new Inventory();
        inventory.setInvCode(trx.getInvCode());
        inventory.setInvName(trx.getInvName());
        inventory.setInvBrand(trx.getInvBrand());
        inventory.setBarcode(trx.getBarcode());
        inventory.setPrice(trx.getPrice());

        return inventory;
    }

    @Override
    @NonNull
    public String toString() {
        return invCode + " | " + invName + " | " + invBrand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(invCode, inventory.invCode) &&
                Objects.equals(barcode, inventory.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invCode, barcode);
    }
}
