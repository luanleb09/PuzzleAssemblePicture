package com.example.puzzle_assemble_picture;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    // SharedPreferences keys
    public static final String PREFS_NAME = "GameSettings";
    public static final String KEY_MUSIC_ENABLED = "music_enabled";
    public static final String KEY_SOUND_ENABLED = "sound_enabled";
    public static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    public static final String KEY_MUSIC_VOLUME = "music_volume";
    public static final String KEY_SOUND_VOLUME = "sound_volume";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_SHOW_HINTS = "show_hints";
    public static final String KEY_AUTO_SAVE = "auto_save";
    public static final String KEY_THEME = "theme";

    private SharedPreferences prefs;
    private boolean hasChanges = false;

    // Views
    private SwitchCompat switchMusic;
    private SwitchCompat switchSound;
    private SwitchCompat switchVibration;
    private SwitchCompat switchHints;
    private SwitchCompat switchAutoSave;

    private SeekBar seekBarMusic;
    private SeekBar seekBarSound;

    private TextView tvMusicVolume;
    private TextView tvSoundVolume;

    private Spinner spinnerLanguage;
    private Spinner spinnerTheme;

    private Button btnSave;
    private Button btnCancel;
    private Button btnResetDefaults;

    // Store original values for cancel
    private boolean originalMusic;
    private boolean originalSound;
    private boolean originalVibration;
    private boolean originalHints;
    private boolean originalAutoSave;
    private int originalMusicVolume;
    private int originalSoundVolume;
    private String originalLanguage;
    private String originalTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initializeViews();
        loadCurrentSettings();
        saveOriginalSettings();
        setupListeners();
    }

    private void initializeViews() {
        // Switches
        switchMusic = findViewById(R.id.switchMusic);
        switchSound = findViewById(R.id.switchSound);
        switchVibration = findViewById(R.id.switchVibration);
        switchHints = findViewById(R.id.switchHints);
        switchAutoSave = findViewById(R.id.switchAutoSave);

        // SeekBars
        seekBarMusic = findViewById(R.id.seekBarMusic);
        seekBarSound = findViewById(R.id.seekBarSound);

        // TextViews
        tvMusicVolume = findViewById(R.id.tvMusicVolume);
        tvSoundVolume = findViewById(R.id.tvSoundVolume);

        // Spinners
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        spinnerTheme = findViewById(R.id.spinnerTheme);

        // Buttons
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnResetDefaults = findViewById(R.id.btnResetDefaults);

        // Setup language spinner
        ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.languages,
                android.R.layout.simple_spinner_item
        );
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(languageAdapter);

        // Setup theme spinner
        ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.themes,
                android.R.layout.simple_spinner_item
        );
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(themeAdapter);
    }

    private void loadCurrentSettings() {
        // Load switches
        switchMusic.setChecked(prefs.getBoolean(KEY_MUSIC_ENABLED, true));
        switchSound.setChecked(prefs.getBoolean(KEY_SOUND_ENABLED, true));
        switchVibration.setChecked(prefs.getBoolean(KEY_VIBRATION_ENABLED, true));
        switchHints.setChecked(prefs.getBoolean(KEY_SHOW_HINTS, true));
        switchAutoSave.setChecked(prefs.getBoolean(KEY_AUTO_SAVE, true));

        // Load volumes
        int musicVolume = prefs.getInt(KEY_MUSIC_VOLUME, 70);
        int soundVolume = prefs.getInt(KEY_SOUND_VOLUME, 80);

        seekBarMusic.setProgress(musicVolume);
        seekBarSound.setProgress(soundVolume);

        tvMusicVolume.setText(musicVolume + "%");
        tvSoundVolume.setText(soundVolume + "%");

        // Load language
        String language = prefs.getString(KEY_LANGUAGE, "en");
        spinnerLanguage.setSelection(getLanguagePosition(language));

        // Load theme
        String theme = prefs.getString(KEY_THEME, "auto");
        spinnerTheme.setSelection(getThemePosition(theme));

        // Update volume seekbar states
        seekBarMusic.setEnabled(switchMusic.isChecked());
        seekBarSound.setEnabled(switchSound.isChecked());
    }

    private void saveOriginalSettings() {
        originalMusic = switchMusic.isChecked();
        originalSound = switchSound.isChecked();
        originalVibration = switchVibration.isChecked();
        originalHints = switchHints.isChecked();
        originalAutoSave = switchAutoSave.isChecked();
        originalMusicVolume = seekBarMusic.getProgress();
        originalSoundVolume = seekBarSound.getProgress();
        originalLanguage = prefs.getString(KEY_LANGUAGE, "en");
        originalTheme = prefs.getString(KEY_THEME, "auto");
    }

    private void setupListeners() {
        // Switch listeners
        switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hasChanges = true;
            seekBarMusic.setEnabled(isChecked);
            if (!isChecked) {
                tvMusicVolume.setAlpha(0.5f);
            } else {
                tvMusicVolume.setAlpha(1.0f);
            }
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hasChanges = true;
            seekBarSound.setEnabled(isChecked);
            if (!isChecked) {
                tvSoundVolume.setAlpha(0.5f);
            } else {
                tvSoundVolume.setAlpha(1.0f);
            }
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> hasChanges = true);
        switchHints.setOnCheckedChangeListener((buttonView, isChecked) -> hasChanges = true);
        switchAutoSave.setOnCheckedChangeListener((buttonView, isChecked) -> hasChanges = true);

        // SeekBar listeners
        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMusicVolume.setText(progress + "%");
                if (fromUser) hasChanges = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarSound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSoundVolume.setText(progress + "%");
                if (fromUser) hasChanges = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Spinner listeners
        spinnerLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                hasChanges = true;
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerTheme.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                hasChanges = true;
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Button listeners
        btnSave.setOnClickListener(v -> saveSettings());
        btnCancel.setOnClickListener(v -> cancelSettings());
        btnResetDefaults.setOnClickListener(v -> showResetConfirmation());
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();

        // Save all settings
        editor.putBoolean(KEY_MUSIC_ENABLED, switchMusic.isChecked());
        editor.putBoolean(KEY_SOUND_ENABLED, switchSound.isChecked());
        editor.putBoolean(KEY_VIBRATION_ENABLED, switchVibration.isChecked());
        editor.putBoolean(KEY_SHOW_HINTS, switchHints.isChecked());
        editor.putBoolean(KEY_AUTO_SAVE, switchAutoSave.isChecked());

        editor.putInt(KEY_MUSIC_VOLUME, seekBarMusic.getProgress());
        editor.putInt(KEY_SOUND_VOLUME, seekBarSound.getProgress());

        editor.putString(KEY_LANGUAGE, getLanguageCode(spinnerLanguage.getSelectedItemPosition()));
        editor.putString(KEY_THEME, getThemeCode(spinnerTheme.getSelectedItemPosition()));

        editor.apply();

        Toast.makeText(this, "✓ Settings saved!", Toast.LENGTH_SHORT).show();
        hasChanges = false;
        finish();
    }

    private void cancelSettings() {
        if (hasChanges) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Discard Changes?");
            builder.setMessage("You have unsaved changes. Are you sure you want to discard them?");
            builder.setPositiveButton("Discard", (dialog, which) -> {
                restoreOriginalSettings();
                finish();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } else {
            finish();
        }
    }

    private void restoreOriginalSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_MUSIC_ENABLED, originalMusic);
        editor.putBoolean(KEY_SOUND_ENABLED, originalSound);
        editor.putBoolean(KEY_VIBRATION_ENABLED, originalVibration);
        editor.putBoolean(KEY_SHOW_HINTS, originalHints);
        editor.putBoolean(KEY_AUTO_SAVE, originalAutoSave);
        editor.putInt(KEY_MUSIC_VOLUME, originalMusicVolume);
        editor.putInt(KEY_SOUND_VOLUME, originalSoundVolume);
        editor.putString(KEY_LANGUAGE, originalLanguage);
        editor.putString(KEY_THEME, originalTheme);
        editor.apply();
    }

    private void showResetConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset to Defaults?");
        builder.setMessage("This will reset all settings to their default values. Continue?");
        builder.setPositiveButton("Reset", (dialog, which) -> resetToDefaults());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void resetToDefaults() {
        // Reset to default values
        switchMusic.setChecked(true);
        switchSound.setChecked(true);
        switchVibration.setChecked(true);
        switchHints.setChecked(true);
        switchAutoSave.setChecked(true);

        seekBarMusic.setProgress(70);
        seekBarSound.setProgress(80);

        spinnerLanguage.setSelection(0);
        spinnerTheme.setSelection(0);

        hasChanges = true;
        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
    }

    private int getLanguagePosition(String languageCode) {
        switch (languageCode) {
            case "en": return 0;
            case "vi": return 1;
            default: return 0;
        }
    }

    private String getLanguageCode(int position) {
        switch (position) {
            case 0: return "en";
            case 1: return "vi";
            default: return "en";
        }
    }

    private int getThemePosition(String themeCode) {
        switch (themeCode) {
            case "auto": return 0;
            case "light": return 1;
            case "dark": return 2;
            default: return 0;
        }
    }

    private String getThemeCode(int position) {
        switch (position) {
            case 0: return "auto";
            case 1: return "light";
            case 2: return "dark";
            default: return "auto";
        }
    }

    @Override
    public void onBackPressed() {
        cancelSettings();
    }

    // ===== STATIC HELPER METHODS =====

    public static boolean isMusicEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_MUSIC_ENABLED, true);
    }

    public static boolean isSoundEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public static boolean isVibrationEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    public static int getMusicVolume(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_MUSIC_VOLUME, 70);
    }

    public static int getSoundVolume(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_SOUND_VOLUME, 80);
    }

    public static float getMusicVolumeFloat(android.content.Context context) {
        return getMusicVolume(context) / 100f;
    }

    public static float getSoundVolumeFloat(android.content.Context context) {
        return getSoundVolume(context) / 100f;
    }

    public static boolean isHintsEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SHOW_HINTS, true);
    }

    public static boolean isAutoSaveEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_SAVE, true);
    }

    public static String getLanguage(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    public static String getTheme(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_THEME, "auto");
    }
}