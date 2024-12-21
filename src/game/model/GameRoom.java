/*
 * game.model.GameRoom.java
 * 게임 방을 나타내기 위한 모델 클래스
 */

package game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class GameRoom {
    private String roomId;
    private String roomName;
    private String password;
    private String hostName;
    private GameMode gameMode;
    private DifficultyLevel difficulty;
    private int maxPlayers;
    private int currentPlayers;
    private boolean inGame;
    private boolean gameStarted;
    private List<String> players;

    // 기본 생성자
    public GameRoom() {
        this.maxPlayers = 2;
        this.currentPlayers = 0;
        this.inGame = false;
        this.gameStarted = false;
        this.players = new ArrayList<>();
    }

    // 전체 필드를 받는 생성자
    public GameRoom(String roomName, String password, GameMode gameMode,
                    DifficultyLevel difficulty, int maxPlayers) {
        this.roomName = roomName;
        this.password = password;
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = 1;
        this.inGame = false;
        this.gameStarted = false;
        this.players = new ArrayList<>();
    }

    // roomString format: "roomId,roomName,currentPlayers,maxPlayers,gameMode,difficulty,hostName,players[,password]"
    public static GameRoom fromString(String roomString) {
        String[] parts = roomString.split(",");
        if (parts.length < 7) return null;

        GameRoom room = new GameRoom();
        room.roomId = parts[0];
        room.roomName = parts[1];
        room.currentPlayers = Integer.parseInt(parts[2]);
        room.maxPlayers = Integer.parseInt(parts[3]);
        room.gameMode = GameMode.fromDisplayName(parts[4]);
        room.difficulty = DifficultyLevel.fromDisplayName(parts[5]);
        room.hostName = parts[6];

        if (parts.length > 7) {
            String[] playerList = parts[7].split(";");
            room.setPlayers(playerList);
        }

        if (parts.length > 8) {
            room.password = parts[8];
        }

        return room;
    }

    @Override
    public String toString() {
        String playerList = String.join(";", players);
        return String.format("%s,%s,%d,%d,%s,%s,%s,%s,%s",
                roomId, roomName, currentPlayers, maxPlayers,
                gameMode.getDisplayName(), difficulty.getDisplayName(),
                hostName, playerList, password);
    }

    // 비밀번호 검증
    public boolean isPasswordValid(String inputPassword) {
        if (password == null || password.isEmpty()) {
            return true;
        }
        return password.equals(inputPassword);
    }

    // 게임 상태 관리
    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
        this.inGame = gameStarted;
    }

    // 상태 체크 메서드들
    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isFull() {
        return currentPlayers >= maxPlayers;
    }

    public boolean isPasswordRequired() {
        return password != null && !password.isEmpty();
    }

    public boolean canStart() {
        return currentPlayers == maxPlayers && !inGame;
    }

    // 플레이어 관리 메서드들
    public String[] getPlayers() {
        return players.toArray(new String[0]);
    }

    public void setPlayers(String[] players) {
        this.players = new ArrayList<>(Arrays.asList(players));
        this.currentPlayers = this.players.size();
    }

    public void addPlayer(String player) {
        if (!players.contains(player)) {
            players.add(player);
            currentPlayers = players.size();
        }
    }

    public void removePlayer(String player) {
        players.remove(player);
        currentPlayers = players.size();
    }

    public boolean hasPlayer(String player) {
        return players.contains(player);
    }

    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }
}