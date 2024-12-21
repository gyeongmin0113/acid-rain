/*
 * client.app.ClientMain.java
 * 게임 기본 진입점
 */

package client.app;

import client.ui.dialog.LoginDialog;
import client.ui.MainMenu;
import javax.swing.*;
import java.util.logging.Logger;

public class ClientMain {
    // for Debugging XD -> 디버깅용으로 로거 사용, 오버헤드 등으로 인해 문제가 생긴다면 지우기.
    private static final Logger logger = Logger.getLogger(ClientMain.class.getName());

    public static void main(String[] args) {
        // 시스템 룩앤필 설정
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warning("시스템 룩앤필 설정 실패: " + e.getMessage());
        }

        // GUI 스레드에서 실행
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ACID RAIN");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            LoginDialog loginDialog = new LoginDialog(frame);
            loginDialog.setVisible(true);

            // 로그인 대화상자에서 확인 버튼을 누르면 메인 메뉴를 생성하고 보여줌
            if (loginDialog.isAccepted()) {
                try {
                    GameClient client = new GameClient(
                            loginDialog.getServerAddress(),
                            loginDialog.getPort(),
                            loginDialog.getNickname()
                    );

                    MainMenu mainMenu = new MainMenu(client);
                    client.setEventListener(mainMenu);

                    client.connect();

                    frame.add(mainMenu);
                    frame.setVisible(true);
                } catch (Exception e) {
                    logger.severe("게임 클라이언트 초기화 실패: " + e.getMessage());
                    JOptionPane.showMessageDialog(frame,
                            "서버 연결 실패: " + e.getMessage(),
                            "오류",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            } else {
                System.exit(0);
            }
        });
    }
}