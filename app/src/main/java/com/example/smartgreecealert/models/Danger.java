package com.example.smartgreecealert.models;

import com.example.smartgreecealert.helpers.Enums;

public class Danger {

    private String userID;
    private double latitude;
    private double longitude;
    private String timestamp;
    private Enum<Enums.dangerType> type;
    private boolean isAborted;

    private Danger (Builder builder) {
        this.userID = builder.userID;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.timestamp = builder.timestamp;
        this.type = builder.type;
        this.isAborted = builder.isAborted;
    }

    public static class Builder {

        private String userID;
        private double latitude;
        private double longitude;
        private String timestamp;
        private Enum<Enums.dangerType> type;
        private boolean isAborted;

        public Builder withUserID(String userID) {
            this.userID = userID;
            return this;
        }

        public Builder withLatLong(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        public Builder withTimestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withType(Enum<Enums.dangerType> type) {
            this.type = type;
            return this;
        }

        public Builder aborted(boolean isAborted) {
            this.isAborted = isAborted;
            return this;
        }

        public Danger build(){
            return new Danger(this);
        }
    }

    public String getUserID() {
        return userID;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isAborted() {
        return isAborted;
    }

    public Enum<Enums.dangerType> getType() {
        return type;
    }
}
