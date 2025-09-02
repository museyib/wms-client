package az.inci.wmsclient.model.v3;

import lombok.Data;
import lombok.NonNull;

@Data
public class LatestMovementItem {
    private String trxNo;
    private String trxDate;
    private double quantity;

    @Override
    @NonNull
    public String toString() {
        return trxNo + " | " + trxDate + " | " + quantity;
    }
}
