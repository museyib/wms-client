package az.inci.wmsclient.model;

import androidx.annotation.NonNull;

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

    @NonNull
    @Override
    public String toString() {
        return attributeName;
    }
}
