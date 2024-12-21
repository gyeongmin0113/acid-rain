/*
 * client.ui.dialog.CreateRoomDialog.java
 * 방을 만들기 위한 다이얼로그 창을 정의하는 클래스, 방 생성 시 필요한 정보를 입력받음
 */

package client.ui.dialog;

import client.ui.components.GameTextField;
import client.ui.components.RetroButton;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import game.model.DifficultyLevel;
import game.model.GameMode;
import game.model.GameRoom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CreateRoomDialog extends BaseDialog {
    private boolean roomCreated = false;
    private GameRoom createdRoom;
    private GameTextField roomNameField;
    private JPasswordField passwordField;
    private JComboBox<GameMode> gameModeCombo;
    private JComboBox<DifficultyLevel> difficultyCombo;
    private JSpinner maxPlayersSpinner;

    public CreateRoomDialog(Window owner) {
        super(owner, "방 만들기");
        setupUI();
        setupKeyboardShortcuts();
        setupWindowListener();
        centerOnScreen();
    }

    private void setupUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0;
        addLabel("방 제목:", gbc);

        gbc.gridx = 1;
        roomNameField = createTextField();
        mainPanel.add(roomNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        addLabel("비밀번호:", gbc);

        gbc.gridx = 1;
        passwordField = createPasswordField();
        mainPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        addLabel("프로그래밍 언어:", gbc);

        gbc.gridx = 1;
        gameModeCombo = new JComboBox<>(GameMode.values());
        stylizeComboBox(gameModeCombo);
        mainPanel.add(gameModeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        addLabel("난이도:", gbc);

        gbc.gridx = 1;
        difficultyCombo = new JComboBox<>(DifficultyLevel.values());
        stylizeComboBox(difficultyCombo);
        mainPanel.add(difficultyCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        addLabel("최대 인원:", gbc);

        gbc.gridx = 1;
        SpinnerModel spinnerModel = new SpinnerNumberModel(2, 2, 4, 1);
        maxPlayersSpinner = createSpinner(spinnerModel);
        mainPanel.add(maxPlayersSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        RetroButton createButton = new RetroButton("생성 (Enter)");
        RetroButton cancelButton = new RetroButton("취소 (ESC)");
        createButton.addActionListener(e -> handleCreateRoom());
        cancelButton.addActionListener(e -> dispose());

        mainPanel.add(createButtonPanel(createButton, cancelButton), gbc);
    }

    private void stylizeComboBox(JComboBox<?> comboBox) {
        comboBox.setRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(FontManager.getFont(14f));
                label.setForeground(ColorScheme.TEXT);
                label.setBackground(isSelected ? ColorScheme.PRIMARY : ColorScheme.SECONDARY);
                label.setOpaque(true);
                return label;
            }
        });
        comboBox.setBackground(ColorScheme.SECONDARY);
        comboBox.setForeground(ColorScheme.TEXT);
        comboBox.setFont(FontManager.getFont(14f));
        comboBox.setBorder(BorderFactory.createLineBorder(ColorScheme.PRIMARY));
    }

    private void setupKeyboardShortcuts() {
        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        getRootPane().registerKeyboardAction(
                e -> handleCreateRoom(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void setupWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                roomNameField.requestFocus();
            }
        });
    }

    private void handleCreateRoom() {
        if (validateInput()) {
            createdRoom = new GameRoom(
                    roomNameField.getText().trim(),
                    new String(passwordField.getPassword()),
                    (GameMode) gameModeCombo.getSelectedItem(),
                    (DifficultyLevel) difficultyCombo.getSelectedItem(),
                    (Integer) maxPlayersSpinner.getValue()
            );
            roomCreated = true;
            dispose();
        }
    }

    private boolean validateInput() {
        String roomName = roomNameField.getText().trim();
        if (roomName.isEmpty()) {
            showError("방 제목을 입력해주세요.");
            roomNameField.requestFocus();
            return false;
        }
        if (roomName.length() < 2 || roomName.length() > 20) {
            showError("방 제목은 2-20자 사이여야 합니다.");
            roomNameField.requestFocus();
            return false;
        }
        return true;
    }

    private GameTextField createTextField() {
        GameTextField field = new GameTextField(20);
        field.setFont(FontManager.getFont(14f));
        return field;
    }

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField(20);
        field.setBackground(ColorScheme.SECONDARY);
        field.setForeground(ColorScheme.TEXT);
        field.setCaretColor(ColorScheme.TEXT);
        field.setFont(FontManager.getFont(14f));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return field;
    }

    private JSpinner createSpinner(SpinnerModel model) {
        JSpinner spinner = new JSpinner(model);
        spinner.setFont(FontManager.getFont(14f));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setBackground(ColorScheme.SECONDARY);
            textField.setForeground(ColorScheme.TEXT);
            textField.setCaretColor(ColorScheme.TEXT);
            textField.setHorizontalAlignment(JTextField.CENTER);
        }
        return spinner;
    }

    public boolean isRoomCreated() {
        return roomCreated;
    }

    public GameRoom getCreatedRoom() {
        return createdRoom;
    }
}
