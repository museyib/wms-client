package az.inci.wmsclient.model.v3;

import org.jetbrains.annotations.NotNull;

import lombok.Data;

@Data
public class NotPickedReason {
    private String reasonId;
    private String reasonDescription;

    @NotNull
    @Override
    public String toString() {
        return reasonId + " - " + reasonDescription;
    }
}
