package az.inci.wmsclient.model;

import androidx.annotation.NonNull;

import java.util.Objects;

import lombok.Data;

@Data
public class ExpCenter {
    private String expCenterCode;
    private String expCenterName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpCenter expCenter = (ExpCenter) o;
        return expCenterCode.equals(expCenter.expCenterCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expCenterCode);
    }

    @Override
    @NonNull
    public String toString() {

        if (expCenterCode != null
                && (!expCenterCode.isEmpty()
                && !expCenterName.isEmpty())) {
            return expCenterCode + " - " + expCenterName;
        } else {
            return "";
        }
    }
}
