package az.inci.wmsclient.model.v2;

import lombok.Data;

@Data
public class UpdateDeliveryRequest {
    private String trxNo;
    private String note;
    private String deliverPerson;
    private String driverCode;
    private boolean transitionFlag;
}
