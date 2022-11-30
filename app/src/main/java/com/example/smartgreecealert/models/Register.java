package com.example.smartgreecealert.models;

public class Register {

    private String fistName, lastName, email, password, retypePassword;

    private Register(Builder builder) {
        this.fistName = builder.fistName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.password = builder.password;
        this.retypePassword = builder.retypePassword;
    }

    public static class Builder {

        private String fistName, lastName, email, password, retypePassword;

        public Builder withFullName(String fistName, String lastName) {
            this.fistName = fistName;
            this.lastName = lastName;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withPassword(String password, String retypePassword) {
            this.password = password;
            this.retypePassword = retypePassword;
            return this;
        }

        public Register build(){
            return new Register(this);
        }
    }

    public String getFistName() {
        return fistName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRetypePassword() {
        return retypePassword;
    }
}
