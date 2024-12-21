/*
 * client.app.GameClient.java
 * 게임 클라이언트 클래스
 */

package client.app;

import client.event.GameEvent.*;
import client.event.GameEventListener;
import client.network.MessageHandler;

import game.model.GameRoom;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class GameClient implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(GameClient.class.getName());
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final MessageHandler messageHandler;
    private GameEventListener eventListener;
    private final String username;
    private final ExecutorService executorService;
    private volatile boolean isRunning;
    private final String host;
    private final int port;
    private GameRoom currentRoom;

    public GameClient(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.messageHandler = new MessageHandler(this);
        this.executorService = Executors.newSingleThreadExecutor();
        this.isRunning = false;
    }

    /**
     * 서버와의 연결을 초기화하고 메시지 수신을 시작
     * @throws IOException 서버 연결 실패 시 예외 발생
     */
    public void connect() throws IOException {
        if (isRunning) {
            logger.warning("이미 연결되어 있습니다.");
            return;
        }

        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            isRunning = true;
            sendMessage(ClientCommand.LOGIN + "|" + username);
            sendMessage(ClientCommand.ROOM_LIST);
            startMessageReceiver();

            logger.info("서버에 연결되었습니다: " + host + ":" + port);
        } catch (IOException e) {
            cleanup();
            throw new IOException("서버 연결 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 메시지 수신 작업을 시작
     * 별도의 스레드에서 서버로부터 지속적으로 메시지를 읽어 처리
     */
    private void startMessageReceiver() {
        executorService.submit(() -> {
            try {
                String message;
                while (isRunning && (message = reader.readLine()) != null) {
                    messageHandler.handleMessage(message);
                }
            } catch (IOException e) {
                if (isRunning) {
                    logger.severe("메시지 수신 중 오류 발생: " + e.getMessage());
                    handleConnectionLost();
                }
            }
        });
    }

    /**
     * 서버에 지정된 메시지를 전송
     * @param message 서버로 보낼 메시지
     */
    public void sendMessage(String message) {
        if (writer != null && isConnected()) {
            writer.println(message);
            logger.info("메시지 전송: " + message);
        } else {
            logger.warning("메시지 전송 실패 (연결 없음): " + message);
        }
    }
    /**
     * 특정 방의 플레이어 목록 요청.
     *
     * @param roomId 요청할 방 ID
     */
    public void sendPlayerListRequest(String roomId) {
        sendMessage(ClientCommand.ROOM_LIST + "|" + roomId);
    }

    /**
     * 새로운 게임 방을 생성하도록 서버에 요청합니다.
     * @param room 생성할 게임 방의 정보를 포함한 객체
     */
    public void sendCreateRoomRequest(GameRoom room) {
        String message = String.format(ClientCommand.CREATE_ROOM + "|%s|%s|%s|%s|%d",
                room.getRoomName(),
                room.getPassword(),
                room.getGameMode().name(),
                room.getDifficulty().name(),
                room.getMaxPlayers()
        );
        sendMessage(message);
    }

    public void sendJoinRoomRequest(String roomId, String password) {
        StringBuilder message = new StringBuilder(ClientCommand.JOIN_ROOM + "|" + roomId);
        if (password != null && !password.isEmpty()) {
            message.append("|").append(password);
        }
        sendMessage(message.toString());
    }

    public void sendLeaveRoomRequest(String roomId) {
        if (!isConnected()) {
            handleConnectionLost();
            return;
        }

        sendMessage(ClientCommand.LEAVE_ROOM + "|" + roomId);
        currentRoom = null;

        if (eventListener != null) {
            eventListener.onGameEvent(ClientEvent.ROOM_CLOSED, roomId, "방이 닫혔습니다.");
            sendMessage(ClientCommand.ROOM_LIST);
        }
    }

    public void sendRoomChatMessage(String roomId, String message) {
        sendMessage(ClientCommand.CHAT + "|" + roomId + "|" + message);
    }

    public void sendGameStartRequest(String roomId) {
        sendMessage(ClientCommand.START_GAME + "|" + roomId);
    }

    // In-Game 관련 메서드
    public void sendGameAction(String roomId, String action, String... params) {
        StringBuilder message = new StringBuilder(ClientCommand.GAME_ACTION + "|" + roomId + "|" + action);
        for (String param : params) {
            message.append("|").append(param);
        }
        sendMessage(message.toString());
    }
/*
 * Deprecated xD
 */
//    public void sendGameEndRequest(String roomId, String winnerName, int myScore, int opponentScore) {
//        sendMessage(String.format("GAME_END|%s|%s|%d|%d",
//                roomId, winnerName, myScore, opponentScore));
//    }
//
//    public void sendUpdateSettingsRequest(String roomId, String settingType, String value) {
//        sendMessage("UPDATE_SETTINGS|" + roomId + "|" + settingType + "|" + value);
//    }
//
//    // 리더보드(랭킹) 관련 메서드
//    public void requestLeaderboardUpdate(String username, int score, GameMode mode, DifficultyLevel difficulty) {
//        sendMessage(String.format("LEADERBOARD_UPDATE|%s|%d|%s|%s",
//                username, score, mode.name(), difficulty.name()));
//    }
//
//    public void requestTopScores(GameMode mode, DifficultyLevel difficulty) {
//        sendMessage("LEADERBOARD_ACTION|GET_TOP|" + mode.name() + "|" + difficulty.name());
//    }
//
//    public void requestUserRecords(GameMode mode, DifficultyLevel difficulty) {
//        sendMessage("LEADERBOARD_ACTION|GET_MY_RECORDS|" + mode.name() + "|" + difficulty.name());
//    }

    // 이벤트 처리 메서드
    public void handleEvent(String eventType, Object... data) {
        if (eventListener != null) {
            try {
                eventListener.onGameEvent(eventType, data);
            } catch (Exception e) {
                logger.severe("이벤트 처리 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /*
     * 서버와의 연결이 끊어졌을 때 호출
     * 클라이언트의 상태를 초기화하고 이벤트를 발생
     */
    private void handleConnectionLost() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        logger.warning("서버와의 연결이 끊어졌습니다.");
        handleEvent("CONNECTION_LOST");
        cleanup();
    }

    // 리소스 정리 메서드
    private void cleanup() {
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
            if (writer != null) {
                writer.close();
                writer = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            logger.severe("리소스 정리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (!executorService.isShutdown()) {
                executorService.shutdownNow();
            }
            eventListener = null;
            currentRoom = null;
        }
    }

    // 연결 종료 메서드
    public void disconnect() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        try {
            if (isConnected()) {
                sendMessage(ClientCommand.LOGOUT);
                Thread.sleep(100); // 메시지 전송 대기
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("로그아웃 메시지 전송 중 인터럽트 발생");
        } finally {
            cleanup();
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    // Getter/Setter 메서드
    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
        logger.info("이벤트 리스너 설정됨: " + (listener != null ? listener.getClass().getSimpleName() : "null"));
    }

    public GameEventListener getEventListener() {
        return eventListener;
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && isRunning;
    }

    public GameRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(GameRoom room) {
        this.currentRoom = room;
    }
}