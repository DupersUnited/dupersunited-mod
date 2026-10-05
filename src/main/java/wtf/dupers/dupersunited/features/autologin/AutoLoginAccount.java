package wtf.dupers.dupersunited.features.autologin;

import com.google.gson.annotations.SerializedName;

public class AutoLoginAccount {
    @SerializedName("Username")
    private final String username;

    @SerializedName("Password")
    private final String password;

    public AutoLoginAccount(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}