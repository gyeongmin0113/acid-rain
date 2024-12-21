package server.game;

import game.model.GameMode;
import game.model.Word;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public class ServerWordManager {
    private static final Logger logger = Logger.getLogger(ServerWordManager.class.getName());
    private static final String WORDS_DIRECTORY = "resources/words/";
    private final Map<GameMode, List<String>> wordsByMode = new HashMap<>();
    private final Random random = new Random();
    private final GameMode mode;

    public ServerWordManager(GameMode mode) {
        this.mode = mode;
        loadWordsForMode(mode);
        logger.info("단어 관리자 초기화: " + mode.name());
    }

    private void loadWordsForMode(GameMode mode) {
        String filename = WORDS_DIRECTORY + "words_" + mode.name().toLowerCase() + ".txt";
        Path filePath = Paths.get(filename);

        try {
            Files.createDirectories(Paths.get(WORDS_DIRECTORY));
        } catch (IOException e) {
            logger.severe("단어 디렉토리 생성 실패: " + e.getMessage());
        }

        if (!Files.exists(filePath)) {
            createDefaultWordFile(mode, filePath);
        }

        try {
            List<String> words = Files.readAllLines(filePath);
            words.removeIf(String::isEmpty);
            wordsByMode.put(mode, words);
            logger.info(mode.name() + " 모드의 단어 " + words.size() + "개 로드됨");
        } catch (IOException e) {
            logger.severe(mode.name() + " 단어 파일 읽기 실패: " + e.getMessage());
            wordsByMode.put(mode, getDefaultWords(mode));
        }
    }

    private List<String> getDefaultWords(GameMode mode) {
        return switch (mode) {
            case JAVA -> List.of(
                    "public", "class", "extends", "implements", "void",
                    "int", "boolean", "String", "final", "static",
                    "private", "protected", "abstract", "try", "catch",
                    "throw", "import", "return", "for", "while",
                    "interface", "package", "synchronized", "volatile", "transient"
            );
            case PYTHON -> List.of(
                    "def", "class", "import", "from", "as",
                    "if", "elif", "else", "while", "for",
                    "in", "try", "except", "finally", "with",
                    "print", "lambda", "yield", "global", "nonlocal",
                    "async", "await", "raise", "assert", "pass"
            );
            case KOTLIN -> List.of(
                    "fun", "val", "var", "class", "object",
                    "interface", "override", "private", "public", "protected",
                    "data", "sealed", "companion", "init", "constructor",
                    "suspend", "coroutine", "flow", "sequence", "lateinit"
            );
            case C -> List.of(
                    "int", "char", "float", "double", "void",
                    "long", "short", "signed", "unsigned", "struct",
                    "union", "enum", "typedef", "const", "static",
                    "extern", "register", "volatile", "sizeof", "switch"
            );
            default -> List.of("default", "word", "test");
        };
    }

    private void createDefaultWordFile(GameMode mode, Path filePath) {
        try {
            List<String> defaultWords = getDefaultWords(mode);
            Files.write(filePath, defaultWords);
            logger.info(mode.name() + " 기본 단어 파일 생성됨");
        } catch (IOException e) {
            logger.severe(mode.name() + " 기본 단어 파일 생성 실패: " + e.getMessage());
        }
    }

    public Word getRandomWord() {
        List<String> words = wordsByMode.getOrDefault(mode, Collections.emptyList());
        if (words.isEmpty()) {
            logger.warning(mode.name() + " 모드의 단어가 없습니다. 기본값 사용");
            return new Word("default", 100, 0);
        }

        String text = words.get(random.nextInt(words.size()));
        int xPos = random.nextInt(600) + 100; // 100~700 범위
        Word word = new Word(text, xPos, 0);

        // 20% 확률로 특수 효과 부여
        if (random.nextDouble() < 0.2) {
            word.setSpecialEffect(true);
            // 50% 확률로 점수 부스트 또는 상대방 블라인드
            if (random.nextBoolean()) {
                word.setEffect(Word.SpecialEffect.SCORE_BOOST);
            } else {
                word.setEffect(Word.SpecialEffect.BLIND_OPPONENT);
            }
            logger.fine("특수 효과 단어 생성: " + text + ", 효과: " + word.getEffect());
        }

        return word;
    }
}