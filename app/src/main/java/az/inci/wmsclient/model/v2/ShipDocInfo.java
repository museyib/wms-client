package az.inci.wmsclient.model.v2;

import lombok.Data;

@Data
public class ShipDocInfo {
    private String driverCode;
    private String driverName;
    private String vehicleCode;
    private String deliverNotes;
    private String shipStatus;
}
