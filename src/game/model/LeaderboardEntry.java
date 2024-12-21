package game.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class LeaderboardEntry implements Comparable<LeaderboardEntry> {
    private final String username;
    private final int score;
    private final GameMode gameMode;
    private final DifficultyLevel difficulty;
    private final LocalDateTime timestamp;

    public LeaderboardEntry(String username, int score, GameMode gameMode,
                            DifficultyLevel difficulty, LocalDateTime timestamp) {
        this.username = username;
        this.score = score;
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.timestamp = timestamp;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(LeaderboardEntry other) {
        // 점수로 먼저 비교
        int scoreCompare = Integer.compare(other.score, this.score);
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        // 점수가 같으면 시간순으로 (최신이 앞으로)
        return other.timestamp.compareTo(this.timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaderboardEntry that = (LeaderboardEntry) o;
        return score == that.score &&
                Objects.equals(username, that.username) &&
                gameMode == that.gameMode &&
                difficulty == that.difficulty &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, score, gameMode, difficulty, timestamp);
    }

    @Override
    public String toString() {
        return String.format("%s - %d points (%s, %s) at %s",
                username, score, gameMode.getDisplayName(),
                difficulty.getDisplayName(), timestamp);
    }

    public String toFileString() {
        return String.format("%s,%d,%s,%s,%s",
                username, score, gameMode.name(), difficulty.name(),
                timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    public static LeaderboardEntry fromString(String str) {
        try {
            String[] fields = str.split(",");
            if (fields.length != 5) {
                throw new IllegalArgumentException("필드 수가 잘못되었습니다: " + fields.length);
            }

            String username = fields[0].trim();
            int score = Integer.parseInt(fields[1].trim());
            GameMode mode = GameMode.valueOf(fields[2].trim());
            DifficultyLevel difficulty = DifficultyLevel.valueOf(fields[3].trim());
            LocalDateTime timestamp = LocalDateTime.parse(fields[4].trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            return new LeaderboardEntry(username, score, mode, difficulty, timestamp);
        } catch (Exception e) {
            throw new IllegalArgumentException("엔트리 파싱 실패: " + str + ", 오류: " + e.getMessage());
        }
    }

    // 랭킹 표시용 문자열 생성
    public String toDisplayString(int rank) {
        return String.format("#%d   %s   %,d점   %s   %s",
                rank, username, score,
                gameMode.getDisplayName(),
                difficulty.getDisplayName());
    }
}