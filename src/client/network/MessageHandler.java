/*
 * client.network.MessageHandler.java
 * 서버로부터 수신된 메시지를 처리하는 클래스
 */

package client.network;

import client.app.GameClient;
import client.event.GameEvent.ClientEvent;
import client.event.GameEvent.ServerMessage;
import game.model.Word;

import java.util.Arrays;
import java.util.logging.Logger;

public class MessageHandler {
    private static final Logger logger = Logger.getLogger(MessageHandler.class.getName());
    private final GameClient gameClient;

    public MessageHandler(GameClient gameClient) {
        this.gameClient = gameClient;
    }

    public void handleMessage(String message) {
        try {
            logger.info("수신된 메시지: " + message);
            String[] parts = message.split("\\|");
            String messageType = parts[0];

            switch (messageType) {
                // 유저 관련 메시지
                case ServerMessage.USERS -> handleUsers(parts);

                // 방 관련 메시지
                case ServerMessage.ROOM_LIST_RESPONSE -> handleRoomList(parts);
                case ServerMessage.PLAYER_LIST_RESPONSE -> handlePlayerList(parts);
                case ServerMessage.CREATE_ROOM_RESPONSE -> handleCreateRoom(parts);
                case ServerMessage.JOIN_ROOM_RESPONSE -> handleJoinRoom(parts);
                case ServerMessage.ROOM_CLOSED -> handleRoomClosed(parts);
                case ServerMessage.HOST_LEFT -> handleHostLeft(parts);
                case ServerMessage.NEW_HOST -> handleNewHost(parts);

                // 채팅 메시지
                case ServerMessage.CHAT -> handleChat(parts);

                // 게임 상태 메시지
                case ServerMessage.PLAYER_UPDATE -> handlePlayerUpdate(parts);
                case ServerMessage.SETTINGS_UPDATE -> handleSettingsUpdate(parts);
                case ServerMessage.GAME_START -> handleGameStart();

                // 게임 플레이 메시지
                case ServerMessage.WORD_SPAWNED -> handleWordSpawned(parts);
                case ServerMessage.WORD_MATCHED -> handleWordMatched(parts);
                case ServerMessage.WORD_MISSED -> handleWordMissed(parts);
                case ServerMessage.BLIND_EFFECT -> handleBlindEffect(parts);
                case ServerMessage.GAME_OVER -> handleGameOver(parts);
                case ServerMessage.PH_UPDATE -> handlePHUpdate(parts);

                // 리더보드 관련 메시지
                case ServerMessage.LEADERBOARD_DATA -> handleLeaderboardData(parts);
                case ServerMessage.LEADERBOARD_UPDATE -> handleLeaderboardUpdate(parts);
                case ServerMessage.MY_RECORDS_DATA -> handleMyRecordsData(parts);

                // 에러 메시지
                case ServerMessage.ERROR -> handleError(parts);

                default -> logger.warning("알 수 없는 메시지 타입: " + messageType);
            }
        } catch (Exception e) {
            logger.severe("메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUsers(String[] parts) {
        if (parts.length >= 2) {
            try {
                int userCount = Integer.parseInt(parts[1]);
                gameClient.handleEvent(ClientEvent.USERS_UPDATED, userCount);
            } catch (NumberFormatException e) {
                logger.severe("사용자 수 파싱 오류: " + parts[1]);
            }
        }
    }

    private void handleRoomList(String[] parts) {
        if (parts.length > 1) {
            String[] roomInfos = Arrays.copyOfRange(parts, 1, parts.length);
            gameClient.handleEvent(ClientEvent.ROOM_LIST_UPDATED, (Object[]) roomInfos);
        } else {
            gameClient.handleEvent(ClientEvent.ROOM_LIST_UPDATED, new String[0]);
        }
    }

    private void handlePlayerList(String[] parts) {
        if (parts.length >= 4) {
            String roomId = parts[1];
            try {
                int playerCount = Integer.parseInt(parts[2]);
                String[] players = parts[3].split(";");
                gameClient.handleEvent(ClientEvent.PLAYER_UPDATED, roomId, playerCount, players);
            } catch (NumberFormatException e) {
                logger.severe("플레이어 수 파싱 오류: " + parts[2]);
            }
        }
    }

    private void handleCreateRoom(String[] parts) {
        if (parts.length >= 3) {
            boolean success = Boolean.parseBoolean(parts[1]);
            String msg = parts[2];
            if (success && parts.length >= 5) {
                String roomInfoStr = parts[3];
                String createdRoomId = parts[4];
                gameClient.handleEvent(ClientEvent.ROOM_CREATED, success, msg, roomInfoStr, createdRoomId);
                gameClient.handleEvent(ClientEvent.ROOM_JOINED, true, "방에 입장했습니다.", roomInfoStr, createdRoomId);
            } else {
                gameClient.handleEvent(ClientEvent.ROOM_CREATED, success, msg, null, null);
            }
        }
    }

    private void handleJoinRoom(String[] parts) {
        if (parts.length >= 3) {
            boolean success = Boolean.parseBoolean(parts[1]);
            String joinMsg = parts[2];
            if (success && parts.length >= 4) {
                String roomInfoStr = parts[3];
                gameClient.handleEvent(ClientEvent.ROOM_JOINED, success, joinMsg, roomInfoStr);
            } else {
                gameClient.handleEvent(ClientEvent.ROOM_JOINED, success, joinMsg, null);
            }
        }
    }

    private void handlePlayerUpdate(String[] parts) {
        if (parts.length >= 4) {
            String roomId = parts[1];
            try {
                int playerCount = Integer.parseInt(parts[2]);
                String[] players = parts[3].split(";");
                gameClient.handleEvent(ClientEvent.PLAYER_UPDATED, roomId, playerCount, players);
            } catch (NumberFormatException e) {
                logger.severe("플레이어 수 파싱 오류: " + parts[2]);
            }
        }
    }

    private void handleSettingsUpdate(String[] parts) {
        if (parts.length >= 4) {
            String roomId = parts[1];
            String mode = parts[2];
            String diff = parts[3];
            gameClient.handleEvent(ClientEvent.SETTINGS_UPDATED, roomId, mode, diff);
        }
    }

    private void handleGameStart() {
        gameClient.handleEvent(ClientEvent.GAME_STARTED);
    }

    private void handleWordSpawned(String[] parts) {
        if (parts.length >= 4) {
            String wordText = parts[2];
            try {
                int xPos = Integer.parseInt(parts[3]);
                if (parts.length >= 5) {
                    // 특수 효과가 있는 경우
                    Word.SpecialEffect effect = Word.SpecialEffect.valueOf(parts[4]);
                    gameClient.handleEvent(ClientEvent.WORD_SPAWNED, wordText, xPos, effect);
                } else {
                    // 일반 단어인 경우
                    gameClient.handleEvent(ClientEvent.WORD_SPAWNED, wordText, xPos);
                }
            } catch (NumberFormatException e) {
                logger.severe("단어 위치 파싱 오류: " + parts[3]);
            } catch (IllegalArgumentException e) {
                logger.severe("특수 효과 파싱 오류: " + parts[4]);
            }
        }
    }

    private void handlePHUpdate(String[] parts) {
        if (parts.length >= 4) {
            String roomId = parts[1];
            String playerName = parts[2];
            try {
                double newPH = Double.parseDouble(parts[3]);
                gameClient.handleEvent(ClientEvent.PH_UPDATE, playerName, newPH);
            } catch (NumberFormatException e) {
                logger.severe("pH 값 파싱 오류: " + parts[3]);
            }
        }
    }

    private void handleWordMatched(String[] parts) {
        if (parts.length >= 5) {
            String wordText = parts[2];
            String playerName = parts[3];
            try {
                int newScore = Integer.parseInt(parts[4]);
                gameClient.handleEvent(ClientEvent.WORD_MATCHED, wordText, playerName, newScore);
            } catch (NumberFormatException e) {
                logger.severe("점수 파싱 오류: " + parts[4]);
            }
        }
    }

    private void handleWordMissed(String[] parts) {
        if (parts.length >= 5) {
            String missedWord = parts[2];
            String playerName = parts[3];
            try {
                double newPH = Double.parseDouble(parts[4]);
                gameClient.handleEvent(ClientEvent.WORD_MISSED, missedWord, playerName, newPH);
            } catch (NumberFormatException e) {
                logger.severe("pH 값 파싱 오류: " + parts[4]);
            }
        }
    }

    private void handleBlindEffect(String[] parts) {
        if (parts.length >= 4) {
            String targetPlayer = parts[2];
            try {
                int durationMs = Integer.parseInt(parts[3]);
                gameClient.handleEvent(ClientEvent.BLIND_EFFECT, targetPlayer, durationMs);
            } catch (NumberFormatException e) {
                logger.severe("효과 지속시간 파싱 오류: " + parts[3]);
            }
        }
    }

    private void handleGameOver(String[] parts) {
        if (parts.length >= 5) {
            String roomId = parts[1];
            String winnerName = parts[2];
            try {
                int myScore = Integer.parseInt(parts[3]);
                int opponentScore = Integer.parseInt(parts[4]);
                boolean isForfeit = parts.length >= 6 && "FORFEIT".equals(parts[5]);

                gameClient.handleEvent(ClientEvent.GAME_OVER, winnerName, myScore, opponentScore, isForfeit);
            } catch (NumberFormatException e) {
                logger.severe("점수 파싱 오류: " + Arrays.toString(parts));
            }
        }
    }

    private void handleLeaderboardData(String[] parts) {
        if (parts.length >= 3) {
            String type = parts[1];
            String[] entries = Arrays.copyOfRange(parts, 2, parts.length);
            switch (type) {
                case "TOP" -> gameClient.handleEvent(ClientEvent.TOP_SCORES, (Object[]) entries);
                case "USER" -> gameClient.handleEvent(ClientEvent.USER_RECORDS, (Object[]) entries);
                default -> logger.warning("알 수 없는 리더보드 타입: " + type);
            }
        } else {
            String type = parts.length >= 2 ? parts[1] : "UNKNOWN";
            switch (type) {
                case "TOP" -> gameClient.handleEvent(ClientEvent.TOP_SCORES, new String[0]);
                case "USER" -> gameClient.handleEvent(ClientEvent.USER_RECORDS, new String[0]);
                default -> logger.warning("알 수 없는 리더보드 타입: " + type);
            }
        }
    }

    private void handleLeaderboardUpdate(String[] parts) {
        if (parts.length >= 4) {
            String roomId = parts[1];
            String playerName = parts[2];
            try {
                int rank = Integer.parseInt(parts[3]);
                gameClient.handleEvent(ClientEvent.LEADERBOARD_UPDATE, roomId, playerName, rank);
            } catch (NumberFormatException e) {
                logger.severe("리더보드 순위 파싱 오류: " + parts[3]);
            }
        }
    }

    private void handleMyRecordsData(String[] parts) {
        if (parts.length >= 2) {
            String[] records = Arrays.copyOfRange(parts, 1, parts.length);
            gameClient.handleEvent(ClientEvent.USER_RECORDS, (Object) records);
        }
    }

    private void handleChat(String[] parts) {
        if (parts.length >= 3) {
            String username = parts[1];
            String chatMsg = parts[2];
            gameClient.handleEvent(ClientEvent.CHAT_RECEIVED, username, chatMsg);
        }
    }

    private void handleHostLeft(String[] parts) {
        if (parts.length >= 3) {
            String roomId = parts[1];
            String hostMsg = parts[2];
            gameClient.handleEvent(ClientEvent.HOST_LEFT, roomId, hostMsg);
        }
    }

    private void handleRoomClosed(String[] parts) {
        if (parts.length >= 3) {
            String roomId = parts[1];
            String reason = parts[2];
            gameClient.handleEvent(ClientEvent.ROOM_CLOSED, roomId, reason);
        }
    }

    private void handleNewHost(String[] parts) {
        if (parts.length >= 3) {
            String roomId = parts[1];
            String newHostName = parts[2];
            gameClient.handleEvent(ClientEvent.NEW_HOST, roomId, newHostName);
        }
    }

    private void handleError(String[] parts) {
        if (parts.length >= 2) {
            String errorMessage = parts[1];
            gameClient.handleEvent(ClientEvent.ERROR_OCCURRED, errorMessage);
        }
    }
}