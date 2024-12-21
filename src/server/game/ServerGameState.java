/*
 * server.game.ServerGameState.java
 * 게임 방의 상태를 관리
 * - 각 플레이어의 점수, pH를 관리
 * - 현재 활성화된 단어 목록을 관리
 * - 게임 시작/종료/진행 상태 관리
 */

package server.game;

import game.model.GameMode;
import game.model.DifficultyLevel;
import game.model.GameRoom;
import game.model.GameStatus;
import game.model.Word;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ServerGameState {
    private static final Logger logger = Logger.getLogger(ServerGameState.class.getName());
    private static final double INITIAL_PH = 7.0;
    private static final double MIN_PH = 0.0;

    private final GameRoom room;
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final Map<String, Double> phValues = new ConcurrentHashMap<>();
    private final List<Word> activeWords = Collections.synchronizedList(new ArrayList<>());
    private volatile GameStatus status = GameStatus.WAITING;

    public ServerGameState(GameRoom room) {
        this.room = room;
        initializePlayers();
        logger.info("게임 상태 초기화: " + room.getRoomId());
    }

    private void initializePlayers() {
        synchronized (this) {
            for (String player : room.getPlayers()) {
                scores.put(player, 0);
                phValues.put(player, INITIAL_PH);
            }
        }
    }

    public void start() {
        synchronized (this) {
            status = GameStatus.IN_PROGRESS;
            // 게임 시작 시 모든 플레이어의 상태 리셋
            initializePlayers();
            activeWords.clear();
        }
        logger.info("게임 시작: " + room.getRoomId());
    }

    public void end() {
        synchronized (this) {
            status = GameStatus.FINISHED;
            // 게임 종료 시 단어 목록 클리어
            activeWords.clear();
        }
        logger.info("게임 종료: " + room.getRoomId());
    }

    public GameStatus getStatus() {
        return status;
    }

    public synchronized void addWord(Word word) {
        activeWords.add(word);
    }

    public synchronized Word removeWord(String text) {
        Optional<Word> word = activeWords.stream()
                .filter(w -> w.getText().equals(text))
                .findFirst();
        word.ifPresent(activeWords::remove);
        return word.orElse(null);
    }

    public List<Word> getActiveWords() {
        synchronized (activeWords) {
            return new ArrayList<>(activeWords);
        }
    }

    public Word matchWord(String typedWord, String player) {
        synchronized (this) {
            Optional<Word> matched = activeWords.stream()
                    .filter(w -> w.getText().equals(typedWord))
                    .findFirst();

            if (matched.isPresent()) {
                Word word = matched.get();
                activeWords.remove(word);

                // 점수 계산 및 pH 변경 로직
                int basePoints = calculateBasePoints(word);
                int finalPoints = calculateFinalPoints(word, basePoints);
                addScore(player, finalPoints);
                adjustPH(player, 0.3);

                String opponent = getOpponentOf(player);
                if (opponent != null) {
                    decreasePH(opponent, 0.2);
                }

                return word; // 매칭된 단어 반환
            }
            return null;
        }
    }

    private int calculateBasePoints(Word word) {
        return word.getText().length() * 10;
    }

    private int calculateFinalPoints(Word word, int basePoints) {
        if (word.hasSpecialEffect() && word.getEffect() == Word.SpecialEffect.SCORE_BOOST) {
            return (int)(basePoints * 1.5);
        }
        return basePoints;
    }

    public void addScore(String player, int points) {
        scores.computeIfPresent(player, (k, v) -> v + points);
        logger.fine(String.format("점수 추가 - 플레이어: %s, 점수: %d", player, points));
    }

    public void decreasePH(String player, double amount) {
        synchronized (this) {
            phValues.computeIfPresent(player, (k, v) -> Math.max(MIN_PH, v - amount));
            logger.fine(String.format("pH 감소 - 플레이어: %s, 감소량: %.2f", player, amount));
        }
    }

    public void adjustPH(String player, double amount) {
        synchronized (this) {
            phValues.computeIfPresent(player, (k, v) ->
                    Math.min(INITIAL_PH, Math.max(MIN_PH, v + amount)));
            logger.fine(String.format("pH 조정 - 플레이어: %s, 조정량: %.2f", player, amount));
        }
    }

    public double getPlayerPH(String player) {
        return phValues.getOrDefault(player, MIN_PH);
    }

    public boolean isGameOver() {
        synchronized (this) {
            return phValues.values().stream().anyMatch(ph -> ph <= MIN_PH);
        }
    }

    public String getWinner() {
        synchronized (this) {
            // 살아있는 플레이어 찾기
            List<String> alivePlayers = new ArrayList<>();
            for (String player : room.getPlayers()) {
                if (phValues.getOrDefault(player, MIN_PH) > MIN_PH) {
                    alivePlayers.add(player);
                }
            }

            // 승자 결정
            if (alivePlayers.size() == 1) {
                return alivePlayers.get(0);
            } else if (alivePlayers.isEmpty()) {
                // 모두 죽었으면 점수로 결정
                return determineWinnerByScore();
            } else {
                // 여러 명 생존 시 점수로 결정
                return determineWinnerByScore(alivePlayers);
            }
        }
    }

    private String determineWinnerByScore() {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(room.getPlayers()[0]);
    }

    private String determineWinnerByScore(List<String> players) {
        return players.stream()
                .max(Comparator.comparingInt(this::getPlayerScore))
                .orElse(players.get(0));
    }

    public int getPlayerScore(String player) {
        return scores.getOrDefault(player, 0);
    }

    public int getOpponentScore(String player) {
        synchronized (this) {
            for (String p : scores.keySet()) {
                if (!p.equals(player)) return scores.get(p);
            }
            return 0;
        }
    }

    public String getOpponentOf(String player) {
        synchronized (this) {
            return room.getPlayers()[0].equals(player) ?
                    room.getPlayers()[1] : room.getPlayers()[0];
        }
    }

    public GameMode getGameMode() {
        return room.getGameMode();
    }

    public DifficultyLevel getDifficulty() {
        return room.getDifficulty();
    }

    public Map<String, Integer> getScores() {
        return new HashMap<>(scores);
    }

    public Map<String, Double> getPHValues() {
        return new HashMap<>(phValues);
    }
}