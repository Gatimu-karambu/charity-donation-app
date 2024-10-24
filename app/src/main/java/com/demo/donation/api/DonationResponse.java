package com.demo.donation.api;

import com.google.gson.annotations.SerializedName;

public class DonationResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("donation_id")
    private Integer donationId;

    @SerializedName("error")
    private String error;

    // Constructor
    public DonationResponse(String message, Integer donationId, String error) {
        this.message = message;
        this.donationId = donationId;
        this.error = error;
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public Integer getDonationId() {
        return donationId;
    }

    public String getError() {
        return error;
    }

    // Optional setters
    public void setMessage(String message) {
        this.message = message;
    }

    public void setDonationId(Integer donationId) {
        this.donationId = donationId;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "DonationResponse{" +
                "message='" + message + '\'' +
                ", donationId=" + donationId +
                ", error='" + error + '\'' +
                '}';
    }
}