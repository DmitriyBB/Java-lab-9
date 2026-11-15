import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class RaceTest {
    private static final int THREADS = 8;
    private static final int PER_THREAD = 100_000;
    private ExecutorService pool;
    @BeforeEach
    void setUp() {
        pool = Executors.newFixedThreadPool(THREADS, r -> {
            Thread t = new Thread(r, "race-worker");
            t.setDaemon(true);
            return t;
        });
    }
    @AfterEach
    void tearDown() {
        pool.shutdownNow();
        try {
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    @Test
    @Timeout(5)
    void lostUpdateIsDetected() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CyclicBarrier start = new CyclicBarrier(THREADS);
        CountDownLatch done = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int k = 0; k < PER_THREAD; k++) {
                        counter.set(counter.get() + 1);
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(done.await(4, TimeUnit.SECONDS), "потоки не завершились вовремя");
        int got = counter.get();
        assertTrue(got < THREADS * PER_THREAD,
                "ожидалась потеря инкрементов, но получено " + got);
    }
    @Test
    @Timeout(5)
    void synchronizedCounterIsCorrect() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CyclicBarrier start = new CyclicBarrier(THREADS);
        CountDownLatch done = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int k = 0; k < PER_THREAD; k++) counter.incrementAndGet();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(done.await(4, TimeUnit.SECONDS));
        assertEquals(THREADS * PER_THREAD, counter.get());
    }
}