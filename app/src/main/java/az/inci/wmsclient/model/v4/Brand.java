package az.inci.wmsclient.model.v4;

import androidx.annotation.NonNull;

import java.util.Objects;

import lombok.Data;

@Data
public class Brand {
    private String brandCode;
    private String brandName;

    @NonNull
    @Override
    public String toString() {
        return brandCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Brand brand = (Brand) o;
        return Objects.equals(brandCode, brand.brandCode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(brandCode);
    }
}
