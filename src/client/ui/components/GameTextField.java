/*
 * client.ui.components.GameTextField.java
 * 프로그램 전반적으로 사용하는 기본 텍스트 필드 정의
 */
package client.ui.components;

import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import javax.swing.*;
import javax.swing.border.Border;

public class GameTextField extends JTextField {
    public GameTextField() {
        this(20);
    }

    public GameTextField(int columns) {
        super(columns);
        setupStyle();
    }

    private void setupStyle() {
        setFont(FontManager.getFont(14f));
        setBackground(ColorScheme.SECONDARY);
        setForeground(ColorScheme.TEXT);
        setCaretColor(ColorScheme.TEXT);

        Border lineBorder = BorderFactory.createLineBorder(ColorScheme.PRIMARY);
        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        setBorder(BorderFactory.createCompoundBorder(lineBorder, emptyBorder));
    }
}
