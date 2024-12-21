/*
 * client.ui.theme.ColorScheme.java
 * 색상 테마를 정의하는 클래스
 */
package client.ui.theme;

import java.awt.Color;

public class ColorScheme {
    public static final Color BACKGROUND = new Color(28, 31, 43);
    public static final Color TEXT = Color.WHITE;
    public static final Color PRIMARY = new Color(71, 185, 251);
    public static final Color SECONDARY = new Color(45, 45, 60);
    public static final Color ACCENT = new Color(91, 205, 255);
    public static final Color ERROR = new Color(255, 99, 71);

    public static Color getTextColorForBackground(Color background) {
        double brightness = (background.getRed() * 299 +
                background.getGreen() * 587 +
                background.getBlue() * 114) / 1000.0;
        return brightness > 128 ? Color.BLACK : Color.WHITE;
    }
}
