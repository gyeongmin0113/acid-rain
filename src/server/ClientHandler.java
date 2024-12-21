package server;

import client.event.GameEvent.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String currentRoomId;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        try {
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            logger.severe("클라이언트 핸들러 초기화 실패: " + e.getMessage());
            running = false;
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                processMessage(message);
            }
        } catch (SocketException e) {
            if (running) {
                logger.warning("클라이언트와의 연결 종료: " + e.getMessage());
            }
        } catch (IOException e) {
            if (running) {
                logger.severe("클라이언트와의 통신 중 오류: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }
    /**
     * 클라이언트로부터 수신한 메시지를 처리합니다.
     *
     * @param message 수신한 메시지
     */

    private void processMessage(String message) {
        String[] parts = message.split("\\|");
        String messageType = parts[0];

        try {
            switch (messageType) {
                case ClientCommand.LOGIN:
                    handleLogin(parts);
                    break;
                case ClientCommand.CREATE_ROOM:
                    server.createRoom(parts, this);
                    break;
                case ClientCommand.JOIN_ROOM:
                    handleJoinRoom(parts);
                    break;
                case ClientCommand.LEAVE_ROOM:
                    handleLeaveRoom(parts);
                    break;
                case ClientCommand.CHAT:
                    handleChat(parts);
                    break;
                case ClientEvent.SETTINGS_UPDATED:
                    handleSettingsUpdate(parts);
                    break;
                case ClientCommand.START_GAME:
                    handleGameStart(parts);
                    break;
                case ClientCommand.GAME_ACTION:
                    handleGameAction(parts);
                    break;
                case "PING":
                    sendMessage("PONG");
                    break;
                case ClientCommand.ROOM_LIST:
                    server.broadcastRoomList();
                    break;
                case ClientCommand.PLAYER_LIST:
                    handlePlayerList(parts);
                    break;
                case ClientCommand.LOGOUT:
                    handleLogout();
                    break;

                case "LEADERBOARD_ACTION":
                    handleLeaderboardAction(parts);
                    break;

                // must be removed after refactoring
                case ClientCommand.USERS_REQUEST:
                    server.broadcastUserCount();
                    break;

                default:
                    logger.warning("알 수 없는 메시지 타입: " + messageType);
                    sendMessage(ServerMessage.ERROR + "|지원하지 않는 메시지 타입입니다.");
            }
        } catch (Exception e) {
            logger.severe("메시지 처리 중 오류 발생: " + e.getMessage());
            sendMessage(ServerMessage.ERROR + "|메시지 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void handleLogin(String[] parts) {
        if (parts.length >= 2) {
            this.username = parts[1];
            logger.info("로그인: " + username);
            server.broadcastUserCount();
            server.broadcastRoomList();
        } else {
            sendMessage(ServerMessage.ERROR + "|잘못된 로그인 요청입니다.");
        }
    }

    private void handleJoinRoom(String[] parts) {
        if (parts.length >= 2) {
            String roomId = parts[1];
            String password = parts.length >= 3 ? parts[2] : "";
            server.joinRoom(roomId, this, password);
        } else {
            sendMessage(ServerMessage.ERROR + "|잘못된 방 입장 요청입니다.");
        }
    }

    private void handleLeaveRoom(String[] parts) {
        if (currentRoomId != null) {
            server.leaveRoom(currentRoomId, this);
            currentRoomId = null;
        }
    }

    private void handleChat(String[] parts) {
        if (parts.length >= 3 && currentRoomId != null) {
            server.handleChat(currentRoomId, this, parts[2]);
        } else {
            sendMessage(ServerMessage.ERROR + "|잘못된 채팅 메시지입니다.");
        }
    }

    private void handleSettingsUpdate(String[] parts) {
        if (parts.length >= 4 && currentRoomId != null) {
            server.updateGameSettings(currentRoomId, parts[2], parts[3], this);
        } else {
            sendMessage(ServerMessage.ERROR + "|잘못된 설정 업데이트 요청입니다.");
        }
    }

    private void handleGameStart(String[] parts) {
        if (currentRoomId != null) {
            server.startGame(currentRoomId, this);
        } else {
            sendMessage(ServerMessage.ERROR + "|게임을 시작할 수 있는 방이 없습니다.");
        }
    }

    private void handleLeaderboardAction(String[] parts) {
        if (parts.length >= 4) {
            String[] params = new String[parts.length - 1];
            System.arraycopy(parts, 1, params, 0, parts.length - 1);
            server.handleLeaderboardAction(this, params);
        } else {
            sendMessage(ServerMessage.ERROR + "|잘못된 리더보드 액션 요청입니다.");
        }
    }

    private void handleGameAction(String[] parts) {
        if (parts.length >= 3) {
            String roomId = parts[1];
            String action = parts[2];
            String[] params = new String[parts.length - 3];
            System.arraycopy(parts, 3, params, 0, parts.length - 3);
            server.handleGameAction(roomId, this, action, params);
        } else {
            sendMessage(ServerMessage.ERROR + "|잘못된 게임 액션 요청입니다.");
        }
    }

    private void handlePlayerList(String[] parts) {
        if (parts.length >= 2) {
            String roomId = parts[1];
            server.sendPlayerList(roomId, this);
        } else {
            sendMessage(ServerMessage.ERROR + "|잘못된 플레이어 목록 요청입니다.");
        }
    }

    private void handleLogout() {
        logger.info("로그아웃: " + username);
        running = false;
    }
    /**
     * 클라이언트에게 메시지를 전송합니다.
     *
     * @param message 전송할 메시지
     */
    public void sendMessage(String message) {
        if (out != null && !socket.isClosed() && running) {
            out.println(message);
            if (out.checkError()) {
                logger.warning("메시지 전송 실패: " + message);
                running = false;
            }
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.severe("리소스 정리 중 오류 발생: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            if (currentRoomId != null) {
                server.leaveRoom(currentRoomId, this);
            }
            server.removeClient(this);
        } finally {
            shutdown();
        }
    }

    public String getUsername() {
        return username;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(String roomId) {
        this.currentRoomId = roomId;
    }

    public boolean isRunning() {
        return running;
    }
}