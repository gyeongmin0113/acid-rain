/*
 * client.ui.MainMenu.java
 * 메인 메뉴 화면을 구성하는 클래스
 * 자바의 그래픽 기능을 활용하여 메인 메뉴를 구성하고, 이벤트를 처리
 * References: https://stackoverflow.com/questions/8342887/how-do-you-move-an-object-in-a-wavy-pattern
 */

package client.ui;

import client.app.GameClient;
import client.event.GameEvent;
import client.event.GameEvent.ClientCommand;
import client.event.GameEventListener;
import client.ui.dialog.LeaderboardDialog;
import client.ui.dialog.RoomListDialog;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import client.ui.theme.StyleManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainMenu extends JPanel implements GameEventListener {
    // 기본 컴포넌트 및 상태 변수
    private final GameClient client;
    private final Timer waveAnimationTimer;
    private final Timer rainAnimationTimer;
    private double waveOffset = 3.0;
    private int connectedUsers = 0;

    // 비 효과를 위한 변수들
    private List<Raindrop> raindrops;
    private Random random;

    private MainContentPanel contentPanel;
    private JPanel topMenuBar;
    private JPanel statusBar;
    private JLabel connectedUsersLabel;

    public MainMenu(GameClient client) {
        this.client = client;
        this.raindrops = new ArrayList<>();
        this.random = new Random();

        // 파도와 비 애니메이션을 위한 타이머 설정
        this.waveAnimationTimer = new Timer(50, e -> {
            waveOffset += 0.15; // 파도 움직임 속도
            contentPanel.repaint();
        });

        this.rainAnimationTimer = new Timer(50, e -> {
            updateRain();
            contentPanel.repaint();
        });

        initializeUI();
        setupKeyboardShortcuts();

        // 애니메이션 타이머 시작
        waveAnimationTimer.start();
        rainAnimationTimer.start();

        // 초기 빗방울 생성
        for (int i = 0; i < 100; i++) {
            addNewRaindrop();
        }

        requestUserCountUpdate();
    }

    /*
     * 빗방울을 표현하는 내부 클래스 -> 단순히 산성비를 표현하기 위한 디자인 요소로 렌더링을 위한 오버헤드가 꽤나 크기 때문에 제거하는게 좋음
     * 다만, 작성하는 날 비가 주륵주륵 내리기에, 감성에 젖어 작성함
     * Reference: https://youtu.be/dD9CwuvsBXc
     */
    private class Raindrop {
        double x, y;          // 빗방울의 위치
        double speed;         // 떨어지는 속도
        double length;        // 빗방울의 길이

        Raindrop() {
            reset();
        }

        // 빗방울 초기화 또는 재설정
        void reset() {
            x = random.nextDouble() * getWidth();
            y = random.nextDouble() * -100; // 화면 위에서 시작
            speed = 5 + random.nextDouble() * 10; // 다양한 속도
            length = 10 + random.nextDouble() * 20; // 다양한 길이
        }

        // 빗방울 위치 업데이트
        void update() {
            y += speed;
            if (y > getHeight()) {
                reset();
            }
        }
    }

    // 모든 빗방울 업데이트
    private void updateRain() {
        for (Raindrop drop : raindrops) {
            drop.update();
        }
    }

    private void addNewRaindrop() {
        raindrops.add(new Raindrop());
    }

    // Raindrop 관련 클래스와 메서드, 끝


    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.BACKGROUND);

        createTopMenuBar();
        createMainContent();
        createStatusBar();
    }

    private void requestUserCountUpdate() {
        client.sendMessage(ClientCommand.USERS_REQUEST);
    }

    private void createTopMenuBar() {
        topMenuBar = new JPanel(new BorderLayout());
        topMenuBar.setBackground(ColorScheme.SECONDARY);

        JPanel leftMenu = createMenuSection(FlowLayout.LEFT,
                createMenuLabel("배경색 설정", this::showBackgroundColorDialog)
        );

        JPanel centerMenu = createMenuSection(FlowLayout.CENTER,
                createMenuLabel(client.getUsername() + "님이 입장하셨습니다.", null)
        );

        JPanel rightMenu = createMenuSection(FlowLayout.RIGHT,
                createMenuLabel("랭킹", this::showRanking),
                createMenuLabel("종료", () -> System.exit(0))
        );

        topMenuBar.add(leftMenu, BorderLayout.WEST);
        topMenuBar.add(centerMenu, BorderLayout.CENTER);
        topMenuBar.add(rightMenu, BorderLayout.EAST);

        add(topMenuBar, BorderLayout.NORTH);
    }

    private JPanel createMenuSection(int alignment, JLabel... labels) {
        JPanel panel = new JPanel(new FlowLayout(alignment, 15, 10));
        panel.setOpaque(false);
        for (JLabel label : labels) {
            panel.add(label);
        }
        return panel;
    }

    private JLabel createMenuLabel(String text, Runnable action) {
        JLabel label = new JLabel(text);
        StyleManager.applyLabelStyle(label);

        if (action != null) {
            label.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // 마우스 이벤트 처리
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    label.setForeground(ColorScheme.PRIMARY);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    label.setForeground(ColorScheme.TEXT);
                }
            });
        }
        return label;
    }

    private void createMainContent() {
        contentPanel = new MainContentPanel();
        add(contentPanel, BorderLayout.CENTER);
    }

    private void createStatusBar() {
        statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(ColorScheme.SECONDARY);

        connectedUsersLabel = new JLabel("현재 접속자 수: " + connectedUsers);
        StyleManager.applyLabelStyle(connectedUsersLabel);

        JLabel startGuide = new JLabel("게임을 시작하려면 Ctrl + S를 눌러주세요");
        StyleManager.applyLabelStyle(startGuide);

        statusBar.add(startGuide, BorderLayout.WEST);
        statusBar.add(connectedUsersLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    // 키보드 단축키 설정
    private void setupKeyboardShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        KeyStroke startKey = KeyStroke.getKeyStroke(KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK);
        inputMap.put(startKey, "StartGame");
        actionMap.put("StartGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });
    }

    private void startGame() {
        setVisible(false);
        JFrame currentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        RoomListDialog dialog = new RoomListDialog(currentFrame, client);
        dialog.setVisible(true);
    }

    private void showBackgroundColorDialog() {
        Color newColor = JColorChooser.showDialog(this, "배경색 선택",
                getBackground());
        if (newColor != null) {
            setBackground(newColor);
            contentPanel.setBackground(newColor);
            repaint();
        }
    }

    private void showRanking() {
        JFrame currentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        LeaderboardDialog leaderboardDialog = new LeaderboardDialog(currentFrame, client);

        GameEventListener currentListener = this;

        leaderboardDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                client.setEventListener(currentListener);
            }
        });

        leaderboardDialog.setVisible(true);
    }

    @Override
    public void onGameEvent(String eventType, Object... data) {
        if (eventType.equals(GameEvent.ClientEvent.USERS_UPDATED) && data.length > 0) {
            int newConnectedUsers = (int) data[0];
            SwingUtilities.invokeLater(() -> {
                connectedUsers = newConnectedUsers;
                connectedUsersLabel.setText("현재 접속자 수: " + connectedUsers);
            });
        }
    }

    // 메인 컨텐츠 패널 (그래픽 효과 담당)
    private class MainContentPanel extends JPanel {
        public MainContentPanel() {
            setBackground(ColorScheme.BACKGROUND);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // 배경
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 메인 텍스트
            drawMainContent(g2d);

            // 웨이브 효과
            drawWaves(g2d);

            // 비 효과
            drawRain(g2d);
        }

        private void drawMainContent(Graphics2D g2d) {
            g2d.setFont(FontManager.getFont(16f));
            g2d.setColor(ColorScheme.TEXT);

            String[] messages = {
                    "타이핑 실력을 향상시켜보세요!",
                    "내리는 단어를 정확하게 입력하면 점수를 획득합니다.",
                    "특별한 단어를 입력하면 추가 점수를 획득할 수 있습니다.",
                    "",
                    "준비되셨나요?",
                    "Ctrl + S를 눌러 게임을 시작하세요!"
            };

            FontMetrics fm = g2d.getFontMetrics();
            int y = 100;

            for (String message : messages) {
                int x = (getWidth() - fm.stringWidth(message)) / 2;
                g2d.drawString(message, x, y);
                y += 40;
            }
        }

        // 웨이브 효과 그리기
        private void drawWaves(Graphics2D g2d) {
            int height = getHeight();
            int width = getWidth();
            int waveStartY = height - 150;

            // 세 개의 파도 레이어 그리기
            for (int i = 0; i < 3; i++) {
                float alpha = 0.4f - (i * 0.1f);
                g2d.setColor(new Color(
                        ColorScheme.PRIMARY.getRed(),
                        ColorScheme.PRIMARY.getGreen(),
                        ColorScheme.PRIMARY.getBlue(),
                        (int)(alpha * 255)
                ));

                Path2D path = new Path2D.Double();
                path.moveTo(0, height);

                // 사인 함수를 이용한 파도 모양 생성
                for (int x = 0; x <= width; x++) {
                    double y = Math.sin((x + waveOffset + (i * 30)) / 60.0) * 50.0;
                    path.lineTo(x, waveStartY - y - (i * 40));
                }

                path.lineTo(width, height);
                path.closePath();
                g2d.fill(path);
            }
        }

        // 비 효과 그리기
        private void drawRain(Graphics2D g2d) {
            // 반투명한 파란빛의 비 효과
            g2d.setColor(new Color(200, 200, 255, 100));
            for (Raindrop drop : raindrops) {
                g2d.draw(new Line2D.Double(
                        drop.x,
                        drop.y,
                        drop.x - 1, // 약간 기울어진 효과
                        drop.y + drop.length
                ));
            }
        }
    }
}