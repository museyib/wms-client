package az.inci.wmsclient.model.v3;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InternalUseRequest {
    private String userId;
    private String whsCode;
    private String expCenterCode;
    private String trxNo;
    private String notes;
    private List<InternalUseRequestItem> requestItems;
}
