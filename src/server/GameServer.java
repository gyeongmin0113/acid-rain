package server;

import game.model.DifficultyLevel;
import game.model.GameMode;
import game.model.GameRoom;
import client.event.GameEvent.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import game.model.LeaderboardEntry;
import server.game.LeaderboardManager;
import server.game.ServerGameController;

public class GameServer {
    private static final Logger logger = Logger.getLogger(GameServer.class.getName());
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, Set<ClientHandler>> roomPlayers = new ConcurrentHashMap<>();
    private int roomIdCounter = 1;
    private final Map<String, ServerGameController> controllers = new ConcurrentHashMap<>();

    public GameServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            logger.info("서버가 포트 " + port + "에서 시작되었습니다.");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleNewConnection(clientSocket);
                } catch (IOException e) {
                    if (running) {
                        logger.severe("클라이언트 연결 수락 중 오류: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.severe("서버 시작 실패: " + e.getMessage());
            throw new RuntimeException("서버 시작 실패", e);
        } finally {
            shutdown();
        }
    }

    private void handleNewConnection(Socket clientSocket) {
        try {
            ClientHandler clientHandler = new ClientHandler(clientSocket, this);
            if (clientHandler.isRunning()) {
                clients.add(clientHandler);
                new Thread(clientHandler).start();
                logger.info("새로운 클라이언트 연결: " + clientSocket.getInetAddress());
                broadcastUserCount();
            } else {
                logger.severe("클라이언트 핸들러 초기화 실패");
                clientSocket.close();
            }
        } catch (IOException e) {
            logger.severe("소켓 처리 중 오류: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                logger.severe("소켓 닫기 실패: " + ex.getMessage());
            }
        }
    }

    public void sendPlayerList(String roomId, ClientHandler requester) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            String playerList = String.join(";", room.getPlayers());
            broadcastToRoom(roomId,  ServerMessage.PLAYER_UPDATE + "|" + roomId + "|" +
                    room.getCurrentPlayers() + "|" + playerList);
            logger.info("플레이어 목록 전송: " + roomId + " - " + playerList);
        }
    }

    public synchronized void createRoom(String[] roomInfo, ClientHandler creator) {
        if (roomInfo.length < 6) {
            creator.sendMessage(ServerMessage.CREATE_ROOM_RESPONSE + "|false|잘못된 요청 형식입니다.");
            return;
        }

        String roomName = roomInfo[1];
        String password = roomInfo[2];
        GameMode gameMode = GameMode.fromDisplayName(roomInfo[3]);
        DifficultyLevel difficulty = DifficultyLevel.fromDisplayName(roomInfo[4]);
        int maxPlayers = Integer.parseInt(roomInfo[5]);

        if (roomName.isEmpty() || maxPlayers < 2 || maxPlayers > 4) {
            creator.sendMessage(ServerMessage.CREATE_ROOM_RESPONSE + "|false|잘못된 설정값입니다.");
            return;
        }

        String roomId = "R" + roomIdCounter++;
        GameRoom room = new GameRoom(roomName, password, gameMode, difficulty, maxPlayers);
        room.setRoomId(roomId);
        room.setHostName(creator.getUsername());
        room.addPlayer(creator.getUsername());

        rooms.put(roomId, room);
        Set<ClientHandler> players = Collections.synchronizedSet(new HashSet<>());
        players.add(creator);
        roomPlayers.put(roomId, players);

        String roomInfoStr = formatRoomInfo(room);
        creator.sendMessage(ServerMessage.CREATE_ROOM_RESPONSE + "|true|방이 생성되었습니다.|" + roomInfoStr + "|" + roomId);
        creator.setCurrentRoomId(roomId);

        String playerList = String.join(";", room.getPlayers());
        broadcastToRoom(roomId, ServerMessage.PLAYER_UPDATE + "|" + roomId + "|" + room.getCurrentPlayers() + "|" + playerList);

        broadcastRoomList();
        logger.info("방 생성 완료: " + roomId + ", 방장: " + creator.getUsername());
    }
    /**
     * 클라이언트를 특정 방에 입장.
     *
     * @param client 입장할 클라이언트
     * @param roomId 입장할 방 ID
     * @param password 방 비밀번호
     * @return 입장 성공 여부
     */
    public synchronized void joinRoom(String roomId, ClientHandler client, String password) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            client.sendMessage(ServerMessage.JOIN_ROOM_RESPONSE + "|false|존재하지 않는 방입니다.");
            return;
        }

        if (room.isPasswordRequired() && !room.isPasswordValid(password)) {
            client.sendMessage(ServerMessage.JOIN_ROOM_RESPONSE + "|false|비밀번호가 일치하지 않습니다.");
            return;
        }

        if (room.isFull()) {
            client.sendMessage(ServerMessage.JOIN_ROOM_RESPONSE + "|false|방이 가득 찼습니다.");
            return;
        }

        if (room.isInGame()) {
            client.sendMessage(ServerMessage.JOIN_ROOM_RESPONSE + "|false|이미 게임이 시작된 방입니다.");
            return;
        }

        Set<ClientHandler> players = roomPlayers.get(roomId);
        players.add(client);
        room.addPlayer(client.getUsername());
        client.setCurrentRoomId(roomId);

        String roomInfoStr = formatRoomInfo(room);
        client.sendMessage(ServerMessage.JOIN_ROOM_RESPONSE + "|true|방에 입장했습니다.|" + roomInfoStr);

        String playerList = String.join(";", room.getPlayers());
        broadcastToRoom(roomId,  ServerMessage.PLAYER_UPDATE + "|" + roomId + "|" + room.getCurrentPlayers() + "|" + playerList);

        broadcastRoomList();
        logger.info(client.getUsername() + "님이 " + roomId + " 방에 입장했습니다.");
    }

    public synchronized void leaveRoom(String roomId, ClientHandler client) {
        Set<ClientHandler> players = roomPlayers.get(roomId);
        GameRoom room = rooms.get(roomId);

        if (room == null || players == null) {
            return;
        }

        boolean isHost = client.getUsername().equals(room.getHostName());

        players.remove(client);
        room.removePlayer(client.getUsername());
        client.setCurrentRoomId(null);

        if (players.isEmpty()) {
            rooms.remove(roomId);
            roomPlayers.remove(roomId);
            controllers.remove(roomId);
            broadcast(ServerMessage.ROOM_CLOSED + "|" + roomId + "|방이 닫혔습니다.");
        } else if (isHost) {
            ClientHandler newHost = players.iterator().next();
            room.setHostName(newHost.getUsername());

            broadcastToRoom(roomId, ServerMessage.HOST_LEFT + "|" + roomId + "|이전 방장이 퇴장했습니다.");
            broadcastToRoom(roomId, ServerMessage.NEW_HOST + "|" + roomId + "|" + newHost.getUsername());

            String playerList = String.join(";", room.getPlayers());
            broadcastToRoom(roomId, ServerMessage.PLAYER_UPDATE + "|" + roomId + "|" + room.getCurrentPlayers() + "|" + playerList);
        } else {
            String playerList = String.join(";", room.getPlayers());
            broadcastToRoom(roomId, ServerMessage.PLAYER_UPDATE + "|" + roomId + "|" + room.getCurrentPlayers() + "|" + playerList);
        }

        broadcastRoomList();
        logger.info(client.getUsername() + "님이 " + roomId + " 방에서 퇴장했습니다.");
    }

    public void handleChat(String roomId, ClientHandler sender, String message) {
        if (rooms.containsKey(roomId)) {
            broadcastToRoom(roomId, ServerMessage.CHAT + "|" + sender.getUsername() + "|" + message);
        }
    }

    public void updateGameSettings(String roomId, String settingType, String newValue, ClientHandler updater) {
        GameRoom room = rooms.get(roomId);
        if (room == null || !updater.getUsername().equals(room.getHostName())) {
            return;
        }

        try {
            switch (settingType) {
                case "MODE" -> room.setGameMode(GameMode.fromDisplayName(newValue));
                case "DIFFICULTY" -> room.setDifficulty(DifficultyLevel.fromDisplayName(newValue));
                default -> {
                    logger.warning("알 수 없는 설정 타입: " + settingType);
                    return;
                }
            }

            broadcastToRoom(roomId, ServerMessage.SETTINGS_UPDATE + "|" + roomId + "|" +
                    room.getGameMode().name() + "|" +
                    room.getDifficulty().name());
            broadcastRoomList();
        } catch (IllegalArgumentException e) {
            updater.sendMessage(ServerMessage.ERROR + "|잘못된 설정값입니다: " + e.getMessage());
        }
    }

    public void startGame(String roomId, ClientHandler starter) {
        GameRoom room = rooms.get(roomId);
        if (room == null || !starter.getUsername().equals(room.getHostName())) {
            starter.sendMessage(ServerMessage.ERROR + "|게임을 시작할 권한이 없습니다.");
            return;
        }

        if (!room.canStart()) {
            starter.sendMessage(ServerMessage.ERROR + "|아직 게임을 시작할 수 없습니다.");
            return;
        }

        try {
            room.setGameStarted(true);
            room.setInGame(true);

            ServerGameController controller = new ServerGameController(this, room);
            controllers.put(roomId, controller);

            // 게임 시작 알림
            broadcastToRoom(roomId, String.format("GAME_CONFIG|%s|%s|%s",
                    room.getGameMode().name(),
                    room.getDifficulty().name(),
                    String.join(";", room.getPlayers())
            ));
            broadcastToRoom(roomId, ServerMessage.GAME_START);

            controller.startGame();
            broadcastRoomList();
            logger.info("게임 시작됨: 방 " + roomId);
        } catch (Exception e) {
            logger.severe("게임 시작 중 오류 발생: " + e.getMessage());
            room.setGameStarted(false);
            room.setInGame(false);
            broadcastToRoom(roomId, ServerMessage.ERROR + "|게임 시작 실패: " + e.getMessage());
        }
    }

    /*
     * GAME_ACTION 메시지를 처리하는 메서드.
     */
    public void handleGameAction(String roomId, ClientHandler player, String action, String... params) {
        GameRoom room = rooms.get(roomId);
        if (room == null || !room.isInGame()) {
            logger.warning("유효하지 않은 게임 액션 시도 - 룸: " + roomId + ", 액션: " + action);
            player.sendMessage(ServerMessage.ERROR + "|유효하지 않은 게임 액션입니다.");
            return;
        }

        ServerGameController controller = controllers.get(roomId);
        if (controller == null) {
            logger.warning("게임 컨트롤러를 찾을 수 없음 - 룸: " + roomId);
            player.sendMessage(ServerMessage.ERROR + "|게임 컨트롤러를 찾을 수 없습니다.");
            return;
        }

        try {
            switch (action) {
                case ClientCommand.WORD_INPUT -> {
                    if (params.length > 0) {
                        controller.handlePlayerInput(player, params[0]);
                    } else {
                        logger.warning("단어 입력 없음 - 플레이어: " + player.getUsername());
                        player.sendMessage(ServerMessage.ERROR + "|단어가 입력되지 않았습니다.");
                    }
                }
                case ServerMessage.WORD_MISSED -> {
                    if (params.length > 0) {
                        controller.handleWordMissed(params[0], player);
                    } else {
                        logger.warning("놓친 단어 정보 없음 - 플레이어: " + player.getUsername());
                        player.sendMessage(ServerMessage.ERROR + "|놓친 단어 정보가 없습니다.");
                    }
                }
                case "PLAYER_LEAVE_GAME" -> {
                    logger.info("플레이어 게임 퇴장 - 플레이어: " + player.getUsername() + ", 룸: " + roomId);
                    controller.handlePlayerLeaveGame(player);
                }
                default -> {
                    logger.warning("알 수 없는 게임 액션: " + action + " - 플레이어: " + player.getUsername());
                    player.sendMessage(ServerMessage.ERROR + "|알 수 없는 게임 액션입니다.");
                }
            }
        } catch (Exception e) {
            logger.severe("게임 액션 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(ServerMessage.ERROR + "|게임 액션 처리 중 오류가 발생했습니다.");
        }
    }

    public void handleLeaderboardAction(ClientHandler player, String... params) {
        if (params.length >= 3) {
            String leaderboardAction = params[0];
            String modeStr = params[1];
            String diffStr = params[2];
            LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();

            try {
                GameMode mode = GameMode.valueOf(modeStr.toUpperCase());
                DifficultyLevel difficulty = DifficultyLevel.valueOf(diffStr.toUpperCase());

                switch (leaderboardAction) {
                    case "GET_TOP" -> {
                        List<LeaderboardEntry> topEntries = leaderboardManager.getTopEntries(mode, difficulty);
                        StringBuilder response = new StringBuilder(ServerMessage.LEADERBOARD_DATA + "|TOP");
                        for (LeaderboardEntry entry : topEntries) {
                            response.append("|").append(entry.toFileString());
                        }
                        player.sendMessage(response.toString());
                        logger.info("상위 기록 전송 - 모드: " + mode + ", 난이도: " + difficulty);
                    }
                    case "GET_MY_RECORDS" -> {
                        List<LeaderboardEntry> userEntries = leaderboardManager.getUserEntries(player.getUsername());
                        StringBuilder response = new StringBuilder(ServerMessage.LEADERBOARD_DATA + "|USER");
                        for (LeaderboardEntry entry : userEntries) {
                            response.append("|").append(entry.toFileString());
                        }
                        player.sendMessage(response.toString());
                        logger.info("사용자 기록 전송 - 사용자: " + player.getUsername());
                    }
                    default -> {
                        logger.warning("알 수 없는 리더보드 액션: " + leaderboardAction);
                        player.sendMessage(ServerMessage.ERROR + "|알 수 없는 리더보드 액션입니다.");
                    }
                }
            } catch (IllegalArgumentException e) {
                logger.warning("잘못된 게임 모드 또는 난이도: " + e.getMessage());
                player.sendMessage(ServerMessage.ERROR + "|잘못된 게임 모드 또는 난이도입니다.");
            } catch (Exception e) {
                logger.severe("리더보드 처리 중 오류: " + e.getMessage());
                player.sendMessage(ServerMessage.ERROR + "|리더보드 처리 중 오류가 발생했습니다.");
            }
        } else {
            logger.warning("잘못된 리더보드 요청 형식");
            player.sendMessage(ServerMessage.ERROR + "|리더보드 요청 형식이 잘못되었습니다.");
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        String roomId = client.getCurrentRoomId();
        if (roomId != null) {
            leaveRoom(roomId, client);
        }
        broadcastUserCount();
    }

    public void broadcastToRoom(String roomId, String message) {
        Set<ClientHandler> players = roomPlayers.get(roomId);
        if (players != null) {
            synchronized (players) {
                for (ClientHandler player : players) {
                    player.sendMessage(message);
                }
            }
        }
    }

    public void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    public void broadcastRoomList() {
        StringBuilder response = new StringBuilder("ROOM_LIST_RESPONSE");
        for (GameRoom room : rooms.values()) {
            response.append("|").append(formatRoomInfo(room));
        }
        broadcast(response.toString());
    }

    public void broadcastUserCount() {
        broadcast(ServerMessage.USERS + "|" + clients.size());
    }

    private String formatRoomInfo(GameRoom room) {
        return String.format("%s,%s,%d,%d,%s,%s,%s",
                room.getRoomId(),
                room.getRoomName(),
                room.getCurrentPlayers(),
                room.getMaxPlayers(),
                room.getGameMode().getDisplayName(),
                room.getDifficulty().getDisplayName(),
                room.getHostName());
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.severe("서버 소켓 종료 중 오류: " + e.getMessage());
        }

        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.shutdown();
            }
            clients.clear();
        }

        rooms.clear();
        roomPlayers.clear();
        controllers.clear();

        logger.info("서버가 종료되었습니다.");
    }

    public Map<String, GameRoom> getRooms() {
        return rooms;
    }

    public Map<String, Set<ClientHandler>> getRoomPlayers() {
        return roomPlayers;
    }
}
