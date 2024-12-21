/*
 * game.model.DifficultyLevel.java
 * 게임의 난이도를 나타내는 열거형
 */
package game.model;

public enum DifficultyLevel {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    private final String displayName;

    DifficultyLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DifficultyLevel fromDisplayName(String displayName) {
        for (DifficultyLevel level : DifficultyLevel.values()) {
            if (level.displayName.equalsIgnoreCase(displayName)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid DifficultyLevel: " + displayName);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
