/*
 * client.ui.components.RetroButton.java
 * 레트로한 디자인의 버튼 컴포넌트
 */
package client.ui.components;

import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import javax.swing.*;
import java.awt.*;

public class RetroButton extends JButton {
    public RetroButton(String text) {
        super(text);
        setupStyle();
    }

    private void setupStyle() {
        setFont(FontManager.getFont(16f));
        setForeground(ColorScheme.TEXT);
        setBackground(ColorScheme.PRIMARY);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(true);

        // 호버 효과
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                setBackground(ColorScheme.ACCENT);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                setBackground(ColorScheme.PRIMARY);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // 버튼 배경
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        // 텍스트
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

        g2d.setColor(getForeground());
        g2d.setFont(getFont());
        g2d.drawString(getText(), x, y);
    }
}
