package me.priyazsh.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class SocialSignInRequest {

    @NotBlank(message = "provider is required")
    private String provider;

    @NotBlank(message = "callbackURL is required")
    private String callbackURL;

    public SocialSignInRequest() {}

    public SocialSignInRequest(String provider, String callbackURL) {
        this.provider = provider;
        this.callbackURL = callbackURL;
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getCallbackURL() { return callbackURL; }
    public void setCallbackURL(String callbackURL) { this.callbackURL = callbackURL; }
}
