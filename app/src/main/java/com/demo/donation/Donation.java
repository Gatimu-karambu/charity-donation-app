package com.demo.donation;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "donations",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE))
public class Donation {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "donation_type")
    private String donationType;

    @ColumnInfo(name = "amount")
    private Double amount;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    public Donation(int userId, String donationType, Double amount,
                    String description, String status, long timestamp) {
        this.userId = userId;
        this.donationType = donationType;
        this.amount = amount;
        this.description = description;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getDonationType() { return donationType; }
    public void setDonationType(String donationType) { this.donationType = donationType; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}