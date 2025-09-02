package az.inci.wmsclient.model;

import java.util.List;
import java.util.Objects;

import lombok.Data;

@Data
public class Doc {
    private String trxNo;
    private String trxDate;
    private int recStatus;
    private String pickStatus;
    private String whsCode;
    private String whsName;
    private String description;
    private String notes;
    private String pickArea;
    private String pickGroup;
    private String pickUser;
    private int itemCount;
    private int pickedItemCount;
    private String prevTrxNo;
    private String bpName;
    private String sbeName;
    private String bpCode;
    private String sbeCode;
    private String approveUser;
    private int trxTypeId;
    private double amount;
    private String srcWhsCode;
    private String srcWhsName;
    private String expCenterCode;
    private String expCenterName;
    private int activeSeconds;
    private List<Trx> trxList;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Doc doc = (Doc) o;
        return Objects.equals(trxNo, doc.trxNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trxNo);
    }
}
