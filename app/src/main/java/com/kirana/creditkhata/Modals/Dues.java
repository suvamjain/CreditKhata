package com.kirana.creditkhata.Modals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

public class Dues {

    private String fromDate;
    private String amtDue;
    private String name;
    private String phNo;
    private Date actualDate;
    private TreeMap<String, Credits.creditVal> dueCredits;

    public Dues() {}

    public Dues(String amtDue, String name, String phNo, TreeMap<String, Credits.creditVal> dueCredits) {
        try {
            Date d = new SimpleDateFormat("yyMMdd").parse(dueCredits.firstKey().split("_")[0]);
            this.actualDate = d;
            this.fromDate = new SimpleDateFormat("dd/MM/yy").format(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        this.amtDue = amtDue;
        this.name = name;
        this.phNo = phNo;
        this.dueCredits = dueCredits;
    }

    public Date getActualDate() { return actualDate; }

    public void setActualDate(Date actualDate) { this.actualDate = actualDate; }

    public String getFromDate() { return fromDate; }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getAmtDue() { return amtDue; }

    public void setAmtDue(String amtDue) {
        this.amtDue = amtDue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhNo() {
        return phNo;
    }

    public void setPhNo(String phNo) {
        this.phNo = phNo;
    }

    public TreeMap<String, Credits.creditVal> getDueCredits() { return dueCredits; }

    public void setDueCredits(TreeMap<String, Credits.creditVal> dueCredits) { this.dueCredits = dueCredits; }
}
