package az.inci.wmsclient.model.v2;

import lombok.Data;

@Data
public class CustomResponse {
    private int statusCode;
    private String systemMessage;
    private String developerMessage;
    private Object data;
}
