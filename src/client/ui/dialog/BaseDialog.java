 /*
  * client.ui.dialog.BaseDialog.java
  * 다이얼로그 창의 기본 틀을 정의하는 Base 클래스
  */

package client.ui.dialog;

import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import client.ui.theme.StyleManager;

import javax.swing.*;
import java.awt.*;

public class BaseDialog extends JDialog {
    protected final JPanel mainPanel;

    protected BaseDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);

        // 메인 패널 초기화
        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(ColorScheme.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 기본 설정
        setContentPane(mainPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    protected void addLabel(String text, GridBagConstraints gbc) {
        JLabel label = new JLabel(text);
        label.setForeground(ColorScheme.TEXT);
        label.setFont(FontManager.getFont(14f));
        mainPanel.add(label, gbc);
    }

    protected JPanel createButtonPanel(JButton... buttons) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(ColorScheme.BACKGROUND);

        for (JButton button : buttons) {
            StyleManager.applyButtonStyle(button);
            buttonPanel.add(button);
        }

        return buttonPanel;
    }

    protected void showError(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "오류",
                JOptionPane.ERROR_MESSAGE);
    }

    protected void centerOnScreen() {
        pack();
        setLocationRelativeTo(getOwner());
    }
}