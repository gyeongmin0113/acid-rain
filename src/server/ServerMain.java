package server;

public class ServerMain {
    private static GameServer server;

    public static void main(String[] args) {
        int port = 12345; // 기본 포트

        // 커맨드 라인 인자로 포트 번호를 받을 수 있도록 함
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException("포트 번호는 1-65535 사이여야 합니다.");
                }
            } catch (NumberFormatException e) {
                System.err.println("잘못된 포트 번호입니다. 기본 포트(12345)를 사용합니다.");
                port = 12345;
            }
        }

        // 서버 종료 훅 등록
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("서버를 종료합니다...");
            if (server != null) {
                server.shutdown();
            }
        }));

        try {
            server = new GameServer(port);
            System.out.println("타자 게임 서버를 시작합니다...");
            System.out.println("포트: " + port);
            System.out.println("서버를 종료하려면 Ctrl+C를 누르세요.");
            server.start();
        } catch (Exception e) {
            System.err.println("서버 실행 중 오류가 발생했습니다: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
