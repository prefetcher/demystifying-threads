package hari;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class MyThreadPoolExecutor {

    private BlockingQueue<Runnable> taskQueue;
    private List<Worker> workers;
    private boolean shutdown;

    public MyThreadPoolExecutor(int size) {
        if(size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        workers = new ArrayList<>(size);
        taskQueue = new LinkedBlockingQueue<>();
        shutdown=false;
        for(int i = 0; i < size; i++) {
            Worker worker = new Worker("Worker-" + (i + 1));
            workers.add(worker);
            worker.start();
        }
    }

    public void execute(Runnable command) {
        if(shutdown) {
            throw new IllegalStateException("Executor is shutdown");
        }
        try {
            taskQueue.put(command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        shutdown = true;
        for(Worker worker : workers) {
            if(!worker.isAlive())
                worker.interrupt();
        }

    }

    private class Worker extends Thread {

        public Worker(String name) {
            super(name);
        }

        @Override
        public void run() {
            while(!shutdown || !taskQueue.isEmpty()) {
                Runnable task;
                try {
                    task = taskQueue.take();
                } catch (InterruptedException e) {
                    continue;
                }
                try{
//                    System.out.println(getName() + ": executing task " + task);
                    task.run();
                } catch (RuntimeException e) {
                    System.out.println("Error executing task " + e);
                }
            }
        }
    }

    public static void main(String[] args) {
        MyThreadPoolExecutor executor = new MyThreadPoolExecutor(3);
        for(int i = 0; i < 10; i++) {
            int task = i+1;
            executor.execute(()->{
                System.out.println("Task: " + task + " being executed by " + Thread.currentThread().getName() + " at " + Instant.now());
                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Task: " + task + " completed by " + Thread.currentThread().getName()+ " at " + Instant.now());
            });
        }
        System.out.println("All tasks executed. Shutting down.");
        Runtime.getRuntime().availableProcessors();
        executor.shutdown();

    }
}


