package az.inci.wmsclient.model.v3;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InternalUseRequestItem {
    private String invCode;
    private String invName;
    private double qty;
    private String invBrand;
    private String barcode;
    private String notes;
}
