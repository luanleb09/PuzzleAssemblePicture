package com.example.puzzle_assemble_picture;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // Giảm xuống 2 giây
    private Handler handler;
    private Runnable splashRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splashLogo);
        TextView appName = findViewById(R.id.splashTitle);


        // ✅ Dùng AlphaAnimation thay vì load từ system (nhanh hơn)
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(800);
        fadeIn.setFillAfter(true);

        logo.startAnimation(fadeIn);
        appName.startAnimation(fadeIn);

        // ✅ Dùng Handler với Looper.getMainLooper() (modern way)
        handler = new Handler(Looper.getMainLooper());

        splashRunnable = () -> {
            // ✅ Check nếu activity vẫn còn active
            if (!isFinishing() && !isDestroyed()) {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        };

        handler.postDelayed(splashRunnable, SPLASH_DURATION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ QUAN TRỌNG: Remove callback để tránh memory leak
        if (handler != null && splashRunnable != null) {
            handler.removeCallbacks(splashRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        // ✅ Disable back button trong splash screen
        // Không làm gì cả
    }
}