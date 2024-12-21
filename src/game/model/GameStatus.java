/*
 * game.model.GameStatus.java
 * 게임의 상태를 나타내는 열거형
 */
package game.model;

public enum GameStatus {
    WAITING("대기중"),
    IN_PROGRESS("진행중"),
    FINISHED("종료됨");

    private final String displayName;

    GameStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}