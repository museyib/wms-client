package az.inci.wmsclient.model.v2;

import java.util.List;

import lombok.Data;

@Data
public class ProductApproveRequest {
    private String trxNo;
    private String trxDate;
    private String notes;
    private int status;
    private String userId;
    private List<ProductApproveRequestItem> requestItems;
}
