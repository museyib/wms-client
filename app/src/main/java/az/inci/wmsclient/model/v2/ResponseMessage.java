package az.inci.wmsclient.model.v2;

import lombok.Data;

@Data
public class ResponseMessage {
    private int statusCode;
    private String title;
    private String body;
    private int iconId;
}
