/**
 * client.ui.theme.FontManager.java
 * 폰트를 로드하고 캐싱하는 클래스 -> 이모지는 별도로 처리할 수 있도록 해야함 !
 */

package client.ui.theme;

import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static final Map<String, Font> fontCache = new HashMap<>();
    private static final String DEFAULT_FONT_RESOURCE = "/fonts/DungGeunMo.ttf";
    private static Font emojiFont = null;  // 이모지 폰트를 캐시할 변수

    public static Font getFont(float size) {
        return getCustomFont(DEFAULT_FONT_RESOURCE, size);
    }

    public static Font getEmojiFont(float size) {
        if (emojiFont == null) {
            Font[] candidates = {
                    new Font("Segoe UI Emoji", Font.PLAIN, (int)size),    // Windows
                    new Font("Apple Color Emoji", Font.PLAIN, (int)size),  // macOS
                    new Font("Noto Color Emoji", Font.PLAIN, (int)size),  // Linux
                    new Font("Noto Emoji", Font.PLAIN, (int)size)         // 대체 폰트
            };

            for (Font font : candidates) {
                if (font.canDisplayUpTo("⚡") == -1 && font.canDisplayUpTo("⭐") == -1) {
                    emojiFont = font;
                    break;
                }
            }

            if (emojiFont == null) {
                emojiFont = candidates[0];
            }
        }

        return emojiFont.deriveFont(size);
    }

    public static Font getCustomFont(String resourcePath, float size) {
        String key = resourcePath + size;
        return fontCache.computeIfAbsent(key, k -> {
            try (InputStream is = FontManager.class.getResourceAsStream(resourcePath)) {
                if (is == null) {
                    System.err.println("폰트 리소스를 찾을 수 없습니다: " + resourcePath);
                    return new Font("Dialog", Font.PLAIN, (int)size);
                }
                return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
            } catch (Exception e) {
                System.err.println("폰트 로딩 실패: " + e.getMessage());
                return new Font("Dialog", Font.PLAIN, (int) size);
            }
        });
    }
}