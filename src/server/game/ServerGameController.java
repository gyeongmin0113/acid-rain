package server.game;

import client.event.GameEvent.ServerMessage;
import game.model.DifficultyLevel;
import game.model.GameRoom;
import game.model.GameStatus;
import game.model.Word;

import server.GameServer;
import server.ClientHandler;

import java.util.concurrent.*;
import java.util.logging.Logger;

public class ServerGameController {
    private static final Logger logger = Logger.getLogger(ServerGameController.class.getName());
    private final GameServer server;
    private final GameRoom room;
    private final ServerGameState gameState;
    private final ServerWordManager wordManager;
    private final LeaderboardManager leaderboardManager;

    // 스케줄링을 위한 스레드 풀 -> 단어 생성, pH 체크
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> spawnTask;
    private ScheduledFuture<?> phCheckTask;

    private static final double PH_CHECK_INTERVAL = 1.0; // 초
    private static final double PH_DECREASE_AMOUNT = 0.2;
    private static final int BLIND_EFFECT_DURATION = 5000; // 5초

    public ServerGameController(GameServer server, GameRoom room) {
        this.server = server;
        this.room = room;
        this.gameState = new ServerGameState(room);
        this.wordManager = new ServerWordManager(room.getGameMode());
        this.leaderboardManager = LeaderboardManager.getInstance();

        logger.info("게임 컨트롤러 생성: " + room.getRoomId());
    }

    public void startGame() {
        try {
            gameState.start();
            long spawnInterval = calculateWordSpawnInterval(room.getDifficulty());

            // 단어 생성 작업 시작
            spawnTask = scheduler.scheduleAtFixedRate(this::spawnWord,
                    0, spawnInterval, TimeUnit.MILLISECONDS);

            // pH 체크 작업 시작
            phCheckTask = scheduler.scheduleAtFixedRate(this::checkPH,
                    0, (long)(PH_CHECK_INTERVAL * 1000), TimeUnit.MILLISECONDS);

            logger.info("게임 시작됨: " + room.getRoomId());
        } catch (Exception e) {
            logger.severe("게임 시작 중 오류 발생: " + e.getMessage());
            stopGame();
            server.broadcastToRoom(room.getRoomId(), "ERROR|게임 시작 실패");
        }
    }

