public class LostIncrements {

    // Проблемный вариант
    static class Broken {
        static int counter = 0;

        static void run() throws Exception {
            int threads = 8, perThread = 1_000_000;
            Thread[] ts = new Thread[threads];
            for (int i = 0; i < threads; i++) {
                ts[i] = new Thread(() -> {
                    for (int j = 0; j < perThread; j++) counter++;
                });
                ts[i].start();
            }
            for (Thread t : ts) t.join();
            System.out.println("[Broken] Ожидалось: " + (long) threads * perThread);
            System.out.println("[Broken] Получено:  " + counter);
        }
    }

    // Исправленный вариант: synchronized-метод
    static class Fixed {
        private long counter = 0;
        public synchronized void increment() { counter++; }
        public synchronized long value()     { return counter; }
    }
    static class FixedBlock {
        private long counter = 0;
        private long unrelatedHeavyWork = 0;
        public synchronized void incrementMethod() {
            counter++;
            heavyWork();
        }
        public void incrementBlock() {
            synchronized (this) {
                counter++;
            }
            heavyWork();
        }
        private void heavyWork() {
            for (int i = 0; i < 1000; i++) unrelatedHeavyWork += i;
        }
        public synchronized long value() { return counter; }
    }
    public static void main(String[] args) throws Exception {
        // Многократно выявляем проблему
        for (int run = 0; run < 3; run++) {
            Broken.counter = 0;
            Broken.run();
        }
        Fixed f = new Fixed();
        int threads = 8, perThread = 1_000_000;
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                for (int j = 0; j < perThread; j++) f.increment();
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        System.out.println("[Fixed] Ожидалось: " + (long) threads * perThread);
        System.out.println("[Fixed] Получено:  " + f.value());
    }
}