package hari;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of a lock-free, thread-safe queue based on the Michael-Scott non-blocking algorithm.
 * This queue is unbounded.
 *
 * @param <E> the type of elements held in this queue
 */
public class LockFreeQueue<E> {

    /**
     * The Node class represents an element in the linked list that backs the queue.
     * The 'next' pointer is an AtomicReference to allow for lock-free updates.
     */
    private static class Node<E> {
        final E item;
        final AtomicReference<Node<E>> next;

        Node(E item) {
            this.item = item;
            this.next = new AtomicReference<>(null);
        }
    }

    // head and tail are AtomicReferences to allow CAS operations.
    // They point to Node<E> objects.
    private final AtomicReference<Node<E>> head;
    private final AtomicReference<Node<E>> tail;

    /**
     * Constructs a new LockFreeQueue.
     * A sentinel (dummy) node is created, and both head and tail point to it.
     */
    public LockFreeQueue() {
        // Create a sentinel node. This simplifies enqueue/dequeue logic.
        Node<E> sentinel = new Node<>(null);
        this.head = new AtomicReference<>(sentinel);
        this.tail = new AtomicReference<>(sentinel);
    }

    /**
     * Adds an item to the tail of the queue.
     *
     * @param item The item to add.
     */
    public void enqueue(E item) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        Node<E> newNode = new Node<>(item);

        // Loop until the new node is successfully added.
        while (true) {
            Node<E> currentTail = tail.get();
            Node<E> nextNode = currentTail.next.get();

            // Check if the tail is pointing to the last node.
            if (currentTail == tail.get()) {
                if (nextNode == null) {
                    // This is the normal case: tail is the last node.
                    // Try to link the new node at the end.
                    if (currentTail.next.compareAndSet(null, newNode)) {
                        // Success! Now try to swing the tail pointer to the new node.
                        // This is an optimization; even if it fails, another thread will fix it.
                        tail.compareAndSet(currentTail, newNode);
                        return;
                    }
                } else {
                    // Another thread has already added a node but hasn't updated the tail yet.
                    // Help that thread by moving the tail pointer forward.
                    tail.compareAndSet(currentTail, nextNode);
                }
            }
        }
    }

    /**
     * Removes and returns the item at the head of the queue.
     *
     * @return The item at the head, or null if the queue is empty.
     */
    public E dequeue() {
        while (true) {
            Node<E> currentHead = head.get();
            Node<E> currentTail = tail.get();
            Node<E> firstNode = currentHead.next.get(); // The first *actual* item node

            if (currentHead == head.get()) { // Ensure head hasn't changed
                if (currentHead == currentTail) {
                    // Queue is empty or in an intermediate state.
                    if (firstNode == null) {
                        return null; // Queue is definitely empty.
                    }
                    // Another thread is mid-enqueue. Help it update the tail.
                    tail.compareAndSet(currentTail, firstNode);
                } else {
                    // The queue is not empty. Try to dequeue.
                    E item = firstNode.item;
                    // Try to swing the head pointer to the first node, making it the new sentinel.
                    if (head.compareAndSet(currentHead, firstNode)) {
                        // Success!
                        currentHead.next.set(null); // Help GC
                        return item;
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Lock-Free Queue stress test...");
        final LockFreeQueue<Integer> queue = new LockFreeQueue<>();
        final int numProducers = 4;
        final int numConsumers = 4;
        final int itemsPerProducer = 100_000;
        final ExecutorService executor = Executors.newFixedThreadPool(numProducers + numConsumers);

        // A thread-safe map to verify that every produced item is consumed exactly once.
        final ConcurrentHashMap<Integer, Boolean> consumedItems = new ConcurrentHashMap<>();
        final AtomicInteger producedCount = new AtomicInteger(0);

        // Start producer threads
        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            executor.submit(() -> {
                for (int j = 0; j < itemsPerProducer; j++) {
                    int item = producerId * itemsPerProducer + j;
                    queue.enqueue(item);
                    producedCount.incrementAndGet();
                }
            });
        }

        // Start consumer threads
        for (int i = 0; i < numConsumers; i++) {
            executor.submit(() -> {
                while (producedCount.get() < numProducers * itemsPerProducer || queue.dequeue() != null) {
                    Integer item = queue.dequeue();
                    if (item != null) {
                        // Mark the item as consumed. If it was already there, it's a duplicate.
                        if (consumedItems.put(item, true) != null) {
                            System.err.println("ERROR: Consumed duplicate item: " + item);
                        }
                    }
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(1, TimeUnit.MINUTES);

        if (!finished) {
            System.err.println("ERROR: Test timed out.");
            executor.shutdownNow();
        }

        System.out.println("Test finished.");
        System.out.println("Total items produced: " + (numProducers * itemsPerProducer));
        System.out.println("Total items consumed: " + consumedItems.size());

        if (consumedItems.size() == numProducers * itemsPerProducer) {
            System.out.println("SUCCESS: All produced items were consumed exactly once.");
        } else {
            System.err.println("FAILURE: Mismatch between produced and consumed items.");
        }

        if (queue.dequeue() == null) {
            System.out.println("SUCCESS: Queue is empty after consumption.");
        } else {
            System.err.println("FAILURE: Queue is not empty after consumption.");
        }
    }
}

