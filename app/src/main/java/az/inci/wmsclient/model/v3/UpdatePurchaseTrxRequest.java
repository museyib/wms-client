package az.inci.wmsclient.model.v3;

import lombok.Data;

@Data
public class UpdatePurchaseTrxRequest {
    private String userId;
    private String deviceId;
    private String trxNo;
    private int trxId;
    private double qty;
}
