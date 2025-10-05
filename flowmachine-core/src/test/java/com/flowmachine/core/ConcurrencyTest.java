package com.flowmachine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.model.StateMachineInfo;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ConcurrencyTest {

  enum State {CREATED, PROCESSING, COMPLETED, FAILED}

  enum Event {START_PROCESSING, COMPLETE, FAIL}

  static class ProcessingContext {

    private final String id;
    private final AtomicInteger processCount = new AtomicInteger(0);
    private volatile long processingTime = 0;

    ProcessingContext(String id) {
      this.id = id;
    }

    void incrementProcessCount() {
      processCount.incrementAndGet();
    }

    int getProcessCount() {
      return processCount.get();
    }

    String getId() {
      return id;
    }

    void setProcessingTime(long time) {
      this.processingTime = time;
    }

    long getProcessingTime() {
      return processingTime;
    }
  }

  @Test
  void shouldHandleThousandsOfConcurrentRequests() throws InterruptedException, ExecutionException {
    StateMachine<State, Event, ProcessingContext> machine = FlowMachine
        .<State, Event, ProcessingContext>builder()
        .initialState(State.CREATED)
        .configure(State.CREATED)
        .permit(Event.START_PROCESSING, State.PROCESSING)
        .and()
        .configure(State.PROCESSING)
        .permit(Event.COMPLETE, State.COMPLETED)
        .permit(Event.FAIL, State.FAILED)
        .onEntry((t, ctx) -> {
          ctx.incrementProcessCount();
          ctx.setProcessingTime(System.nanoTime());
        })
        .and()
        .configure(State.COMPLETED)
        .and()
        .configure(State.FAILED)
        .and()
        .build();

    int numberOfThreads = 50;
    int numberOfContextsPerThread = 100;
    int totalContexts = numberOfThreads * numberOfContextsPerThread;

    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(totalContexts);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);
    AtomicLong totalProcessingTime = new AtomicLong(0);

    try {
      @SuppressWarnings("unchecked")
      CompletableFuture<Void>[] futures = new CompletableFuture[totalContexts];

      for (int i = 0; i < totalContexts; i++) {
        final int contextId = i;
        futures[i] = CompletableFuture.runAsync(() -> {
          try {
            ProcessingContext context = new ProcessingContext("context-" + contextId);

            State currentState = State.CREATED;
            currentState = machine.fire(currentState, Event.START_PROCESSING, context);

            assertEquals(State.PROCESSING, currentState);
            assertEquals(1, context.getProcessCount());

            if (contextId % 10 == 0) {
              currentState = machine.fire(currentState, Event.FAIL, context);
              assertEquals(State.FAILED, currentState);
              failureCount.incrementAndGet();
            } else {
              currentState = machine.fire(currentState, Event.COMPLETE, context);
              assertEquals(State.COMPLETED, currentState);
              successCount.incrementAndGet();
            }

            totalProcessingTime.addAndGet(System.nanoTime() - context.getProcessingTime());

          } catch (Exception e) {
            e.printStackTrace();
            fail("Concurrent execution failed: " + e.getMessage());
          } finally {
            latch.countDown();
          }
        }, executor);
      }

      assertTrue(latch.await(100, TimeUnit.MILLISECONDS), "All tasks should complete within 100 milliseconds");

      try {
        CompletableFuture.allOf(futures).get(1, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
        fail("Tasks did not complete within timeout: " + e.getMessage());
      }

      assertEquals(totalContexts, successCount.get() + failureCount.get());
      assertTrue(successCount.get() > 0, "Should have some successful transitions");
      assertTrue(failureCount.get() > 0, "Should have some failed transitions");

      System.out.printf("Processed %d contexts: %d successes, %d failures%n",
          totalContexts, successCount.get(), failureCount.get());
      System.out.printf("Average processing time: %.2f microseconds%n",
          totalProcessingTime.get() / (double) totalContexts / 1000);

    } finally {
      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void shouldMaintainThreadSafetyWithSharedStateMachine() throws InterruptedException {
    StateMachine<State, Event, ProcessingContext> machine = FlowMachine
        .<State, Event, ProcessingContext>builder()
        .initialState(State.CREATED)
        .configure(State.CREATED)
        .permitIf(Event.START_PROCESSING, State.PROCESSING,
            (t, ctx) -> ctx.getId().hashCode() % 2 == 0)
        .permitIf(Event.FAIL, State.FAILED,
            (t, ctx) -> ctx.getId().hashCode() % 2 != 0)
        .and()
        .configure(State.PROCESSING)
        .permit(Event.COMPLETE, State.COMPLETED)
        .and()
        .configure(State.COMPLETED)
        .and()
        .configure(State.FAILED)
        .and()
        .build();

    int numberOfThreads = 20;
    int operationsPerThread = 1000;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    AtomicInteger totalOperations = new AtomicInteger(0);

    for (int threadId = 0; threadId < numberOfThreads; threadId++) {
      final int tId = threadId;
      executor.submit(() -> {
        try {
          for (int i = 0; i < operationsPerThread; i++) {
            ProcessingContext context = new ProcessingContext("thread-" + tId + "-ctx-" + i);

            State result;
            if (context.getId().hashCode() % 2 == 0) {
              result = machine.fire(State.CREATED, Event.START_PROCESSING, context);
              assertEquals(State.PROCESSING, result);

              result = machine.fire(result, Event.COMPLETE, context);
              assertEquals(State.COMPLETED, result);
            } else {
              result = machine.fire(State.CREATED, Event.FAIL, context);
              assertEquals(State.FAILED, result);
            }

            totalOperations.incrementAndGet();
          }
        } catch (Exception e) {
          e.printStackTrace();
          fail("Thread safety test failed: " + e.getMessage());
        } finally {
          latch.countDown();
        }
      });
    }

    assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete");
    assertEquals(numberOfThreads * operationsPerThread, totalOperations.get());

    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
  }

  @Test
  void shouldPerformWellUnderLoad() throws InterruptedException {
    StateMachine<State, Event, ProcessingContext> machine = FlowMachine
        .<State, Event, ProcessingContext>builder()
        .initialState(State.CREATED)
        .configure(State.CREATED)
        .permit(Event.START_PROCESSING, State.PROCESSING)
        .and()
        .configure(State.PROCESSING)
        .permit(Event.COMPLETE, State.COMPLETED)
        .and()
        .configure(State.COMPLETED)
        .and()
        .build();

    int warmupIterations = 1000;
    for (int i = 0; i < warmupIterations; i++) {
      ProcessingContext context = new ProcessingContext("warmup-" + i);
      State state = machine.fire(State.CREATED, Event.START_PROCESSING, context);
      machine.fire(state, Event.COMPLETE, context);
    }

    int testIterations = 100_000;
    long startTime = System.nanoTime();

    IntStream.range(0, testIterations).parallel().forEach(i -> {
      ProcessingContext context = new ProcessingContext("perf-" + i);
      State state = machine.fire(State.CREATED, Event.START_PROCESSING, context);
      machine.fire(state, Event.COMPLETE, context);
    });

    long endTime = System.nanoTime();
    long totalTime = endTime - startTime;
    double averageTimePerOperation = totalTime / (double) testIterations;

    System.out.printf("Performance test: %d operations in %.2f ms%n",
        testIterations, totalTime / 1_000_000.0);
    System.out.printf("Average time per operation: %.2f nanoseconds%n", averageTimePerOperation);

    assertTrue(averageTimePerOperation < 10_000, "Operations should complete in under 10 microseconds on average");
  }

  @Test
  void shouldValidateImmutabilityAfterBuild() {
    StateMachine<State, Event, ProcessingContext> machine = FlowMachine
        .<State, Event, ProcessingContext>builder()
        .initialState(State.CREATED)
        .configure(State.CREATED)
        .permit(Event.START_PROCESSING, State.PROCESSING)
        .and()
        .configure(State.PROCESSING)
        .permit(Event.COMPLETE, State.COMPLETED)
        .and()
        .configure(State.COMPLETED)
        .and()
        .build();

    StateMachineInfo<State, Event, ProcessingContext> info1 = machine.getInfo();
    StateMachineInfo<State, Event, ProcessingContext> info2 = machine.getInfo();

    assertEquals(info1.initialState(), info2.initialState());
    assertEquals(info1.states(), info2.states());
    assertEquals(info1.events(), info2.events());
    assertEquals(info1.transitions(), info2.transitions());

    ProcessingContext context = new ProcessingContext("test");
    machine.fire(State.CREATED, Event.START_PROCESSING, context);

    StateMachineInfo<State, Event, ProcessingContext> info3 = machine.getInfo();
    assertEquals(info1.states(), info3.states());
    assertEquals(info1.events(), info3.events());
  }
}