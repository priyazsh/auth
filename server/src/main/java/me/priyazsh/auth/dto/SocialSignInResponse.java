package me.priyazsh.auth.dto;

public class SocialSignInResponse {

    private String url;
    private boolean redirect;

    public SocialSignInResponse() {}

    public SocialSignInResponse(String url, boolean redirect) {
        this.url = url;
        this.redirect = redirect;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isRedirect() { return redirect; }
    public void setRedirect(boolean redirect) { this.redirect = redirect; }
}
