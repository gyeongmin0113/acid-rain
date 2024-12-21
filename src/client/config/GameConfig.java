/*
 * client.config.GameConfig.java
 * 게임 설정값을 정의하는 클래스, 모든 설정 값이 이용되진 않지만, 추후 확장을 위해 작성됨
 */

package client.config;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GameConfig {
    private static final Properties properties = new Properties();

    // 기본 설정값들
    public static final Color BACKGROUND_COLOR = new Color(28, 31, 43);
    public static final Color TEXT_COLOR = Color.WHITE;
    public static final Color PRIMARY_COLOR = new Color(71, 185, 251);
    public static final int DEFAULT_PORT = 12345;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = GameConfig.class.getClassLoader()
                .getResourceAsStream("config/game.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("설정 파일을 불러오는데 실패했습니다: " + e.getMessage());
        }
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}