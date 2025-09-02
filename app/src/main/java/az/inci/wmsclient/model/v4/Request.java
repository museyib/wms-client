package az.inci.wmsclient.model.v4;

import az.inci.wmsclient.activity.AppBaseActivity;
import lombok.Data;

@Data
public class Request<T> {
    private String userId;
    private String deviceId;
    private T data;

    public static <T> Request<T> create(AppBaseActivity context, T data) {
        Request<T> request = new Request<>();
        request.setUserId(context.appUser.getId());
        request.setDeviceId(context.getDeviceIdString());
        request.setData(data);
        return request;
    }
}
