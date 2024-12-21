/*
 * client.event.GameEventListener.java
 * 게임 내에서 발생하는 이벤트를 처리하기 위한 리스너 인터페이스
 */

package client.event;

public interface GameEventListener {
    void onGameEvent(String eventType, Object... data);
}