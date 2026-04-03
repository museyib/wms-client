package az.inci.wmsclient.model;

import lombok.Data;

@Data
public class ShipDoc {
    private String regionCode;
    private String driverCode;
    private String driverName;
    private String assistantCode;
    private String assistantName;
    private String vehicleCode;
    private String userId;
    private int count;
}
