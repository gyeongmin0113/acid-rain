/*
 * client.ui.dialog.LoginDialog.java
 * 로그인을 위한 다이얼로그 창을 정의하는 클래스, 로그인 시 필요한 정보를 입력받음
 */
package client.ui.dialog;

import client.ui.components.GameTextField;
import client.ui.components.RetroButton;
import client.config.GameConfig;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends BaseDialog {
    private GameTextField nicknameField;
    private GameTextField serverAddressField;
    private GameTextField portField;
    private boolean accepted = false;

    public LoginDialog(JFrame parent) {
        super(parent, "로그인");
        setupUI();
        centerOnScreen();
    }

    private void setupUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 필드 생성
        nicknameField = new GameTextField(15);
        serverAddressField = new GameTextField(15);
        serverAddressField.setText("localhost");
        portField = new GameTextField(15);
        portField.setText(String.valueOf(GameConfig.DEFAULT_PORT));

        // 레이블과 필드 배치
        gbc.gridx = 0; gbc.gridy = 0;
        addLabel("닉네임:", gbc);

        gbc.gridx = 1;
        mainPanel.add(nicknameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        addLabel("서버 주소:", gbc);

        gbc.gridx = 1;
        mainPanel.add(serverAddressField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        addLabel("포트:", gbc);

        gbc.gridx = 1;
        mainPanel.add(portField, gbc);

        // 버튼 패널
        RetroButton connectButton = new RetroButton("접속");
        RetroButton cancelButton = new RetroButton("취소");

        connectButton.addActionListener(e -> {
            if (validateInput()) {
                accepted = true;
                dispose();
            }
        });

        cancelButton.addActionListener(e -> {
            accepted = false;
            dispose();
        });

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(createButtonPanel(connectButton, cancelButton), gbc);
    }

    private boolean validateInput() {
        if (nicknameField.getText().trim().isEmpty()) {
            showError("닉네임을 입력해주세요.");
            return false;
        }
        if (serverAddressField.getText().trim().isEmpty()) {
            showError("서버 주소를 입력해주세요.");
            return false;
        }
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) {
                showError("포트 번호는 1-65535 사이의 값이어야 합니다.");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("올바른 포트 번호를 입력해주세요.");
            return false;
        }
        return true;
    }

    public String getNickname() {
        return nicknameField.getText().trim();
    }

    public String getServerAddress() {
        return serverAddressField.getText().trim();
    }

    public int getPort() {
        return Integer.parseInt(portField.getText().trim());
    }

    public boolean isAccepted() {
        return accepted;
    }
}