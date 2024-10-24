package com.demo.donation.api;

import com.google.gson.annotations.SerializedName;

public class DonationHistory {
    @SerializedName("id")
    private int id;

    @SerializedName("donationType")
    private String donationType;

    @SerializedName("amount")
    private Double amount;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    @SerializedName("date")
    private String date;

    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    public DonationHistory(int id, String donationType, Double amount,
                           String description, String status, String date,
                           String name, String phone) {
        this.id = id;
        this.donationType = donationType;
        this.amount = amount;
        this.description = description;
        this.status = status;
        this.date = date;
        this.name = name;
        this.phone = phone;
    }

    // Getters
    public int getId() { return id; }
    public String getDonationType() { return donationType; }
    public Double getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public String getName() { return name; }
    public String getPhone() { return phone; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setDonationType(String donationType) { this.donationType = donationType; }
    public void setAmount(Double amount) { this.amount = amount; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setDate(String date) { this.date = date; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
}