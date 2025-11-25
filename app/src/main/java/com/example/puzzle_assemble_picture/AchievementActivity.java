package com.example.puzzle_assemble_picture;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AchievementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create simple layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(0xFFF5F5F5);

        // Title
        TextView title = new TextView(this);
        title.setText("🏆 Achievements");
        title.setTextSize(32);
        title.setTextColor(0xFF333333);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        // Message
        TextView message = new TextView(this);
        message.setText("\n\nAchievements feature\ncoming soon!\n\nTap anywhere to go back");
        message.setTextSize(18);
        message.setTextColor(0xFF666666);
        message.setGravity(android.view.Gravity.CENTER);
        message.setPadding(0, 24, 0, 0);
        layout.addView(message);

        setContentView(layout);

        // Click to close
        layout.setOnClickListener(v -> finish());
    }
}