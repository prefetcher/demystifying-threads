package hari;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Let's break down the roles of Lock and Condition in the code you've selected from the Canvas.
 *
 * Think of it like a small, one-person-at-a-time workshop (the SharedBuffer).
 *
 * The Lock (The Key to the Workshop)
 * The Lock (specifically a ReentrantLock here) acts as the single key to the workshop door.
 *
 * Purpose: Its job is to ensure mutual exclusion, meaning only one thread (either the Producer or the Consumer) can be inside the workshop (accessing the queue) at any given moment. This prevents chaos, like two threads trying to add or remove items simultaneously, which could corrupt the data.
 *
 * How it's used:
 *
 * lock.lock();: A thread arrives at the door and locks it. If another thread is already inside, it must wait outside until the door is unlocked.
 *
 * lock.unlock();: When the thread is finished with its work, it unlocks the door, allowing another waiting thread to enter. This is always done in a finally block to guarantee the lock is released, even if an error occurs.
 *
 * The Condition (The Waiting Areas)
 * Condition objects are like designated waiting areas inside the workshop for specific problems. Simply having the key isn't enough; sometimes a thread gets inside but realizes it can't do its job yet.
 *
 * Purpose: Its job is to provide a way for threads to pause and wait for a specific state to change, without holding up everyone else.
 *
 * How it's used:
 *
 * notFull: This is the waiting area for the Producer. If the producer enters the workshop and finds the buffer is full, it can't add a new item. Instead of just standing there holding the key (which would block the consumer from ever entering), it goes to the notFull waiting area by calling notFull.await().
 *
 * notEmpty: This is the waiting area for the Consumer. If the consumer enters and finds the buffer empty, it goes to the notEmpty waiting area by calling notEmpty.await().
 *
 * The Magic of await() and signal()
 * These two methods are how the waiting areas work:
 *
 * await(): When a thread calls await(), two crucial things happen:
 *
 * It releases the lock (hands the key back).
 *
 * It goes to sleep in its designated waiting area.
 * This is the most important part: by releasing the lock, it allows another thread to enter the workshop to change the situation (e.g., the consumer can now enter to take an item).
 *
 * signal(): This is the wake-up call.
 *
 * When the consumer takes an item, it makes the buffer "not full." It then calls notFull.signal() to wake up a single thread sleeping in the notFull waiting area.
 *
 * When the producer adds an item, it makes the buffer "not empty." It then calls notEmpty.signal() to wake up a single thread sleeping in the notEmpty waiting area.
 *
 * In summary, the Lock ensures only one thread can work at a time, while the Condition objects provide an efficient way for threads to pause and wait for the right circumstances without causing a deadlock.
 */

/**
 * It gets to the heart of how Condition works.
 *
 * You are correct with your second guess. When notEmpty.signal() is called by the producer:
 *
 * The consumer thread wakes up. It moves from the "waiting" state to a "runnable" state.
 *
 * However, it cannot proceed immediately. It must wait to re-acquire the lock.
 *
 * The producer thread, which called signal(), still holds the lock. The consumer can only resume its work after the producer executes its finally block and calls lock.unlock().
 *
 * So, the sequence is:
 *
 * Producer (holding the lock) calls notEmpty.signal().
 *
 * Consumer wakes up and is now ready to run, but it's blocked, waiting for the lock to become available.
 *
 * Producer finishes its put method and releases the lock via lock.unlock().
 *
 * Consumer can now acquire the lock and continue execution from right after its notEmpty.await() call.
 */
public class ConditionAwaitSignal {
    /**
     * A thread-safe buffer that holds items for producers and consumers.
     */
    static class SharedBuffer {
        private final Queue<Integer> queue = new LinkedList<>();
        private final int capacity;
        private final Lock lock = new ReentrantLock();

        // Condition for waiting when the buffer is full (for producers).
        private final Condition notFull = lock.newCondition();
        // Condition for waiting when the buffer is empty (for consumers).
        private final Condition notEmpty = lock.newCondition();

