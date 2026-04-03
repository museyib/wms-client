package az.inci.wmsclient.model;

import androidx.annotation.NonNull;

import java.util.Objects;

import lombok.Data;

@Data
public class ShipTrx {
    private String regionCode;
    private String driverCode;
    private String driverName;
    private String assistantCode;
    private String assistantName;
    private String srcTrxNo;
    private String vehicleCode;
    private String userId;
    private boolean taxed;

    @NonNull
    @Override
    public String toString() {
        return srcTrxNo + (taxed ? "\t : İcazəlidir" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShipTrx trx = (ShipTrx) o;
        return srcTrxNo.equals(trx.srcTrxNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcTrxNo);
    }
}
