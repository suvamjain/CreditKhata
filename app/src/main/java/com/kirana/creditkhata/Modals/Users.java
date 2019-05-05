package com.kirana.creditkhata.Modals;

public class Users {

    private String u_name, u_addr, u_email, u_img, u_token, u_ph;

    public Users() {}

    public Users(String u_name, String u_addr, String u_email, String u_img, String u_token) {
        this.u_name = u_name;
        this.u_addr = u_addr;
        this.u_email = u_email;
        this.u_img = u_img;
        this.u_token = u_token;
    }

    public String getU_name() { return u_name; }

    public void setU_name(String u_name) { this.u_name = u_name; }

    public String getU_addr() { return u_addr; }

    public void setU_addr(String u_addr) { this.u_addr = u_addr; }

    public String getU_email() { return u_email; }

    public void setU_email(String u_email) { this.u_email = u_email; }

    public String getU_img() { return u_img; }

    public void setU_img(String u_img) { this.u_img = u_img; }

    public String getU_token() { return u_token; }

    public void setU_token(String u_token) { this.u_token = u_token; }

    public String getU_ph() { return u_ph; }

    public void setU_ph(String u_ph) { this.u_ph = u_ph; }

    @Override
    public String toString() {
        return "Users{" +
                "u_name='" + u_name + '\'' +
                ", u_addr='" + u_addr + '\'' +
                ", u_email='" + u_email + '\'' +
                ", u_img='" + u_img + '\'' +
                ", u_token='" + u_token + '\'' +
                ", u_ph='" + u_ph + '\'' +
                '}';
    }
}
