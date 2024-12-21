/*
 * client.network.NetworkManager.java
 * 네트워크 연결을 관리하는 클래스, 서버와의 연결을 담당, 가장 중요한 클래스! 소켓 통신의 꽃, 다만 사용하진 않았음
 * 추후, 확장시 네트워크 관련 매니저를 이용해 사용하는 것이 좋을 것 같아 작성함.
 */

package client.network;

import java.io.*;
import java.net.Socket;

public class NetworkManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final MessageHandler messageHandler;
    private boolean connected = false;

    public NetworkManager(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public synchronized void connect(String host, int port) {
        if (connected) {
            System.out.println("이미 연결된 상태입니다: " + host + ":" + port);
            return; // 이미 연결되어 있으면 재연결 방지
        }

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            startReceiving();
        } catch (IOException e) {
            throw new RuntimeException("서버 연결 실패: " + e.getMessage());
        }
    }

    private void startReceiving() {
        new Thread(() -> {
            try {
                String message;
                while (connected && (message = in.readLine()) != null) {
                    messageHandler.handleMessage(message);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("서버와의 연결이 끊어졌습니다: " + e.getMessage());
                    disconnect();
                }
            }
        }).start();
    }

    public synchronized void sendMessage(String message) {
        if (!connected || out == null) {
            System.err.println("메시지를 보낼 수 없습니다. 서버와 연결되지 않았습니다.");
            return;
        }
        out.println(message);
    }

    public synchronized void disconnect() {
        if (!connected) return; // 이미 연결이 끊어진 상태라면 무시

        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("소켓 종료 중 에러: " + e.getMessage());
        } finally {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
            } catch (IOException e) {
                System.err.println("스트림 종료 중 에러: " + e.getMessage());
            }
        }
    }
}
