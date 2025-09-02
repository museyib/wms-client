package az.inci.wmsclient.model.v3;

import java.util.List;

import lombok.Data;

@Data
public class PurchaseTrx {
    private int trxId;
    private String trxNo;
    private String invCode;
    private String invName;
    private String whsCode;
    private double qty;
    private double countedQty;
    private String uom;
    private double uomFactor;
    private double price;
    private double amount;
    private List<String> barcodes;
}
