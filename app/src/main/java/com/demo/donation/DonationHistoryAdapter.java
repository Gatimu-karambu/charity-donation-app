package com.demo.donation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DonationHistoryAdapter extends ListAdapter<Donation, DonationHistoryAdapter.DonationViewHolder> {

    protected DonationHistoryAdapter() {
        super(new DiffUtil.ItemCallback<Donation>() {
            @Override
            public boolean areItemsTheSame(@NonNull Donation oldItem, @NonNull Donation newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Donation oldItem, @NonNull Donation newItem) {
                return oldItem.getStatus().equals(newItem.getStatus()) &&
                        oldItem.getDescription().equals(newItem.getDescription());
            }
        });
    }

    @NonNull
    @Override
    public DonationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_donation, parent, false);
        return new DonationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonationViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class DonationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvType;
        private final TextView tvAmount;
        private final TextView tvDescription;
        private final TextView tvDate;
        private final TextView tvStatus;
        private final ImageView ivIcon;
        private final SimpleDateFormat dateFormat;
        private final Context context;

        public DonationViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            tvType = itemView.findViewById(R.id.tvType);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        }

        void bind(Donation donation) {
            // Set donation type with icon
            tvType.setText(donation.getDonationType());
            setDonationIcon(donation.getDonationType());

            // Handle amount display
            if (donation.getAmount() != null) {
                tvAmount.setVisibility(View.VISIBLE);
                tvAmount.setText(String.format("KES %.2f", donation.getAmount()));
            } else {
                tvAmount.setVisibility(View.GONE);
            }

            // Set description
            String description = donation.getDescription();
            if (description != null && !description.isEmpty()) {
                tvDescription.setVisibility(View.VISIBLE);
                tvDescription.setText(description);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Format and set date
            tvDate.setText(dateFormat.format(new Date(donation.getTimestamp())));

            // Set status with appropriate styling
            setDonationStatus(donation.getStatus());
        }

        private void setDonationStatus(String status) {
            // Convert to lowercase for comparison, but display with proper capitalization
            String displayStatus = capitalizeFirstLetter(status);
            tvStatus.setText(displayStatus);

            // Set status color
            int colorResId;
            switch (status.toLowerCase()) {
                case "received":
                    colorResId = R.color.status_received;
                    tvStatus.setBackgroundResource(R.drawable.bg_status_received);
                    break;
                case "pending":
                default:
                    colorResId = R.color.status_pending;
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    break;
            }
            tvStatus.setTextColor(ContextCompat.getColor(context, colorResId));
        }

        private void setDonationIcon(String type) {
            int iconRes = getDonationTypeIcon(type);
            ivIcon.setImageResource(iconRes);

            // Set icon tint based on donation type
            int tintColor = ContextCompat.getColor(context, getDonationIconTint(type));
            ivIcon.setColorFilter(tintColor);
        }

        private int getDonationTypeIcon(String type) {
            switch (type.toLowerCase()) {
                case "money": return R.drawable.donate_money;
                case "food": return R.drawable.food;
                case "clothes": return R.drawable.clothes;
                case "furniture": return R.drawable.armchair;
                default: return R.drawable.gift;
            }
        }

        private int getDonationIconTint(String type) {
            switch (type.toLowerCase()) {
                case "money": return R.color.money_icon;
                case "food": return R.color.food_icon;
                case "clothes": return R.color.clothes_icon;
                case "furniture": return R.color.furniture_icon;
                default: return R.color.default_icon;
            }
        }

        private String capitalizeFirstLetter(String text) {
            if (text == null || text.isEmpty()) {
                return text;
            }
            return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
        }
    }
}