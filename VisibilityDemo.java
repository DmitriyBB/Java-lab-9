public class VisibilityDemo {
    static class Broken {
        static boolean running = true;

        static void run() throws Exception {
            Thread worker = new Thread(() -> {
                long n = 0;
                while (running) n++;
                System.out.println("[Broken] worker stopped, n=" + n);
            });
            worker.setDaemon(true);
            worker.start();
            Thread.sleep(300);
            running = false;
            worker.join(1000);
            System.out.println("[Broken] alive after stop = " + worker.isAlive());
        }
    }
    static class VolatileFix {
        static volatile boolean running = true;
        static void run() throws Exception {
            Thread worker = new Thread(() -> {
                long n = 0;
                while (running) n++;
                System.out.println("[Volatile] worker stopped, n=" + n);
            });
            worker.setDaemon(true);
            worker.start();
            Thread.sleep(300);
            running = false;
            worker.join(1000);
            System.out.println("[Volatile] alive after stop = " + worker.isAlive());
        }
    }
    static class SyncFix {
        private static final Object lock = new Object();
        private static boolean running = true;
        static boolean isRunning() { synchronized (lock) { return running; } }
        static void stop()          { synchronized (lock) { running = false; } }
        static void run() throws Exception {
            Thread worker = new Thread(() -> {
                long n = 0;
                while (isRunning()) n++;
                System.out.println("[Sync] worker stopped, n=" + n);
            });
            worker.setDaemon(true);
            worker.start();
            Thread.sleep(300);
            stop();
            worker.join(1000);
            System.out.println("[Sync] alive after stop = " + worker.isAlive());
        }
    }
    public static void main(String[] args) throws Exception {
        Broken.run();
        VolatileFix.run();
        SyncFix.run();
    }
}