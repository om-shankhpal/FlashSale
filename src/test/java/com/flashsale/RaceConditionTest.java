package com.flashsale;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

public class RaceConditionTest {

    public static void main(String[] args) throws Exception {
        long productId = 1L; // Replace with your product ID
        int requests = 50;

        HttpClient client = HttpClient.newHttpClient();
        ExecutorService executor =
                Executors.newFixedThreadPool(requests);
        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < requests; i++) {
            executor.submit(() -> {
                ready.countDown();

                try {
                    start.await();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(
                                    "http://localhost:8080/api/purchases/"
                                            + productId))
                            .header("Idempotency-Key", "race-demo-002")
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();

                    HttpResponse<String> response = client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString());

                    System.out.println(response.statusCode()
                            + " - " + response.body());

                } catch (Exception e) {
                    System.out.println("Request failed: "
                            + e.getMessage());
                }
            });
        }

        ready.await();
        start.countDown();

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);
    }
}