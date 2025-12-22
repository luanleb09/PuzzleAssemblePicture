package com.example.puzzle_assemble_picture;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import java.util.Calendar;

public class DailyCheckInDialog extends Dialog {
    private static final String TAG = "DailyCheckInDialog";

    private final Activity activity; // ✅ Store Activity reference
    private final DailyRewardManager rewardManager;
    private final OnRewardListener rewardListener;
    private GridLayout calendarGrid;
    private Button checkInButton;
    private TextView monthYearText;
    private TextView totalDaysText;
    private RewardedAd rewardedAd;
    private int selectedPastDay = -1;

    public interface OnRewardListener {
        void onReward(int coins, int day);
    }

    // ✅ Constructor accepts Activity
    public DailyCheckInDialog(@NonNull Activity activity,
                              DailyRewardManager rewardManager,
                              OnRewardListener listener) {
        super(activity);
        this.activity = activity; // ✅ Store Activity
        this.rewardManager = rewardManager;
        this.rewardListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_daily_checkin);

        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95);
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(params);

        calendarGrid = findViewById(R.id.calendarGrid);
        checkInButton = findViewById(R.id.checkInButton);
        monthYearText = findViewById(R.id.monthYearText);
        totalDaysText = findViewById(R.id.totalDaysText);

        // Check for new month
        rewardManager.checkAndResetForNewMonth();

        setupCalendar();
        updateMonthYear();
        updateTotalDays();
        updateCheckInButton(); // ✅ Update button state

        checkInButton.setOnClickListener(v -> performCheckIn());
        findViewById(R.id.closeButton).setOnClickListener(v -> dismiss());

        loadRewardedAd();
    }

    private void updateCheckInButton() {
        if (rewardManager.canCheckInToday()) {
            checkInButton.setEnabled(true);
            checkInButton.setText("Check In Today");
        } else {
            checkInButton.setEnabled(false);
            checkInButton.setText("✓ Checked In Today");
        }
    }

    private void setupCalendar() {
        calendarGrid.removeAllViews();

        int daysInMonth = rewardManager.getDaysInCurrentMonth();
        int currentDay = rewardManager.getCurrentDay();

        android.util.DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        float density = metrics.density;

        // ✅ Calculate more conservative size
        int screenWidth = (int) (metrics.widthPixels * 0.95f);

        // Account for dialog padding (12dp each side = 24dp total)
        // Account for GridLayout margins (1dp * 2 per item * 7 items = 14dp)
        int dialogPadding = (int) (24 * density);
        int totalMargins = (int) (14 * density);
        int availableWidth = screenWidth - dialogPadding - totalMargins;

        // Divide by 7 columns
        int itemWidth = availableWidth / 7;
        int itemHeight = (int) (itemWidth * 1.15); // Slightly taller

        // ✅ Make sure minimum size
        if (itemWidth < (int) (40 * density)) {
            itemWidth = (int) (40 * density);
            itemHeight = (int) (46 * density);
        }

        calendarGrid.setColumnCount(7);
        calendarGrid.setRowCount((int) Math.ceil(daysInMonth / 7.0));

        for (int day = 1; day <= daysInMonth; day++) {
            View dayView = getLayoutInflater().inflate(R.layout.item_calendar_day, calendarGrid, false);

            // ✅ Set exact size with small margins
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = itemWidth;
            params.height = itemHeight;
            params.setMargins(1, 1, 1, 1); // Consistent 1px margins
            dayView.setLayoutParams(params);

            TextView dayNumber = dayView.findViewById(R.id.dayNumber);
            TextView rewardAmount = dayView.findViewById(R.id.rewardAmount);
            View checkMark = dayView.findViewById(R.id.checkMark);
            View todayIndicator = dayView.findViewById(R.id.todayIndicator);
            View adIcon = dayView.findViewById(R.id.adIcon);

            dayNumber.setText(String.valueOf(day));

            int reward = rewardManager.getRewardForDay(day);
            rewardAmount.setText(reward + "🪙");
            rewardAmount.setVisibility(View.VISIBLE);

            boolean isCheckedIn = rewardManager.isDayCheckedIn(day);

            if (day == currentDay) {
                todayIndicator.setVisibility(View.VISIBLE);
                if (!isCheckedIn) {
                    dayView.setBackgroundResource(R.drawable.bg_calendar_today);
                }
            }

            if (isCheckedIn) {
                checkMark.setVisibility(View.VISIBLE);
                dayView.setBackgroundResource(R.drawable.bg_calendar_checked);
                adIcon.setVisibility(View.GONE);
            } else if (day < currentDay) {
                adIcon.setVisibility(View.VISIBLE);
                dayView.setBackgroundResource(R.drawable.bg_calendar_missed);

                final int pastDay = day;
                dayView.setOnClickListener(v -> showAdForPastDay(pastDay));
            } else if (day > currentDay) {
                dayView.setBackgroundResource(R.drawable.bg_calendar_future);
                adIcon.setVisibility(View.GONE);
            }

            calendarGrid.addView(dayView);
        }
    }

    private void updateMonthYear() {
        Calendar calendar = Calendar.getInstance();
        String monthYear = new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                .format(calendar.getTime());
        monthYearText.setText(monthYear);
    }

    private void updateTotalDays() {
        int checkedDays = rewardManager.getTotalCheckedInDays();
        int totalDays = rewardManager.getDaysInCurrentMonth();
        totalDaysText.setText("Checked in: " + checkedDays + "/" + totalDays + " days");
    }

    private void performCheckIn() {
        if (!rewardManager.canCheckInToday()) {
            Toast.makeText(getContext(), "Already checked in today!", Toast.LENGTH_SHORT).show();
            return;
        }

        DailyRewardManager.CheckInResult result = rewardManager.checkIn();

        if (result.success) {
            Toast.makeText(getContext(),
                    "✅ Checked in! +" + result.reward + " coins",
                    Toast.LENGTH_SHORT).show();

            if (rewardListener != null) {
                rewardListener.onReward(result.reward, result.day);
            }

            setupCalendar();
            updateTotalDays();
            updateCheckInButton(); // ✅ Update button after check-in

        } else {
            Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
        }
    }

    // ===== REWARDED AD =====

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(getContext(), AdMobConfig.REWARDED_AD_UNIT_ID, adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d(TAG, "Rewarded ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Failed to load rewarded ad: " + loadAdError.getMessage());
                        rewardedAd = null;
                    }
                });
    }

    private void showAdForPastDay(int day) {
        if (rewardedAd == null) {
            Toast.makeText(getContext(), "Ad not ready. Please try again.", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
            return;
        }

        selectedPastDay = day;

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed");
                rewardedAd = null;
                loadRewardedAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                rewardedAd = null;
                Toast.makeText(getContext(), "Failed to show ad", Toast.LENGTH_SHORT).show();
                loadRewardedAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed");
            }
        });

        rewardedAd.show(activity, rewardItem -> {
            DailyRewardManager.CheckInResult result = rewardManager.checkInPastDay(selectedPastDay);

            if (result.success) {
                Toast.makeText(getContext(),
                        "✅ Day " + selectedPastDay + " checked in! +" + result.reward + " coins",
                        Toast.LENGTH_SHORT).show();

                if (rewardListener != null) {
                    rewardListener.onReward(result.reward, result.day);
                }

                setupCalendar();
                updateTotalDays();
                // Don't update button here - only when checking in today
            } else {
                Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}