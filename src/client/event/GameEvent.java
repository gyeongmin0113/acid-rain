/*
 * client.event.GameEvent.java
 * 게임 내에서 발생하는 이벤트와 프로토콜 메시지를 정의하는 클래스
 * 서버-클라이언트 간 통신에 사용되는 모든 상수들을 체계적으로 관리
 * 처음에 각 코드에서 정의해서 사용했으나, 중복 / 불일치가 발생하여 작성함
 * 다만, 리팩터링 과정에서 누락된 부분이 있을 수 있음. 발견되는 대로 수정이 필요함...
 */

package client.event;

public class GameEvent {
    /*
     * ============================
     * =     클라이언트 이벤트     =
     * ============================
     */
    public static class ClientEvent {
        // 방 관련 이벤트
        public static final String ROOM_CREATED = "ROOM_CREATED";           // 방 생성됨
        public static final String ROOM_JOINED = "ROOM_JOINED";            // 방 입장함
        public static final String ROOM_LIST_UPDATED = "ROOM_LIST_UPDATED"; // 방 목록 업데이트됨
        public static final String ROOM_CLOSED = "ROOM_CLOSED";            // 방 닫힘
        public static final String HOST_LEFT = "HOST_LEFT";                // 방장이 나감
        public static final String NEW_HOST = "NEW_HOST";                  // 새로운 방장 선정됨

        // 게임 상태 이벤트
        public static final String GAME_STARTED = "GAME_STARTED";          // 게임 시작됨
        public static final String GAME_OVER = "GAME_OVER";               // 게임 종료됨

        // 플레이어 관련 이벤트
        public static final String PLAYER_UPDATED = "PLAYER_UPDATED";      // 플레이어 정보 업데이트
        public static final String USERS_UPDATED = "USERS_UPDATED";        // 전체 유저수 업데이트

        // 채팅 관련 이벤트
        public static final String CHAT_RECEIVED = "CHAT_RECEIVED";        // 채팅 메시지 수신

        // 게임 플레이 이벤트
        public static final String WORD_SPAWNED = "WORD_SPAWNED";         // 새 단어 생성됨
        public static final String WORD_MATCHED = "WORD_MATCHED";         // 단어 매치됨
        public static final String WORD_MISSED = "WORD_MISSED";           // 단어 놓침
        public static final String BLIND_EFFECT = "BLIND_EFFECT";         // 블라인드 효과 발동
        public static final String PH_UPDATE = "PH_UPDATE";               // pH 값 업데이트

        // 리더보드 이벤트
        public static final String TOP_SCORES = "TOP_SCORES";             // 최고 점수 데이터
        public static final String USER_RECORDS = "USER_RECORDS";         // 유저 기록 데이터
        public static final String LEADERBOARD_UPDATE = "LEADERBOARD_UPDATE"; // 리더보드 업데이트

        // 설정 관련 이벤트
        public static final String SETTINGS_UPDATED = "SETTINGS_UPDATED";  // 게임 설정 업데이트

        // 오류 이벤트
        public static final String ERROR_OCCURRED = "ERROR_OCCURRED";      // 오류 발생
    }

    /*
     * ============================
     * =      클라이언트 커맨드     =
     * ============================
     */
    public static class ClientCommand {
        // 인증 관련 커맨드
        public static final String LOGIN = "LOGIN";                      // 로그인 요청
        public static final String LOGOUT = "LOGOUT";                    // 로그아웃 요청

        // 방 관련 커맨드
        public static final String CREATE_ROOM = "CREATE_ROOM";          // 방 생성 요청
        public static final String JOIN_ROOM = "JOIN_ROOM";             // 방 입장 요청
        public static final String LEAVE_ROOM = "LEAVE_ROOM";           // 방 퇴장 요청
        public static final String ROOM_LIST = "ROOM_LIST";             // 방 목록 요청
        public static final String PLAYER_LIST = "PLAYER_LIST";         // 플레이어 목록 요청

        // 게임 플레이 관련 커맨드
        public static final String START_GAME = "START_GAME";           // 게임 시작 요청
        public static final String WORD_INPUT = "WORD_INPUT";           // 단어 입력
        public static final String GAME_ACTION = "GAME_ACTION";         // 게임 액션

        // 채팅 관련 커맨드
        public static final String CHAT = "CHAT";                       // 채팅 메시지 전송 ** 서로 다름 **

        // must be removed ...
        public static final String USERS_REQUEST = "USERS_REQUEST";     // 전체 유저수 요청
    }

    /*
     * ============================
     * =       서버 응답 타입      =
     * ============================
     */
    public static class ServerMessage {
        // 유저 및 방 관리 메시지
        public static final String USERS = "USERS";                           // 전체 유저수 응답
        public static final String ROOM_LIST_RESPONSE = "ROOM_LIST_RESPONSE"; // 방 목록 응답
        public static final String PLAYER_LIST_RESPONSE = "PLAYER_LIST_RESPONSE"; // 플레이어 목록 응답
        public static final String CREATE_ROOM_RESPONSE = "CREATE_ROOM_RESPONSE"; // 방 생성 응답
        public static final String JOIN_ROOM_RESPONSE = "JOIN_ROOM_RESPONSE"; // 방 입장 응답
        public static final String ROOM_CLOSED = "ROOM_CLOSED";              // 방 닫힘 알림
        public static final String HOST_LEFT = "HOST_LEFT";                  // 방장 퇴장 알림
        public static final String NEW_HOST = "NEW_HOST";                    // 새 방장 알림

        // 게임 플레이 메시지
        public static final String WORD_SPAWNED = "WORD_SPAWNED";           // 단어 생성 알림
        public static final String WORD_MATCHED = "WORD_MATCHED";           // 단어 매치 알림
        public static final String WORD_MISSED = "WORD_MISSED";             // 단어 미스 알림
        public static final String BLIND_EFFECT = "BLIND_EFFECT";           // 블라인드 효과 알림
        public static final String GAME_OVER = "GAME_OVER";                 // 게임 종료 알림
        public static final String PH_UPDATE = "PH_UPDATE";                 // pH 업데이트 알림

        // 게임 상태 및 설정 메시지
        public static final String PLAYER_UPDATE = "PLAYER_UPDATE";         // 플레이어 정보 업데이트
        public static final String SETTINGS_UPDATE = "SETTINGS_UPDATED";     // 설정 업데이트 알림
        public static final String GAME_START = "GAME_START";              // 게임 시작 알림

        // 채팅 메시지
        public static final String CHAT = "CHAT";                          // 채팅 메시지 알림 ** 서로 다름 **

        // 리더보드 관련 메시지
        public static final String LEADERBOARD_DATA = "LEADERBOARD_DATA";     // 리더보드 데이터
        public static final String LEADERBOARD_UPDATE = "LEADERBOARD_UPDATE"; // 리더보드 업데이트
        public static final String MY_RECORDS_DATA = "MY_RECORDS_DATA";       // 개인 기록 데이터

        // 에러 메시지
        public static final String ERROR = "ERROR";                        // 에러 알림
    }
}