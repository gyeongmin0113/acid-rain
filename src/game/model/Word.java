/*
 * game.model.Word.java
 * 단어를 나타내기 위한 모델 클래스
 */

package game.model;

import java.awt.Color;

public class Word {
    private final String text;
    private int x;
    private int y;
    private boolean hasSpecialEffect;
    private SpecialEffect effect;
    private Color color;

    public enum SpecialEffect {
        SCORE_BOOST,    // 점수 1.5배
        BLIND_OPPONENT  // 상대방 화면 가리기
    }

    public Word(String text, int x, int y) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.hasSpecialEffect = false;
        this.effect = null;
        this.color = Color.WHITE; // 기본 색상
    }

    // Getters & Setters
    public String getText() { return text; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public boolean hasSpecialEffect() { return hasSpecialEffect; }
    public void setSpecialEffect(boolean hasSpecialEffect) {
        this.hasSpecialEffect = hasSpecialEffect;
    }

    public SpecialEffect getEffect() { return effect; }
    public void setEffect(SpecialEffect effect) {
        this.effect = effect;
    }

    public Color getColor() { return color; }
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Word word = (Word) o;
        return text.equals(word.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "Word{" +
                "text='" + text + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", hasSpecialEffect=" + hasSpecialEffect +
                ", effect=" + effect +
                '}';
    }
}