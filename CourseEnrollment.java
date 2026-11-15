import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CourseEnrollment {

    // Интерфейс объявлен на уровне внешнего класса, чтобы вложенные классы его видели
    interface Enrollable {
        boolean enroll(String id);
        int size();
    }

    // ---------- Вариант A: coarse-grained lock ----------
    static class CourseLock implements Enrollable {
        private final int capacity;
        private final Set<String> students = new HashSet<>();
        private final Object lock = new Object();

        CourseLock(int capacity) { this.capacity = capacity; }

        @Override
        public boolean enroll(String studentId) {
            synchronized (lock) {
                if (students.contains(studentId)) return false;
                if (students.size() >= capacity) return false;
                students.add(studentId);
                return true;
            }
        }

        @Override
        public int size() { synchronized (lock) { return students.size(); } }
    }

    // ---------- Вариант B: атомарные операции + ConcurrentHashMap ----------
    static class CourseAtomic implements Enrollable {
        private final int capacity;
        private final AtomicInteger enrolled = new AtomicInteger(0);
        private final ConcurrentHashMap<String, Boolean> students = new ConcurrentHashMap<>();

        CourseAtomic(int capacity) { this.capacity = capacity; }

        @Override
        public boolean enroll(String studentId) {
            while (true) {
                int cur = enrolled.get();
                if (cur >= capacity) return false;
                if (enrolled.compareAndSet(cur, cur + 1)) break;
            }
            Boolean prev = students.putIfAbsent(studentId, Boolean.TRUE);
            if (prev != null) {
                enrolled.decrementAndGet();
                return false;
            }
            return true;
        }

        @Override
        public int size() { return enrolled.get(); }
    }

    // ---------- Демонстрация ----------
    public static void main(String[] args) throws Exception {
        runScenario("CourseLock",   new CourseLock(50));
        runScenario("CourseAtomic", new CourseAtomic(50));
    }

    static void runScenario(String name, Enrollable course) throws Exception {
        int threads = 5, perThread = 1000, studentPool = 1000;
        Thread[] ts = new Thread[threads];
        final AtomicInteger success = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            ts[t] = new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    String id = "s-" + (i % studentPool);
                    if (course.enroll(id)) success.incrementAndGet();
                }
            });
            ts[t].start();
        }
        for (Thread t : ts) t.join();

        System.out.println("[" + name + "] успешных записей: " + success.get()
                + ", size=" + course.size()
                + ", capacity=50 -> "
                + (course.size() <= 50 && success.get() <= 50 ? "OK" : "VIOLATION"));
    }
}