package com.mc_host.api.util;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PersistenceContext {
    
    private final TransactionTemplate transactionTemplate;
    
    public PersistenceContext(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }
    
    public <T> T inTransaction(TransactionCallback<T> action) {
        return transactionTemplate.execute(action);
    }
    
    public void inTransaction(TransactionCallbackWithoutResult action) {
        transactionTemplate.execute(action);
    }
    
    public void inTransaction(ThrowingRunnable action) {
        transactionTemplate.execute(status -> {
            try {
                action.run();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException("Transaction failed", e);
                }
            }
            return null;
        });
    }
    
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}