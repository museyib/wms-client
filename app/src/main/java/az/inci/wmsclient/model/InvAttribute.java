package az.inci.wmsclient.model;

import lombok.Data;

@Data
public class InvAttribute {
    private String invCode;
    private String attributeId;
    private String attributeType;
    private String attributeName;
    private String attributeValue;
    private String whsCode;
    private boolean defined;
}
