package com.example.puzzle_assemble_picture;

public class GameMode {
    // Mode type constants
    public static final String MODE_EASY = "EASY";
    public static final String MODE_NORMAL = "NORMAL";
    public static final String MODE_HARD = "HARD";
    public static final String MODE_INSANE = "INSANE";

    // Instance properties
    private String modeType;
    private String displayName;
    private int iconResource;
    private boolean isLocked;
    private int requiredLevel;
    private String description; // THÊM PROPERTY NÀY

    // Constructor - Thêm parameter description
    public GameMode(String modeType, String displayName, int iconResource, boolean isLocked, int requiredLevel) {
        this.modeType = modeType;
        this.displayName = displayName;
        this.iconResource = iconResource;
        this.isLocked = isLocked;
        this.requiredLevel = requiredLevel;
        this.description = generateDescription(modeType); // Tự động tạo description
    }

    // Tạo description dựa trên mode type
    private String generateDescription(String modeType) {
        switch (modeType) {
            case MODE_EASY:
                return "Perfect for beginners!\n\n" +
                        "✓ Sample image shown\n" +
                        "✓ Pieces auto-lock when placed correctly\n" +
                        "✓ Locked pieces dim";

            case MODE_NORMAL:
                return "Standard challenge!\n\n" +
                        "✓ Sample image shown\n" +
                        "✗ No auto-lock\n" +
                        "✗ No dimming effect";

            case MODE_HARD:
                return "For experienced players!\n\n" +
                        "✗ No sample image\n" +
                        "✗ No auto-lock\n" +
                        "✗ No dimming\n" +
                        "Standard puzzle experience with zero assistance.";

            case MODE_INSANE:
                return "Ultimate challenge!\n\n" +
                        "✗ No sample image\n" +
                        "✗ No auto-lock\n" +
                        "✗ No dimming\n" +
                        "✓ Pieces start rotated randomly (90°–180°)\n" +
                        "✓ Limited mistakes allowed (too many → shuffle)\n" +
                        "✓ Time pressure — complete before the timer runs out\n" +
                        "Only for true masters!";

            default:
                return "Select this mode to start playing!";
        }
    }

    // Getters
    public String getModeType() {
        return modeType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return displayName;
    }

    public int getIconResource() {
        return iconResource;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public String getDescription() {
        return description;
    }

    // Setters
    public void setModeType(String modeType) {
        this.modeType = modeType;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}