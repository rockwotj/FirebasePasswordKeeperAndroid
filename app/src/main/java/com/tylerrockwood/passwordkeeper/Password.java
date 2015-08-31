package com.tylerrockwood.passwordkeeper;

public class Password {

    private String key;
    String service;
    String username;
    String password;


    Password() {
        this(null, null, null);
    }

    public Password(String username, String password, String service) {
        this(null, username, password, service);
    }

    public Password(String key, String username, String password, String service) {
        this.key = key;
        this.service = service;
        this.username = username;
        this.password = password;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
