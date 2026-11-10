package me.priyazsh.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserInfo {

    private String sub;
    private String email;

    @JsonProperty("email_verified")
    private boolean emailVerified;

    private String name;
    private String picture;

    @JsonProperty("preferred_username")
    private String preferredUsername;

    private String role;

    public UserInfo() {}

    public UserInfo(String sub, String email, boolean emailVerified, String name,
                    String picture, String preferredUsername, String role) {
        this.sub = sub;
        this.email = email;
        this.emailVerified = emailVerified;
        this.name = name;
        this.picture = picture;
        this.preferredUsername = preferredUsername;
        this.role = role;
    }

    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPicture() { return picture; }
    public void setPicture(String picture) { this.picture = picture; }

    public String getPreferredUsername() { return preferredUsername; }
    public void setPreferredUsername(String preferredUsername) { this.preferredUsername = preferredUsername; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
