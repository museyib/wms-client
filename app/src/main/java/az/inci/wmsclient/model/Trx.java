package az.inci.wmsclient.model;

import java.util.Objects;

import lombok.Data;

@Data
public class Trx {
    private int position;
    private int trxId;
    private String trxNo;
    private String trxDate;
    private String pickStatus;
    private String invCode;
    private String invName;
    private double qty;
    private double pickedQty;
    private double packedQty;
    private String whsCode;
    private String pickArea;
    private String pickGroup;
    private String pickUser;
    private String approveUser;
    private String uom;
    private double uomFactor;
    private String invBrand;
    private String bpName;
    private String sbeName;
    private String barcode;
    private String prevTrxNo;
    private String notes = "";
    private int priority;
    private int trxTypeId;
    private double amount;
    private double price;
    private double discountRatio;
    private double discount;
    private double prevQty;
    private double prevQtySum;
    private String prevTrxDate;
    private int prevTrxId;
    private int minutes;
    private String notPickedReasonId;

    public static Trx parseFromInv(Inventory inventory) {
        Trx trx = new Trx();
        trx.setInvCode(inventory.getInvCode());
        trx.setInvName(inventory.getInvName());
        trx.setBarcode(inventory.getBarcode());
        trx.setInvBrand(inventory.getInvBrand());
        trx.setNotes(inventory.getInternalCount());
        trx.setPrice(inventory.getPrice());
        trx.setPrevTrxNo("");
        return trx;
    }

    public boolean isReturned() {
        return prevTrxId != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trx trx = (Trx) o;
        return trxId == trx.trxId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(trxId);
    }
}
