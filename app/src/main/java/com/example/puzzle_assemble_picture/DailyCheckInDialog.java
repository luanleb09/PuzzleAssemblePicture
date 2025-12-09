package com.example.puzzle_assemble_picture;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DailyCheckInDialog extends Dialog {

    private DailyRewardManager dailyRewardManager;
    private OnCheckInListener listener;

    public interface OnCheckInListener {
        void onCheckInSuccess(int reward, int streak);
    }

    public DailyCheckInDialog(Context context, DailyRewardManager manager, OnCheckInListener listener) {
        super(context);
        this.dailyRewardManager = manager;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_daily_checkin);

        TextView streakText = findViewById(R.id.streakText);
        Button btnCheckIn = findViewById(R.id.btnCheckIn);
        Button btnClose = findViewById(R.id.btnClose);

        int currentStreak = dailyRewardManager.getCheckInStreak();
        boolean canCheckIn = dailyRewardManager.canCheckInToday();

        streakText.setText("Current Streak: " + currentStreak + " days 🔥");

        // Highlight completed days
        highlightDays(currentStreak, canCheckIn);

        if (!canCheckIn) {
            btnCheckIn.setText("Already Checked In Today ✅");
            btnCheckIn.setEnabled(false);
        }

        btnCheckIn.setOnClickListener(v -> {
            int reward = dailyRewardManager.performCheckIn();
            if (reward > 0) {
                int newStreak = dailyRewardManager.getCheckInStreak();
                Toast.makeText(getContext(),
                        "Check-in successful! +" + reward + " coins 🎉",
                        Toast.LENGTH_LONG).show();

                if (listener != null) {
                    listener.onCheckInSuccess(reward, newStreak);
                }

                dismiss();
            } else {
                Toast.makeText(getContext(), "Already checked in today!", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dismiss());
    }

    private void highlightDays(int currentStreak, boolean canCheckIn) {
        int[] dayLayouts = {
                R.id.day1Layout, R.id.day2Layout, R.id.day3Layout, R.id.day4Layout,
                R.id.day5Layout, R.id.day6Layout, R.id.day7Layout
        };

        int completedDays = canCheckIn ? currentStreak : currentStreak;

        for (int i = 0; i < dayLayouts.length; i++) {
            LinearLayout dayLayout = findViewById(dayLayouts[i]);

            if (i < completedDays) {
                // Completed
                dayLayout.setBackgroundColor(Color.parseColor("#4CAF50"));
            } else if (i == completedDays && canCheckIn) {
                // Today
                dayLayout.setBackgroundColor(Color.parseColor("#FFD700"));
            } else {
                // Not yet
                dayLayout.setBackgroundColor(Color.parseColor("#E0E0E0"));
            }
        }
    }
}