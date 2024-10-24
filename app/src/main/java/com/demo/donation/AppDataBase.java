package com.demo.donation;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {User.class, Donation.class}, version = 1)
public abstract class AppDataBase extends RoomDatabase {
    private static AppDataBase instance;

    // Define DAOs
    public abstract UserDao userDao();
    public abstract DonationDao donationDao();

    public static synchronized AppDataBase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDataBase.class,
                    "donation_app_db"
            ).build();
        }
        return instance;
    }
}