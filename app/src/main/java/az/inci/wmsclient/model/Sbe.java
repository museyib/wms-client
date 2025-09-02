package az.inci.wmsclient.model;

import androidx.annotation.NonNull;

import lombok.Data;

@Data
public class Sbe {
    private String sbeCode;
    private String sbeName;

    @Override
    @NonNull
    public String toString() {
        return sbeCode + " - " + sbeName;
    }
}
