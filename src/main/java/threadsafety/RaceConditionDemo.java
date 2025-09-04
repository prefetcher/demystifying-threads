package threadsafety;

import java.time.Duration;
import java.time.Instant;

public class RaceConditionDemo {

    private int counter = 0;

    public static void main(String[] args) {
        RaceConditionDemo demo = new RaceConditionDemo();
        demo.runTest();
    }

    private void runTest() {
        Thread thread1 = Thread.ofPlatform().start(this::incrementCounter);
        Thread thread2 = Thread.ofPlatform().start(this::incrementCounter);

        Instant start = Instant.now();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Threads interrupted");
        }
        Instant end = Instant.now();
        System.out.println("Total time: " + Duration.between(start, end).toMillis());

        System.out.println("Final counter value: " + counter);
    }

    private void incrementCounter() {
        Instant start = Instant.now();
        synchronized (this) {
            for (int i = 0; i < 100000; i++) {

                counter++;

            }
        }
        Instant end = Instant.now();
        System.out.println(Duration.between(start, end).toMillis());
    }
}
