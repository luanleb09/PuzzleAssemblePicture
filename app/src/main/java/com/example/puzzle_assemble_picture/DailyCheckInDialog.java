package com.example.puzzle_assemble_picture;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DailyCheckInDialog extends Dialog {

    private static final String TAG = "DailyCheckInDialog";

    private DailyRewardManager rewardManager;
    private OnCheckInListener listener;
    private LinearLayout calendarLayout;

    public interface OnCheckInListener {
        void onCheckIn(int reward, int streak);
    }

    public DailyCheckInDialog(@NonNull Context context, DailyRewardManager rewardManager, OnCheckInListener listener) {
        super(context, android.R.style.Theme_Material_Dialog);
        this.rewardManager = rewardManager;
        this.listener = listener;

        setContentView(R.layout.dialog_daily_checkin);

        calendarLayout = findViewById(R.id.calendarLayout);
        Button btnCheckIn = findViewById(R.id.btnCheckIn);
        Button btnClose = findViewById(R.id.btnClose);

        setupCalendar();

        if (btnCheckIn != null) {
            if (rewardManager.canCheckInToday()) {
                btnCheckIn.setEnabled(true);
                btnCheckIn.setText("✅ Check In Today");
                btnCheckIn.setOnClickListener(v -> performCheckIn());
            } else {
                btnCheckIn.setEnabled(false);
                btnCheckIn.setText("Already checked in today ✓");
                btnCheckIn.setAlpha(0.5f);
            }
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }
    }

    /**
     * ✅ FIX: Setup calendar cho tháng hiện tại với current date highlight
     */
    private void setupCalendar() {
        if (calendarLayout == null) return;

        calendarLayout.removeAllViews();

        // ✅ GET: Current date info
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        int currentMonth = calendar.get(Calendar.MONTH); // 0-11
        int currentYear = calendar.get(Calendar.YEAR);

        // ✅ GET: Số ngày trong tháng hiện tại
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Display month/year header
        TextView monthYearText = new TextView(getContext());
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        monthYearText.setText(sdf.format(calendar.getTime()));
        monthYearText.setTextSize(22);
        monthYearText.setTextColor(Color.parseColor("#333333"));
        monthYearText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        monthYearText.setPadding(0, 10, 0, 30);
        monthYearText.setTypeface(null, android.graphics.Typeface.BOLD);
        calendarLayout.addView(monthYearText);

        // Add day labels (Sun, Mon, Tue...)
        addWeekdayLabels();

        // ✅ CREATE: Grid layout for calendar
        LinearLayout weekRow = null;
        int dayOfWeek = 0;

        // Get first day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday

        // Start first week row
        weekRow = createWeekRow();

        // Add empty cells for days before month starts
        for (int i = 0; i < firstDayOfWeek; i++) {
            addEmptyDayCell(weekRow);
            dayOfWeek++;
        }

        // ✅ ADD: All days in current month
        for (int day = 1; day <= daysInMonth; day++) {
            // New week row
            if (dayOfWeek == 7) {
                calendarLayout.addView(weekRow);
                weekRow = createWeekRow();
                dayOfWeek = 0;
            }

            boolean isToday = (day == currentDay);
            boolean isPast = (day < currentDay);
            boolean isCheckedIn = !rewardManager.canCheckInToday() && isToday;

            addDayCell(weekRow, day, isToday, isPast, isCheckedIn);
            dayOfWeek++;
        }

        // Fill remaining cells in last week
        if (weekRow != null && weekRow.getChildCount() > 0) {
            while (dayOfWeek < 7) {
                addEmptyDayCell(weekRow);
                dayOfWeek++;
            }
            calendarLayout.addView(weekRow);
        }
    }

    /**
     * Add weekday labels
     */
    private void addWeekdayLabels() {
        LinearLayout labelRow = createWeekRow();
        String[] weekdays = {"S", "M", "T", "W", "T", "F", "S"};

        for (String day : weekdays) {
            TextView label = new TextView(getContext());
            label.setText(day);
            label.setTextSize(14);
            label.setTextColor(Color.parseColor("#999999"));
            label.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            label.setTypeface(null, android.graphics.Typeface.BOLD);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            params.setMargins(4, 4, 4, 4);
            label.setLayoutParams(params);

            labelRow.addView(label);
        }

        calendarLayout.addView(labelRow);
    }

    /**
     * Create week row container
     */
    private LinearLayout createWeekRow() {
        LinearLayout weekRow = new LinearLayout(getContext());
        weekRow.setOrientation(LinearLayout.HORIZONTAL);
        weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return weekRow;
    }

    /**
     * Add day cell to calendar
     */
    private void addDayCell(LinearLayout weekRow, int day, boolean isToday, boolean isPast, boolean isCheckedIn) {
        TextView dayView = new TextView(getContext());

        // ✅ TEXT: Show checkmark if checked in today
        if (isCheckedIn) {
            dayView.setText(day + "\n✅");
        } else {
            dayView.setText(String.valueOf(day));
        }

        dayView.setTextSize(16);
        dayView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        dayView.setPadding(12, 16, 12, 16);
        dayView.setGravity(android.view.Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                100, // Fixed height
                1.0f
        );
        params.setMargins(4, 4, 4, 4);
        dayView.setLayoutParams(params);

        // ✅ STYLE: Different styles for different states
        if (isToday) {
            // Current day - Green background
            dayView.setBackgroundResource(R.drawable.bg_day_current);
            dayView.setTextColor(Color.WHITE);
            dayView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (isPast) {
            // Past days - Gray (could check if actually checked in)
            dayView.setBackgroundResource(R.drawable.bg_day_past);
            dayView.setTextColor(Color.parseColor("#666666"));
        } else {
            // Future days - Light gray
            dayView.setBackgroundResource(R.drawable.bg_day_future);
            dayView.setTextColor(Color.parseColor("#CCCCCC"));
        }

        weekRow.addView(dayView);
    }

    /**
     * Add empty cell for padding
     */
    private void addEmptyDayCell(LinearLayout weekRow) {
        View emptyView = new View(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                100,
                1.0f
        );
        params.setMargins(4, 4, 4, 4);
        emptyView.setLayoutParams(params);
        weekRow.addView(emptyView);
    }

    /**
     * Perform check-in
     */
    private void performCheckIn() {
        try {
            // ✅ FIX: Use existing method or add simple version
            boolean success = rewardManager.canCheckInToday();

            if (!success) {
                android.widget.Toast.makeText(getContext(),
                        "Already checked in today!",
                        android.widget.Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }

            // Simple reward (fixed amount for now)
            int reward = 10; // Base reward
            int streak = 1; // Default streak

            if (listener != null) {
                listener.onCheckIn(reward, streak);
            }

            android.widget.Toast.makeText(getContext(),
                    "✅ Checked in! +" + reward + " coins",
                    android.widget.Toast.LENGTH_SHORT).show();

            dismiss();

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error performing check-in", e);
            android.widget.Toast.makeText(getContext(),
                    "Error: " + e.getMessage(),
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}