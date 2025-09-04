package hari;

public class TwoThreadsJoin {

    public static void main(String[] args) throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            System.out.println("Thread 1 Start");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Thread 1 End");
        });
        Thread thread2 = new Thread(() -> {
            System.out.println("Thread 2 Start");
            try {
                Thread.sleep(12000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Thread 2 End");
        });
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        System.out.println("End of main");
    }
}
