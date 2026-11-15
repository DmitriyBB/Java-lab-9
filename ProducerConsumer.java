import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ProducerConsumer {

    record Request(int id) {}

    static final Request POISON = new Request(-1);

    static final AtomicInteger processed = new AtomicInteger();

    static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, prefix + "-" + n.incrementAndGet());
            t.setDaemon(false);
            return t;
        };
    }

    static ExecutorService newPool(int size, String prefix) {
        return Executors.newFixedThreadPool(size, named(prefix));
    }

    static void shutdownGracefully(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Пул не завершился");
                }
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    static void process(Request r) {
        processed.incrementAndGet();
        // имитация работы
        long s = 0; for (int i = 0; i < 1000; i++) s += i;
    }

    public static void main(String[] args) {
        final int capacity = 100;
        final int producers = 4;
        final int consumers = 4;
        final int perProducer = 10_000;

        BlockingQueue<Request> queue = new ArrayBlockingQueue<>(capacity);

        ExecutorService producerPool = newPool(producers, "producer");
        ExecutorService consumerPool = newPool(consumers, "consumer");

        // producers
        for (int p = 0; p < producers; p++) {
            final int base = p * perProducer;
            producerPool.submit(() -> {
                try {
                    for (int i = 0; i < perProducer; i++) {
                        queue.put(new Request(base + i)); // блокируется при заполнении — backpressure
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        producerPool.shutdown();

        // consumers
        for (int c = 0; c < consumers; c++) {
            consumerPool.submit(() -> {
                try {
                    while (true) {
                        Request r = queue.take();               // блокирующее ожидание, без busy wait
                        if (r == POISON) {
                            queue.put(POISON);                  // вернуть маркер остальным
                            return;
                        }
                        process(r);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Дождаться producers и разослать poison
        try {
            if (!producerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                producerPool.shutdownNow();
            }
            // по одному poison на каждого consumer'а
            for (int c = 0; c < consumers; c++) queue.put(POISON);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        shutdownGracefully(consumerPool);
        System.out.println("Обработано заявок: " + processed.get()
                + " (ожидалось " + (long) producers * perProducer + ")");
    }
}