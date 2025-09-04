package hari;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * This class demonstrates the use of CyclicBarrier to synchronize multiple threads
 * at a common point. This example highlights the "cyclic" nature, where the
 * barrier resets after being tripped and can be used again.
 */
public class CyclicBarrierExample {

    /**
     * A worker task that simulates performing work in multiple, synchronized cycles.
     */
    static class Worker implements Runnable {
        private final CyclicBarrier barrier;
        private final int numCycles;

        public Worker(CyclicBarrier barrier, int numCycles) {
            this.barrier = barrier;
            this.numCycles = numCycles;
        }

        @Override
        public void run() {
            try {
                // Loop for a number of cycles to demonstrate the barrier's reusability.
                for (int i = 1; i <= numCycles; i++) {
                    // --- Phase 1 ---
                    System.out.println(Thread.currentThread().getName() + " [Cycle " + i + "] is performing Part 1.");
                    Thread.sleep((long) (Math.random() * 2000 + 500)); // Simulate work

                    System.out.println(Thread.currentThread().getName() + " [Cycle " + i + "] finished Part 1, waiting at barrier.");

                    // The worker arrives at the barrier and waits for others.
                    // The barrier resets automatically after all threads have passed.
                    barrier.await();

                    // --- Phase 2 ---
                    // This code is executed only after all threads have reached the barrier for the current cycle.
                    System.out.println(Thread.currentThread().getName() + " [Cycle " + i + "] passed barrier, performing Part 2.");
                    Thread.sleep((long) (Math.random() * 1500 + 500)); // Simulate work
                }
                System.out.println(Thread.currentThread().getName() + " has finished all its cycles.");

            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " was interrupted.");
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                System.out.println("The barrier was broken. " + Thread.currentThread().getName() + " is stopping.");
            }
        }
    }

    public static void main(String[] args) {
        final int NUM_WORKERS = 3;
        final int NUM_CYCLES = 3;

        // This action runs each time the barrier is tripped.
        Runnable barrierAction = () -> {
            System.out.println("------------------------------------------------------------");
            System.out.println("BARRIER TRIPPED: All workers are synchronized. Moving to next phase.");
            System.out.println("------------------------------------------------------------");
        };

        // Create a CyclicBarrier that waits for 3 threads.
        CyclicBarrier barrier = new CyclicBarrier(NUM_WORKERS, barrierAction);

        System.out.println("Starting " + NUM_WORKERS + " workers for " + NUM_CYCLES + " cycles.");

        // Create and start the worker threads.
        for (int i = 0; i < NUM_WORKERS; i++) {
            new Thread(new Worker(barrier, NUM_CYCLES), "Worker-" + (i + 1)).start();
        }
    }
}