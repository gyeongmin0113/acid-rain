/*
 * client.ui.game.GameScreen.java
 * 게임 화면을 정의하는 클래스, 단어를 입력하고 점수를 획득하는 게임 화면, 제일 알짜 클래스..
 */

package client.ui.game;

import client.app.GameClient;
import client.event.GameEvent.*;
import client.event.GameEventListener;
import client.ui.MainMenu;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import client.ui.components.GameTextField;
import game.model.Word;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GameScreen extends JFrame implements GameEventListener {
    private static final Logger logger = Logger.getLogger(GameScreen.class.getName());
    private final GameClient client;
    private final String roomId;
    private final JFrame mainFrame;

    private JPanel gamePanel;
    private GameTextField inputField;
    private JLabel scoreLabel;
    private JLabel phLabel;
    private JProgressBar phMeter;
    private JLabel opponentScoreLabel;
    private Timer screenRefreshTimer;

    private List<Word> activeWords = new ArrayList<>();
    private boolean isBlinded = false;
    private long blindEndTime = 0;
    private int myScore = 0;
    private int opponentScore = 0;
    private double myPH = 7.0;
    private final String myName;
    private final String opponentName;
    private volatile boolean isClosing = false;

    public GameScreen(GameClient client, String roomId, String myName, String opponentName) {
        this.client = client;
        this.roomId = roomId;
        this.myName = myName;
        this.opponentName = opponentName;
        this.mainFrame = (JFrame) SwingUtilities.getWindowAncestor(this);

        client.setEventListener(this);
        initializeFrame();
        setupUI();
        setupInput();
        setupTimers();
        setVisible(true);
    }

    private void initializeFrame() {
        setTitle("Typing Game - " + myName + " vs " + opponentName);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleGameEnd();
            }
        });
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        add(createInfoPanel(), BorderLayout.NORTH);
        add(createGamePanel(), BorderLayout.CENTER);
        add(createInputPanel(), BorderLayout.SOUTH);
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.SECONDARY);
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        phLabel = new JLabel("pH: 7.0");
        phLabel.setFont(FontManager.getFont(16f));
        phLabel.setForeground(ColorScheme.TEXT);

        phMeter = new JProgressBar(0, 70);
        phMeter.setValue(70);
        phMeter.setPreferredSize(new Dimension(150, 20));
        phMeter.setForeground(ColorScheme.PRIMARY);
        phMeter.setBackground(ColorScheme.BACKGROUND);

        leftPanel.add(phLabel);
        leftPanel.add(phMeter);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.setOpaque(false);
        scoreLabel = new JLabel("점수: 0");
        scoreLabel.setFont(FontManager.getFont(20f));
        scoreLabel.setForeground(ColorScheme.TEXT);
        centerPanel.add(scoreLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        opponentScoreLabel = new JLabel("상대방 점수: 0");
        opponentScoreLabel.setFont(FontManager.getFont(16f));
        opponentScoreLabel.setForeground(ColorScheme.TEXT);
        rightPanel.add(opponentScoreLabel);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createGamePanel() {
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGame(g);
            }
        };
        gamePanel.setBackground(ColorScheme.BACKGROUND);
        return gamePanel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(ColorScheme.SECONDARY);
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));

        inputField = new GameTextField();
        inputField.setFont(FontManager.getFont(16f));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        JButton exitButton = new JButton("게임 종료 (ESC)");
        styleButton(exitButton);
        exitButton.addActionListener(e -> handleGameEnd());

        buttonPanel.add(exitButton);
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private void styleButton(JButton button) {
        button.setFont(FontManager.getFont(14f));
        button.setForeground(ColorScheme.TEXT);
        button.setBackground(ColorScheme.PRIMARY);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setFocusPainted(false);
    }

    private void setupInput() {
        inputField.addActionListener(e -> {
            String input = inputField.getText().trim();
            if (!input.isEmpty()) {
                client.sendGameAction(roomId, ClientCommand.WORD_INPUT, input);
                inputField.setText("");
            }
        });

        getRootPane().registerKeyboardAction(
                e -> handleGameEnd(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void setupTimers() {
        screenRefreshTimer = new Timer(1000/60, e -> refreshScreen());
        screenRefreshTimer.start();
    }

    private void drawGame(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setFont(FontManager.getFont(16f));

        synchronized(activeWords) {
            for (Word word : activeWords) {
                if (word.hasSpecialEffect()) {
                    g2d.setFont(FontManager.getEmojiFont(16f));
                    if (word.getEffect() == Word.SpecialEffect.SCORE_BOOST) {
                        g2d.setColor(new Color(255, 215, 0));
                        g2d.drawString("⚡", word.getX() - 25, word.getY());
                    } else {
                        g2d.setColor(new Color(147, 112, 219));
                        g2d.drawString("⭐", word.getX() - 25, word.getY());  // 🌟 대신 ⭐ 사용
                    }
                    g2d.setFont(FontManager.getFont(16f));
                }

                g2d.setColor(Color.WHITE);
                g2d.drawString(word.getText(), word.getX(), word.getY());
            }
        }

        // 그 위에 블라인드 효과를 그림
        if (isBlinded && System.currentTimeMillis() < blindEndTime) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        } else if (isBlinded && System.currentTimeMillis() >= blindEndTime) {
            isBlinded = false;
        }
    }

    private void refreshScreen() {
        if (!isClosing) {
            synchronized(activeWords) {
                for (Word word : new ArrayList<>(activeWords)) {
                    word.setY(word.getY() + 2);
                    if (word.getY() > gamePanel.getHeight()) {
                        activeWords.remove(word);
                        client.sendGameAction(roomId, ClientEvent.WORD_MISSED, word.getText());
                    }
                }
            }
            updateGameInfo();
            gamePanel.repaint();
        }
    }

    private void updateGameInfo() {
        scoreLabel.setText(String.format("점수: %d", myScore));
        phLabel.setText(String.format("pH: %.1f", myPH));
        int phValue = (int)(myPH * 10);
        phMeter.setValue(phValue);

        if (myPH < 5.0) {
            phMeter.setForeground(Color.RED);
        } else if (myPH < 6.0) {
            phMeter.setForeground(Color.ORANGE);
        } else {
            phMeter.setForeground(ColorScheme.PRIMARY);
        }

        opponentScoreLabel.setText(String.format("상대방 점수: %d", opponentScore));
    }

    private void handleGameEnd() {
        if (isClosing) return;

        int option = JOptionPane.showConfirmDialog(this,
                "정말로 게임을 종료하시겠습니까?\n상대방이 자동으로 승리하게 됩니다.",
                "게임 종료",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            isClosing = true;

            if (screenRefreshTimer != null) {
                screenRefreshTimer.stop();
            }

            // 게임 중 퇴장 메시지 전송
            client.sendGameAction(roomId, "PLAYER_LEAVE_GAME", myName);

            // 방 나가기 처리
            client.sendMessage(ClientCommand.LEAVE_ROOM + "|" + roomId);
            client.setEventListener(null);

            if (mainFrame != null) {
                mainFrame.setVisible(true);
            }

            dispose();
        }
    }

    @Override
    public void onGameEvent(String eventType, Object... data) {
        if (isClosing) return;

        switch (eventType) {
            case "WORD_SPAWNED" -> {
                synchronized(activeWords) {
                    Word word = new Word((String)data[0], (int)data[1], 0);
                    if (data.length > 2) {
                        word.setSpecialEffect(true);
                        word.setEffect((Word.SpecialEffect)data[2]);
                    }
                    activeWords.add(word);
                }
            }

            case "WORD_MATCHED" -> {
                String wordText = (String) data[0];
                String playerName = (String) data[1];
                int newScore = (int) data[2];
                synchronized(activeWords) {
                    activeWords.removeIf(w -> w.getText().equals(wordText));
                }
                if (playerName.equals(myName)) {
                    myScore = newScore;
                } else {
                    opponentScore = newScore;
                }
                updateGameInfo();
            }

            case "WORD_MISSED" -> {
                String missedWord = (String) data[0];
                String playerNameMissed = (String) data[1];
                synchronized(activeWords) {
                    activeWords.removeIf(w -> w.getText().equals(missedWord));
                }
                if (playerNameMissed.equals(myName)) {
                    myPH = (double) data[2];
                }
                updateGameInfo();
            }

            case "PH_UPDATE" -> {
                String playerName = (String) data[0];
                double newPH = (double) data[1];
                if (playerName.equals(myName)) {
                    myPH = newPH;
                }
                updateGameInfo();
            }

            case "BLIND_EFFECT" -> {
                String targetPlayer = (String) data[0];
                int durationMs = (int) data[1];
                if (targetPlayer.equals(myName)) {
                    isBlinded = true;
                    blindEndTime = System.currentTimeMillis() + durationMs;
                    System.out.println("블라인드 효과 적용됨: " + durationMs + "ms");
                }
            }

            case "OPPONENT_LEFT_GAME" -> handleOpponentLeftGame((String)data[0]);

            case "GAME_OVER" -> {
                String winner = (String)data[0];
                int finalMyScore = (int)data[1];
                int finalOppScore = (int)data[2];
                boolean isForfeit = data.length >= 4 && (boolean)data[3];
                handleGameOver(winner, finalMyScore, finalOppScore, isForfeit);
            }

            case "ROOM_CLOSED" -> handleRoomClosed((String)data[1]);
        }

        gamePanel.repaint();
    }

    private void handleOpponentLeftGame(String opponentName) {
        if (isClosing) return;

        isClosing = true;
        if (screenRefreshTimer != null) {
            screenRefreshTimer.stop();
        }

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    opponentName + "님이 게임에서 나갔습니다.\n당신의 승리입니다!",
                    "게임 종료",
                    JOptionPane.INFORMATION_MESSAGE);

            returnToMainMenu(); // 메인 메뉴 복귀 메서드 호출
        });
    }

    private void handleGameOver(String winner, int finalMyScore, int finalOppScore, boolean isForfeit) {
        if (isClosing) return;

        isClosing = true;
        if (screenRefreshTimer != null) {
            screenRefreshTimer.stop();
        }

        SwingUtilities.invokeLater(() -> {
            // 결과 메시지 생성
            String resultMessage;
            if (winner.equals(myName)) {
                resultMessage = isForfeit ?
                        String.format("상대방이 게임을 나가서 승리했습니다!\n내 점수: %d\n상대방 점수: %d",
                                finalMyScore, finalOppScore) :
                        String.format("승리!\n내 점수: %d\n상대방 점수: %d",
                                finalMyScore, finalOppScore);
            } else {
                resultMessage = isForfeit ?
                        String.format("게임을 나가서 패배했습니다...\n내 점수: %d\n상대방 점수: %d",
                                finalOppScore, finalMyScore) :
                        String.format("패배...\n내 점수: %d\n상대방 점수: %d",
                                finalOppScore, finalMyScore);
            }

            // 결과 다이얼로그 표시
            JOptionPane.showMessageDialog(this,
                    resultMessage,
                    "게임 종료",
                    JOptionPane.INFORMATION_MESSAGE);

            // 메인 메뉴로 복귀
            returnToMainMenu();
        });
    }

    private void handleRoomClosed(String reason) {
        if (isClosing) return;

        isClosing = true;
        if (screenRefreshTimer != null) {
            screenRefreshTimer.stop();
        }

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "방이 닫혔습니다: " + reason,
                    "게임 종료",
                    JOptionPane.INFORMATION_MESSAGE);

            client.setEventListener(null);
            if (mainFrame != null) {
                mainFrame.setVisible(true);
            }
            dispose();
        });
    }

    private void returnToMainMenu() {
        SwingUtilities.invokeLater(() -> {
            try {
                // 방 나가기 처리
                client.sendMessage(ClientCommand.LEAVE_ROOM + "|" + roomId);

                // 새로운 MainMenu 인스턴스를 생성해서 현재 프레임에 표시
                MainMenu mainMenu = new MainMenu(client);
                client.setEventListener(mainMenu);

                // 현재 GameScreen 프레임의 내용을 모두 제거하고 MainMenu를 추가
                getContentPane().removeAll();
                add(mainMenu, BorderLayout.CENTER);
                revalidate();
                repaint();
                setTitle("메인 메뉴");

            } catch (Exception e) {
                logger.severe("메인 메뉴 복귀 중 오류: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "메인 메뉴로 복귀하는 중 오류가 발생했습니다.",
                        "오류",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }


    @Override
    public void dispose() {
        if (!isClosing) {
            handleGameEnd();
        } else {
            super.dispose();
        }
    }
}