package com.demo.donation;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DonationDao {
    @Insert
    long insert(Donation donation);

    @Update
    void update(Donation donation);
    
    @Query("UPDATE donations SET status = :status WHERE id = :donationId")
    void updateDonationStatus(int donationId, String status);

    @Query("SELECT * FROM donations ORDER BY timestamp DESC")
    List<Donation> getAllDonations();

    @Query("SELECT * FROM donations WHERE user_id = :userId ORDER BY timestamp DESC")
    List<Donation> getUserDonations(int userId);

    @Query("DELETE FROM donations WHERE user_id = :userId")
    void deleteUserDonations(int userId);

    @Query("SELECT * FROM donations WHERE id = :id LIMIT 1")
    Donation getDonationById(int id);
}