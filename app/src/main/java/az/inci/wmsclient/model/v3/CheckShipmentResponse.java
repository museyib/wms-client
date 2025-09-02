package az.inci.wmsclient.model.v3;

import lombok.Data;

@Data
public class CheckShipmentResponse {
    private String driverCode;
    private String driverName;
    private boolean shipped;
}
