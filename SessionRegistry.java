import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {

    public static final class Session {
        private final String id;
        private final long createdAt = System.nanoTime();
        private Session(String id) { this.id = id; }
        public String id() { return id; }
        public long createdAt() { return createdAt; }
    }

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    /** Атомарно: создать, только если отсутствует. */
    public Session createIfAbsent(String id) {
        return sessions.computeIfAbsent(id, Session::new);
    }

    public Session get(String id)      { return sessions.get(id); }
    public Session remove(String id)   { return sessions.remove(id); }
    public int size()                  { return sessions.size(); }

    // Демонстрация: два потока пытаются создать одну сессию — создаётся ровно одна
    public static void main(String[] args) throws Exception {
        SessionRegistry reg = new SessionRegistry();
        Thread[] ts = new Thread[8];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(() -> {
                for (int k = 0; k < 1000; k++) reg.createIfAbsent("s-" + k);
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        System.out.println("Уникальных сессий: " + reg.size()); // 1000
    }
}