        public SharedBuffer(int capacity) {
            this.capacity = capacity;
        }

        /**
         * Adds an item to the buffer. If the buffer is full, the calling thread will wait.
         *
         * @param item The integer item to add.
         * @throws InterruptedException if the thread is interrupted while waiting.
         */
        public void put(int item) throws InterruptedException {
            lock.lock(); // Acquire the lock
            try {
                // While the buffer is full, wait for a signal that it's not full anymore.
                while (queue.size() == capacity) {
                    System.out.println(Thread.currentThread().getName() + " | Buffer is full, waiting...");
                    notFull.await(); // Releases the lock and waits.
                }

                queue.add(item);
                System.out.println(Thread.currentThread().getName() + " | Produced: " + item);

                // Signal to one waiting consumer thread that the buffer is no longer empty.
                notEmpty.signal();
            } finally {
                lock.unlock(); // Always release the lock in a finally block.
            }
        }

        /**
         * Takes an item from the buffer. If the buffer is empty, the calling thread will wait.
         *
         * @return The integer item taken from the buffer.
         * @throws InterruptedException if the thread is interrupted while waiting.
         */
        public int get() throws InterruptedException {
            lock.lock(); // Acquire the lock
            try {
                // While the buffer is empty, wait for a signal that it's not empty anymore.
                while (queue.isEmpty()) {
                    System.out.println(Thread.currentThread().getName() + " | Buffer is empty, waiting...");
                    notEmpty.await(); // Releases the lock and waits.
                }

                int item = queue.poll();
                System.out.println(Thread.currentThread().getName() + " | Consumed: " + item);

                // Signal to one waiting producer thread that the buffer is no longer full.
                notFull.signal();
                return item;
            } finally {
                lock.unlock(); // Always release the lock.
            }
        }
    }

    public static void main(String[] args) {
        SharedBuffer buffer = new SharedBuffer(5); // A buffer with a capacity of 5.

        // Create and start a producer thread.
        Thread producerThread = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    buffer.put(i);
                    Thread.sleep(100); // Simulate time taken to produce an item.
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        // Create and start a consumer thread.
        Thread consumerThread = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    buffer.get();
                    Thread.sleep(250); // Simulate time taken to consume an item.
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        producerThread.start();
        consumerThread.start();
    }
}

/**
 Using await() inside a while loop is a critical and mandatory pattern in concurrent programming.

 Here’s why, using the code in the condition_example_java document as a reference:

 The Problem: Spurious Wakeups
 A thread can, on rare occasions, wake up from an await() call without having been signal()-ed by another thread. This is known as a spurious wakeup. It's a low-level, unpredictable event that can happen for various reasons related to the operating system's thread scheduler.

 How the while Loop Protects You
 The while loop acts as a safety net. Let's look at the producer's code:

 Java

 // While the buffer is full, wait...
 while (queue.size() == capacity) {
 notFull.await();
 }
 // ...proceed to add to the queue
 Here's the sequence of events:

 Check: The thread checks the condition: is the queue full?

 Wait: If it's full, the thread calls await() and goes to sleep.

 Wake Up: The thread wakes up for one of two reasons:

 Legitimate Signal: The consumer called notFull.signal().

 Spurious Wakeup: The thread woke up for no reason.

 Re-Check: Because it's in a while loop, the thread immediately goes back to step 1 and re-checks the condition.

 If it was a spurious wakeup, the queue is still full. The condition queue.size() == capacity is still true, and the thread correctly goes back to sleep by calling await() again.

 Why an if Statement Is Dangerous
 If you were to use an if statement instead:

 Java

 // DANGEROUS CODE - DO NOT USE
 if (queue.size() == capacity) {
 notFull.await();
 }
 // This line would be executed incorrectly after a spurious wakeup
 queue.add(item);
 In the case of a spurious wakeup, the thread would wake up, exit the if block, and incorrectly try to add an item to an already full queue, breaking the logic of your program.

 In short, the while loop ensures that the thread only proceeds when the condition it was waiting for is actually true.

 */