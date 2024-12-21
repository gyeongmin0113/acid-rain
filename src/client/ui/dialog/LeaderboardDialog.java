/*
 * client.ui.dialog.LeaderboardDialog.java
 * 게임의 리더보드(순위표) 기능을 담당하는 다이얼로그 클래스.
 * 전체 순위와 개인 기록을 표시하며, 게임 모드와 난이도별 필터링을 지원함.
 */

package client.ui.dialog;

import client.app.GameClient;
import client.event.GameEventListener;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import game.model.GameMode;
import game.model.DifficultyLevel;
import game.model.LeaderboardEntry;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class LeaderboardDialog extends BaseDialog implements GameEventListener {
    private static final Logger logger = Logger.getLogger(LeaderboardDialog.class.getName());

    private final GameClient client;
    private final JTabbedPane tabbedPane;
    private final JTable globalTable;
    private final JTable myRecordsTable;
    private final DefaultTableModel globalModel;
    private final DefaultTableModel myRecordsModel;
    private final JComboBox<GameModeWrapper> modeFilter;
    private final JComboBox<DifficultyWrapper> difficultyFilter;
    private final DateTimeFormatter dateFormatter;

    // 게임모드와 난이도를 위한 래퍼 클래스
    private static class GameModeWrapper {
        private final GameMode mode;

        public GameModeWrapper(GameMode mode) {
            this.mode = mode;
        }

        @Override
        public String toString() {
            return mode.getDisplayName();
        }

        public GameMode getMode() {
            return mode;
        }
    }

    private static class DifficultyWrapper {
        private final DifficultyLevel difficulty;

        public DifficultyWrapper(DifficultyLevel difficulty) {
            this.difficulty = difficulty;
        }

        @Override
        public String toString() {
            return difficulty.getDisplayName();
        }

        public DifficultyLevel getDifficulty() {
            return difficulty;
        }
    }

    public LeaderboardDialog(Window owner, GameClient client) {
        super(owner, "리더보드");
        this.client = client;
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 기본 설정
        setSize(800, 600);
        setResizable(true);
        setMinimumSize(new Dimension(600, 400));

        // 테이블 모델 초기화
        String[] columns = {"순위", "닉네임", "점수", "게임 모드", "난이도", "달성 일시"};
        globalModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        myRecordsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // 필터 콤보박스 초기화
        modeFilter = new JComboBox<>(createGameModeWrappers());
        difficultyFilter = new JComboBox<>(createDifficultyWrappers());

        // 컴포넌트 초기화
        tabbedPane = new JTabbedPane();
        globalTable = createTable(globalModel);
        myRecordsTable = createTable(myRecordsModel);

        setupUI();
        client.setEventListener(this);
        loadLeaderboard(); // 초기 데이터 로드
    }

    // 게임 모드 래퍼 배열 생성
    private GameModeWrapper[] createGameModeWrappers() {
        GameMode[] modes = GameMode.values();
        GameModeWrapper[] wrappers = new GameModeWrapper[modes.length];
        for (int i = 0; i < modes.length; i++) {
            wrappers[i] = new GameModeWrapper(modes[i]);
        }
        return wrappers;
    }

    // 난이도 래퍼 배열 생성
    private DifficultyWrapper[] createDifficultyWrappers() {
        DifficultyLevel[] difficulties = DifficultyLevel.values();
        DifficultyWrapper[] wrappers = new DifficultyWrapper[difficulties.length];
        for (int i = 0; i < difficulties.length; i++) {
            wrappers[i] = new DifficultyWrapper(difficulties[i]);
        }
        return wrappers;
    }

    private void setupUI() {
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 필터 패널 설정
        JPanel filterPanel = createFilterPanel();
        mainPanel.add(filterPanel, BorderLayout.NORTH);

        // 테이블 패널 설정
        tabbedPane.addTab("전체 순위", createTablePanel(globalTable));
        tabbedPane.addTab("내 기록", createTablePanel(myRecordsTable));
        styleTabbedPane(tabbedPane);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // 이벤트 리스너 설정
        setupEventListeners();
    }

    // 필터 패널 생성
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBackground(ColorScheme.BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.PRIMARY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // 게임 모드 필터
        addLabelAndComboBox(panel, "게임 모드:", modeFilter);

        // 난이도 필터
        addLabelAndComboBox(panel, "난이도:", difficultyFilter);

        // 새로고침 버튼
        JButton refreshButton = createStyledButton("새로고침");
        panel.add(Box.createHorizontalStrut(20));
        panel.add(refreshButton);

        return panel;
    }

    // 레이블과 콤보박스 추가 헬퍼 메소드
    private void addLabelAndComboBox(JPanel panel, String labelText, JComboBox<?> comboBox) {
        JLabel label = new JLabel(labelText);
        label.setForeground(ColorScheme.TEXT);
        label.setFont(FontManager.getFont(14f));
        panel.add(label);

        styleComboBox(comboBox);
        panel.add(comboBox);
    }

    // 이벤트 리스너 설정
    private void setupEventListeners() {
        modeFilter.addActionListener(e -> loadLeaderboard());
        difficultyFilter.addActionListener(e -> loadLeaderboard());
    }

    // 리더보드 데이터 로드
    private void loadLeaderboard() {
        clearTables();
        GameMode mode = ((GameModeWrapper) modeFilter.getSelectedItem()).getMode();
        DifficultyLevel difficulty = ((DifficultyWrapper) difficultyFilter.getSelectedItem()).getDifficulty();

        // 서버에 데이터 요청
        client.sendMessage("LEADERBOARD_ACTION|GET_TOP|" + mode.name() + "|" + difficulty.name());
        client.sendMessage("LEADERBOARD_ACTION|GET_MY_RECORDS|" + mode.name() + "|" + difficulty.name());

    }

    // 테이블 생성
    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component comp = super.prepareRenderer(renderer, row, column);
                if (comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    if (column == 2) { // 점수 칼럼
                        label.setHorizontalAlignment(SwingConstants.RIGHT);
                    } else {
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                }
                return comp;
            }
        };

        // 테이블 스타일링
        styleTable(table);
        return table;
    }

    // 테이블 스타일링
    private void styleTable(JTable table) {
        table.setBackground(ColorScheme.SECONDARY);
        table.setForeground(ColorScheme.TEXT);
        table.setFont(FontManager.getFont(14f));
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(ColorScheme.PRIMARY.darker());
        table.setSelectionBackground(ColorScheme.PRIMARY.brighter());
        table.setSelectionForeground(ColorScheme.TEXT);
        table.setFocusable(false);

        // 헤더 스타일링
        JTableHeader header = table.getTableHeader();
        header.setBackground(ColorScheme.PRIMARY);
        header.setForeground(ColorScheme.TEXT);
        header.setFont(FontManager.getFont(14f).deriveFont(Font.BOLD));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ColorScheme.PRIMARY.darker()));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        // 열 너비 설정
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);   // 순위
        columnModel.getColumn(1).setPreferredWidth(120);  // 닉네임
        columnModel.getColumn(2).setPreferredWidth(100);  // 점수
        columnModel.getColumn(3).setPreferredWidth(100);  // 게임 모드
        columnModel.getColumn(4).setPreferredWidth(80);   // 난이도
        columnModel.getColumn(5).setPreferredWidth(150);  // 달성 일시
    }

    // 게임 이벤트 처리
    @Override
    public void onGameEvent(String eventType, Object... data) {
        switch (eventType) {
            case "TOP_SCORES" -> handleTopScores(data);
            case "USER_RECORDS" -> handleUserRecords(data);
        }
    }

    // 전체 순위 데이터 처리
    private void handleTopScores(Object... data) {
        SwingUtilities.invokeLater(() -> {
            try {
                globalModel.setRowCount(0);
                List<LeaderboardEntry> entries = parseLeaderboardEntries(data);

                GameMode selectedMode = ((GameModeWrapper) modeFilter.getSelectedItem()).getMode();
                DifficultyLevel selectedDifficulty = ((DifficultyWrapper) difficultyFilter.getSelectedItem()).getDifficulty();

                int rank = 1;
                for (LeaderboardEntry entry : entries) {
                    if (entry.getGameMode() == selectedMode &&
                            entry.getDifficulty() == selectedDifficulty) {
                        addEntryToModel(globalModel, rank++, entry);
                    }
                }
            } catch (Exception e) {
                logger.warning("리더보드 데이터 처리 중 오류: " + e.getMessage());
                showError("리더보드 데이터를 불러오는 중 오류가 발생했습니다.");
            }
        });
    }

    private void handleUserRecords(Object... data) {
        SwingUtilities.invokeLater(() -> {
            try {
                myRecordsModel.setRowCount(0);
                List<LeaderboardEntry> entries = parseLeaderboardEntries(data);

                GameMode selectedMode = ((GameModeWrapper) modeFilter.getSelectedItem()).getMode();
                DifficultyLevel selectedDifficulty = ((DifficultyWrapper) difficultyFilter.getSelectedItem()).getDifficulty();

                int rank = 1;
                for (LeaderboardEntry entry : entries) {
                    if (entry.getGameMode() == selectedMode &&
                            entry.getDifficulty() == selectedDifficulty) {
                        addEntryToModel(myRecordsModel, rank++, entry);
                    }
                }
            } catch (Exception e) {
                logger.warning("개인 기록 데이터 처리 중 오류: " + e.getMessage());
                showError("개인 기록을 불러오는 중 오류가 발생했습니다.");
            }
        });
    }


    // 테이블 모델에 엔트리 추가
    private void addEntryToModel(DefaultTableModel model, int rank, LeaderboardEntry entry) {
        model.addRow(new Object[]{
                rank,
                entry.getUsername(),
                String.format("%,d", entry.getScore()),
                entry.getGameMode().getDisplayName(),
                entry.getDifficulty().getDisplayName(),
                entry.getTimestamp().format(dateFormatter)
        });
    }

    // 리더보드 엔트리 파싱
    private List<LeaderboardEntry> parseLeaderboardEntries(Object... data) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (data.length == 0) return entries;

        for (Object obj : data) {
            if (obj instanceof String) {
                String row = ((String) obj).trim();
                if (!row.isEmpty()) {
                    try {
                        LeaderboardEntry entry = LeaderboardEntry.fromString(row);
                        entries.add(entry);
                    } catch (Exception e) {
                        logger.warning("엔트리 파싱 실패: " + row + ", 오류: " + e.getMessage());
                    }
                }
            } else {
                logger.warning("예상치 못한 데이터 타입: " + obj.getClass().getName());
            }
        }

        // 점수 내림차순
        entries.sort(Comparator
                .comparingInt(LeaderboardEntry::getScore).reversed());


        return entries;
    }

    // UI 스타일링 관련 메서드들
    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(ColorScheme.SECONDARY);
        comboBox.setForeground(ColorScheme.TEXT);
        comboBox.setFont(FontManager.getFont(14f));
        comboBox.setPreferredSize(new Dimension(120, 30));
        comboBox.setBorder(BorderFactory.createLineBorder(ColorScheme.PRIMARY));

        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        };
        comboBox.setRenderer(renderer);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ColorScheme.PRIMARY);
        button.setForeground(ColorScheme.TEXT);
        button.setFont(FontManager.getFont(14f));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.PRIMARY.darker()),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));

        // 호버 효과
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(ColorScheme.PRIMARY.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(ColorScheme.PRIMARY);
            }
        });

        button.addActionListener(e -> loadLeaderboard());
        return button;
    }

    private void styleTabbedPane(JTabbedPane tabbedPane) {
        tabbedPane.setBackground(ColorScheme.BACKGROUND);
        tabbedPane.setForeground(ColorScheme.TEXT);
        tabbedPane.setFont(FontManager.getFont(14f));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private JPanel createTablePanel(JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(ColorScheme.BACKGROUND);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 스크롤바 스타일링
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = ColorScheme.PRIMARY;
                this.trackColor = ColorScheme.SECONDARY;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
        });

        panel.add(scrollPane);
        return panel;
    }

    private void clearTables() {
        globalModel.setRowCount(0);
        myRecordsModel.setRowCount(0);
    }

    @Override
    public void dispose() {
        client.setEventListener(null);
        super.dispose();
    }
}