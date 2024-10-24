package com.demo.donation.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface DonationApi {
    @POST("donate")
    Call<DonationResponse> addDonation(@Body DonationRequest request);

    // Updated to match Flask route
    @GET("donations/{phone}")
    Call<List<DonationHistory>> getUserDonations(@Path("phone") String phone);

    @PUT("donations/{donationId}/status")
    Call<DonationResponse> updateDonationStatus(
            @Path("donationId") int donationId,
            @Body StatusUpdateRequest status
    );
}

class StatusUpdateRequest {
    private String status;

    public StatusUpdateRequest(String status) {
        this.status = status;
    }
}