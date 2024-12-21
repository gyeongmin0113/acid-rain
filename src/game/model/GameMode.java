/*
 * game.model.GameMode.java
 * 게임의 프로그래밍 언어 모드를 나타내는 열거형
 */
package game.model;

public enum GameMode {
    JAVA("Java"),
    PYTHON("Python"),
    KOTLIN("Kotlin"),
    C("C");

    private final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static GameMode fromDisplayName(String displayName) {
        for (GameMode mode : GameMode.values()) {
            if (mode.displayName.equalsIgnoreCase(displayName)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid GameMode: " + displayName);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
