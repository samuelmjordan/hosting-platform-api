package com.mc_host.api.service.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Task {
    private static final Logger LOGGER = Logger.getLogger(Task.class.getName());

    public static CompletableFuture<Void> alwaysAttempt(String taskName, CheckedRunnable task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                task.run();
                return null;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to execute task: " + taskName, e);
                return null;
            }
        });
    }

    public static CompletableFuture<Void> criticalTask(String taskName, CheckedRunnable task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                task.run();
                return null;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to execute critical task: " + taskName, e);
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> whenAllComplete(
            CompletableFuture<Void>[] prerequisites,
            String taskName,
            CheckedRunnable task,
            boolean critical) {
        
        return CompletableFuture.allOf(prerequisites)
            .thenComposeAsync(ignored -> 
                critical ? criticalTask(taskName, task) : alwaysAttempt(taskName, task));
    }
    
    @SafeVarargs
    public static CompletableFuture<Void> whenAllCompleteCritical(
            String taskName,
            CheckedRunnable task,
            CompletableFuture<Void>... prerequisites) {
        
        return whenAllComplete(prerequisites, taskName, task, true);
    }
    
    @SafeVarargs
    public static CompletableFuture<Void> whenAllCompleteNonCritical(
            String taskName,
            CheckedRunnable task,
            CompletableFuture<Void>... prerequisites) {
        
        return whenAllComplete(prerequisites, taskName, task, false);
    }

    @SafeVarargs
    public static void awaitCompletion(CompletableFuture<Void>... tasks) {
        try {
            CompletableFuture.allOf(tasks).join();
        } catch (CompletionException e) {
            throw new RuntimeException("Resource cleanup failed", e.getCause());
        }
    }

    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }
}