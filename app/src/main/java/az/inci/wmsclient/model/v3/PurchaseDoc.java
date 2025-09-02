package az.inci.wmsclient.model.v3;

import lombok.Data;

@Data
public class PurchaseDoc {
    private String trxNo;
    private String trxDate;
    private String description;
    private String bpCode;
    private String bpName;
    private String sbeCode;
    private String sbeName;
    private int trxTypeId;
    private double amount;
    private String whsCode;
}
