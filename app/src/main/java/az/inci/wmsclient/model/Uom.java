package az.inci.wmsclient.model;

import androidx.annotation.NonNull;

import java.util.Objects;

import lombok.Data;

@Data
public class Uom {
    private String uomCode;
    private String uomName;

    public Uom(String uomCode) {
        this.uomCode = uomCode;
    }

    @NonNull
    @Override
    public String toString() {
        return uomCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Uom uom = (Uom) o;
        return Objects.equals(uomCode, uom.uomCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uomCode);
    }
}
