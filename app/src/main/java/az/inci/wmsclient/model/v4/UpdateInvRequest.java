package az.inci.wmsclient.model.v4;

import lombok.Data;

@Data
public class UpdateInvRequest {
    private String invCode;
    private String brandCode;
    private String userId;
    private String deviceId;
}
