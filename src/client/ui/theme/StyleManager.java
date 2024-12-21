/*
 * client.ui.theme.StyleManager.java
 * UI 컴포넌트의 스타일을 일관되게 적용하는 클래스
 * -> 확장에 대비 하세요 ! -> 다만 이번 프로젝트에서 실질적으로 사용하지 않았음.
 */

package client.ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class StyleManager {
    public static void applyDefaultStyle(JComponent component) {
        component.setBackground(ColorScheme.BACKGROUND);
        component.setForeground(ColorScheme.TEXT);
        component.setFont(FontManager.getFont(14f));
    }

    public static void applyInputStyle(JTextField textField) {
        textField.setBackground(ColorScheme.SECONDARY);
        textField.setForeground(ColorScheme.TEXT);
        textField.setCaretColor(ColorScheme.TEXT);
        textField.setFont(FontManager.getFont(14f));
        textField.setBorder(createInputBorder());
    }

    public static void applyLabelStyle(JComponent label) {
        label.setForeground(ColorScheme.TEXT);
        label.setFont(FontManager.getFont(14f));
        if (label instanceof JLabel) {
            ((JLabel) label).setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }
    }

    public static void applyButtonStyle(JButton button) {
        button.setBackground(ColorScheme.PRIMARY);
        button.setForeground(ColorScheme.TEXT);
        button.setFont(FontManager.getFont(14f));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(ColorScheme.ACCENT);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(ColorScheme.PRIMARY);
            }
        });
    }

    public static void applyComboBoxStyle(JComboBox<?> comboBox) {
        comboBox.setBackground(ColorScheme.SECONDARY);
        comboBox.setForeground(ColorScheme.TEXT);
        comboBox.setFont(FontManager.getFont(14f));
        ((JComponent) comboBox.getRenderer()).setBackground(ColorScheme.SECONDARY);
        comboBox.setBorder(BorderFactory.createLineBorder(ColorScheme.PRIMARY));
    }

    public static void applyChatAreaStyle(JTextArea chatArea) {
        chatArea.setBackground(ColorScheme.SECONDARY);
        chatArea.setForeground(ColorScheme.TEXT);
        chatArea.setFont(FontManager.getFont(14f));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setMargin(new Insets(5, 5, 5, 5));
    }

    public static Border createInputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );
    }

    public static Color getTextColorForBackground(Color background) {
        // 배경색의 밝기를 계산
        double brightness = (background.getRed() * 299 +
                background.getGreen() * 587 +
                background.getBlue() * 114) / 1000.0;
        // 밝은 배경색에는 어두운 텍스트, 어두운 배경색에는 밝은 텍스트
        return brightness > 128 ? Color.BLACK : Color.WHITE;
    }
}