    private void spawnWord() {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            Word word = wordManager.getRandomWord();
            gameState.addWord(word);

            // 특수 효과 정보를 포함한 메시지 구성
            String spawnMessage;
            if (word.hasSpecialEffect()) {
                spawnMessage = String.format(ServerMessage.WORD_SPAWNED + "|%s|%s|%d|%s",
                        room.getRoomId(),
                        word.getText(),
                        word.getX(),
                        word.getEffect().name());  // SCORE_BOOST 또는 BLIND_OPPONENT
            } else {
                spawnMessage = String.format(ServerMessage.WORD_SPAWNED + "|%s|%s|%d",
                        room.getRoomId(),
                        word.getText(),
                        word.getX());
            }

            server.broadcastToRoom(room.getRoomId(), spawnMessage);
            logger.fine("단어 생성: " + word.getText() +
                    (word.hasSpecialEffect() ? ", 효과: " + word.getEffect() : ""));
        } catch (Exception e) {
            logger.severe("단어 생성 중 오류: " + e.getMessage());
        }
    }

    private void checkPH() {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            // 단순히 pH가 0 이하인지만 체크
            for (String player : room.getPlayers()) {
                double ph = gameState.getPlayerPH(player);
                if (ph <= 0) {
                    handleGameOver();
                    return;
                }
            }
        } catch (Exception e) {
            logger.severe("pH 체크 중 오류: " + e.getMessage());
        }
    }

    public void handlePlayerInput(ClientHandler player, String typedWord) {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            Word matchedWord = gameState.matchWord(typedWord, player.getUsername());
            if (matchedWord != null) {
                int newScore = gameState.getPlayerScore(player.getUsername());
                String opponent = gameState.getOpponentOf(player.getUsername());
                double opponentPH = opponent != null ? gameState.getPlayerPH(opponent) : 0.0;
                double playerPH = gameState.getPlayerPH(player.getUsername());

                // WORD_MATCHED 메시지 전송 (점수 정보 포함)
                server.broadcastToRoom(room.getRoomId(),
                        String.format(ServerMessage.WORD_MATCHED + "|%s|%s|%s|%d",
                                room.getRoomId(), matchedWord.getText(), player.getUsername(), newScore));

                // pH 업데이트 메시지
                server.broadcastToRoom(room.getRoomId(),
                        String.format(ServerMessage.PH_UPDATE + "|%s|%s|%.2f",
                                room.getRoomId(), player.getUsername(), playerPH));

                if (opponent != null) {
                    server.broadcastToRoom(room.getRoomId(),
                            String.format(ServerMessage.PH_UPDATE + "|%s|%s|%.2f",
                                    room.getRoomId(), opponent, opponentPH));
                }

                // 특수효과 처리
                if (matchedWord.hasSpecialEffect()) {
                    switch (matchedWord.getEffect()) {
                        case BLIND_OPPONENT:
                            if (opponent != null) {
                                // BLIND_EFFECT 메시지 전송 (예: 5초=5000ms)
                                server.broadcastToRoom(room.getRoomId(),
                                        String.format(ServerMessage.BLIND_EFFECT + "|%s|%s|%d",
                                                room.getRoomId(), opponent, BLIND_EFFECT_DURATION));
                            }
                            break;
                        case SCORE_BOOST:
                            // SCORE_BOOST는 점수 계산 시 이미 반영됨
                            break;
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("단어 입력 처리 중 오류: " + e.getMessage());
        }
    }

    public void handleWordMissed(String word, ClientHandler player) {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            synchronized(gameState) {
                gameState.removeWord(word);  // 단어 제거

                // 모든 플레이어의 pH 감소
                for (String playerName : room.getPlayers()) {
                    gameState.decreasePH(playerName, PH_DECREASE_AMOUNT);  // 단어 놓칠 때마다 0.2 감소
                    double newPH = gameState.getPlayerPH(playerName);

                    // pH 감소 메시지 전송
                    server.broadcastToRoom(room.getRoomId(),
                            String.format(ServerMessage.WORD_MISSED + "|%s|%s|%s|%.2f",
                                    room.getRoomId(), word, playerName, newPH));

                    if (newPH <= 0) {
                        handleGameOver();
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("단어 놓침 처리 중 오류: " + e.getMessage());
        }
    }

    public void handlePlayerLeaveGame(ClientHandler player) {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            String leavingPlayer = player.getUsername();
            String opponent = gameState.getOpponentOf(leavingPlayer);

            // 게임 상태 종료
            stopGame();

            if (opponent != null) {
                // 점수 정보 가져오기
                int winnerScore = gameState.getPlayerScore(opponent);
                int loserScore = gameState.getPlayerScore(leavingPlayer);

                // 리더보드 등록 시도
                if (leaderboardManager.addEntry(opponent, winnerScore,
                        room.getGameMode(), room.getDifficulty())) {
                    int rank = leaderboardManager.getUserRank(opponent,
                            room.getGameMode(), room.getDifficulty());
                    server.broadcastToRoom(room.getRoomId(),
                            ServerMessage.LEADERBOARD_UPDATE + "|" + room.getRoomId() + "|" + opponent + "|" + rank);
                }

                // 게임 종료 메시지 전송 (몰수승/패 처리)
                server.broadcastToRoom(room.getRoomId(),
                        String.format(ServerMessage.GAME_OVER + "|%s|%s|%d|%d|FORFEIT",
                                room.getRoomId(), opponent, winnerScore, loserScore));
            }

            logger.info("플레이어 게임 중 퇴장 (몰수패): " + leavingPlayer);
        } catch (Exception e) {
            logger.severe("플레이어 퇴장 처리 중 오류: " + e.getMessage());
        }
    }

    // 게임 종료 처리
    private void handleGameOver() {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            String winner = gameState.getWinner();
            if (winner != null) {
                int winnerScore = gameState.getPlayerScore(winner);
                int loserScore = gameState.getOpponentScore(winner);

                // 리더보드 등록 시도
                if (leaderboardManager.addEntry(winner, winnerScore,
                        gameState.getGameMode(), gameState.getDifficulty())) {
                    int rank = leaderboardManager.getUserRank(winner,
                            gameState.getGameMode(), gameState.getDifficulty());
                    server.broadcastToRoom(room.getRoomId(),
                            ServerMessage.LEADERBOARD_UPDATE + room.getRoomId() + "|" + winner + "|" + rank);
                }

                server.broadcastToRoom(room.getRoomId(),
                        String.format(ServerMessage.GAME_OVER + "|%s|%s|%d|%d",
                                room.getRoomId(), winner, winnerScore, loserScore));
            }

            stopGame();
            logger.info("게임 종료: " + room.getRoomId() + ", 승자: " + winner);
        } catch (Exception e) {
            logger.severe("게임 종료 처리 중 오류: " + e.getMessage());
        }
    }

    public void stopGame() {
        try {
            if (spawnTask != null) {
                spawnTask.cancel(false);
            }
            if (phCheckTask != null) {
                phCheckTask.cancel(false);
            }
            scheduler.shutdown();
            gameState.end();

            logger.info("게임 중지됨: " + room.getRoomId());
        } catch (Exception e) {
            logger.severe("게임 중지 중 오류: " + e.getMessage());
        }
    }

    private long calculateWordSpawnInterval(DifficultyLevel diff) {
        return switch (diff) {
            case EASY -> 4000;    // 4초
            case MEDIUM -> 3500;  // 3.5초
            case HARD -> 2000;    // 2초
        };
    }
}
