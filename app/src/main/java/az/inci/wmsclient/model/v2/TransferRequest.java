package az.inci.wmsclient.model.v2;

import java.util.List;

import lombok.Data;

@Data
public class TransferRequest {
    private String srcWhsCode;
    private String trgWhsCode;
    private String userId;
    private List<TransferRequestItem> requestItems;
}
