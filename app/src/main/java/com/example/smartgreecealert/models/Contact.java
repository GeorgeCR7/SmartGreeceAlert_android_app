package com.example.smartgreecealert.models;

public class Contact {
    private String id;
    private String name;
    private String mobileNumber;

    public Contact(String id, String name, String mobileNumber) {
        this.id = id;
        this.name = name;
        this.mobileNumber = mobileNumber;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }
}
