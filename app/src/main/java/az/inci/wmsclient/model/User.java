package az.inci.wmsclient.model;

import lombok.Data;

@Data
public class User {
    private String id;
    private String password;
    private String name;
    private String whsCode;
    private String pickGroup;
    private boolean collectFlag;
    private boolean pickFlag;
    private boolean checkFlag;
    private boolean countFlag;
    private boolean attributeFlag;
    private boolean locationFlag;
    private boolean packFlag;
    private boolean docFlag;
    private boolean loadingFlag;
    private boolean approveFlag;
    private boolean approvePrdFlag;
    private boolean purchaseOrdersFlag;
    private boolean barcodeFlag;
    private boolean changeInvMasterFlag;
}
