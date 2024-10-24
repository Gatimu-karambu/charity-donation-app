package com.demo.donation.api;


import com.google.gson.annotations.SerializedName;

public class DonationRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    @SerializedName("donationType")
    private String donationType;

    @SerializedName("amount")
    private Double amount;

    @SerializedName("description")
    private String description;

    public DonationRequest(String name, String phone, String donationType, Double amount, String description) {
        this.name = name;
        this.phone = phone;
        this.donationType = donationType;
        this.amount = amount;
        this.description = description;
    }

    // Getters
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getDonationType() { return donationType; }
    public Double getAmount() { return amount; }
    public String getDescription() { return description; }
}