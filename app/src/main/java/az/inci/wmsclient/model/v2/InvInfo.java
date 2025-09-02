package az.inci.wmsclient.model.v2;

import lombok.Data;

@Data
public class InvInfo {
    private String invCode;
    private String invName;
    private String defaultUomCode;
    private double whsQty;
    private String info;
    private String whsCode;
}
