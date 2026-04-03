package az.inci.wmsclient;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class AppConfig {
    public static final int PICK_MODE = 0;
    public static final int PACK_MODE = 1;
    public static final int SHIP_MODE = 2;
    public static final int APPROVE_MODE = 3;
    public static final int PRODUCT_APPROVE_MODE = 4;
    public static final int INV_ATTRIBUTE_MODE = 5;
    public static final int CONFIRM_DELIVERY_MODE = 6;
    public static final int PURCHASE_ORDER_MODE = 7;
    public static final int VIEW_MODE = 0;
    public static final int NEW_MODE = 1;
    public static final String DB_NAME = "WMSClient.db";
    public static final int DB_VERSION = 17;
}
