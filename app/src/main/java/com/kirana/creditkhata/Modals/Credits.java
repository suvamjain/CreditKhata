package com.kirana.creditkhata.Modals;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Credits {

    public String name, addr;
    private String _dt, _amount, _detail;
    public Map<String, creditVal> credits;

    public Credits() {}

    public Credits(String name, String addr, String dt, String amount, String detail) {
        this._dt = dt;
        this.name = name;
        this.addr = addr;
        this._amount = amount;
        this._detail = detail;
        this.credits = new HashMap<String, creditVal>();
        credits.put(_dt, new creditVal(_amount, _detail, dt.split("_")[1]));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public Map<String, creditVal> getCredits() { return credits; }

    public void setCredits(Map<String, creditVal> credits) {
        this.credits = credits;
    }

    public creditVal setOnlyCredit(String amount, String detail, String time) {
       return new creditVal(amount, detail, time);
    }

    @Override
    public String toString() {
        return "Credits{" +
                "name='" + name + '\'' +
                ", addr='" + addr + '\'' +
                ", credits=" + credits.values().toString() +
                '}';
    }

    public static class creditVal implements Parcelable{

        public String amount, detail, status, actualAmt, transacTime;

        public creditVal(String _amount, String _detail, String _transacTime) {
            this.amount = _amount;
            this.detail = _detail;
            this.transacTime = _transacTime;
        }

        public creditVal(){}

        protected creditVal(Parcel in) {
            amount = in.readString();
            detail = in.readString();
            status = in.readString();
            actualAmt = in.readString();
            transacTime = in.readString();
        }

        public static final Creator<creditVal> CREATOR = new Creator<creditVal>() {
            @Override
            public creditVal createFromParcel(Parcel in) {
                return new creditVal(in);
            }

            @Override
            public creditVal[] newArray(int size) {
                return new creditVal[size];
            }
        };

        public String getAmount() { return amount; }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public String getStatus() { return status; }

        public void setStatus(String status) { this.status = status; }

        public String getActualAmt() { return actualAmt; }

        public void setActualAmt(String actualAmt) { this.actualAmt = actualAmt; }

        public String getTransacTime() { return transacTime; }

        public void setTransacTime(String transacTime) { this.transacTime = transacTime; }

        @Override
        public String toString() {
            return "creditVal{" +
                    "amount='" + amount + '\'' +
                    ", detail='" + detail + '\'' +
                    ", status='" + status + '\'' +
                    ", actualAmt='" + actualAmt + '\'' +
                    ", transacTime='" + transacTime + '\'' +
                    '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(amount);
            dest.writeString(detail);
            dest.writeString(status);
            dest.writeString(actualAmt);
            dest.writeString(transacTime);
        }
    }
}