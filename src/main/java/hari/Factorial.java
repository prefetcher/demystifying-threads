package hari;

import java.util.concurrent.*;
import java.util.stream.IntStream;

public class Factorial {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int n = 1000_000;
        long start = System.currentTimeMillis();
//        long sum = IntStream.rangeClosed(0, n)
//                .parallel()
//                .mapToLong(x ->(long)x).map(Factorial::factorial)
//                .sum();

//        CompletionService<Long> executor = new ExecutorCompletionService<>(Executors.newVirtualThreadPerTaskExecutor());
//        for(int i = 1; i <= n; i++) {
//            int finalI = i;
//            executor.submit(() -> factorial(finalI));
//        }
//        long sum = 0;
//        for(int i = 1; i <= n; i++) {
//            sum += executor.take().get();
//        }


        CompletableFuture<Long> cf = IntStream.rangeClosed(1, n).mapToObj(i -> CompletableFuture.supplyAsync(() -> factorial(i))).reduce(CompletableFuture.completedFuture(0L), (a, b) -> a.thenCombine(b, Long::sum));
        long sum = cf.get();
//

        long end = System.currentTimeMillis();
        System.out.println(end - start);
        System.out.println("Factorial of " + n + " is " + sum);
    }

    public static long factorial(long n) {
        long fact = 1;
        for (int i = 1; i <= n; i++) {
            fact = fact * i;
        }
        return fact;
    }
}
