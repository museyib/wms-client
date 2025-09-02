package az.inci.wmsclient.model;

import java.util.Objects;

import lombok.Data;

@Data
public class InvBarcode {
    private String invCode;
    private String barcode;
    private String uom;
    private double uomFactor;
    private boolean defined;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvBarcode barcode1 = (InvBarcode) o;
        return Objects.equals(barcode, barcode1.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode);
    }
}
