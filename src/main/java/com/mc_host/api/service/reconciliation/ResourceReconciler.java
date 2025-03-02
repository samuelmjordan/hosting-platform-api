package com.mc_host.api.service.reconciliation;

import java.util.stream.Stream;

public interface ResourceReconciler<T> {

    Stream<T> fetchActualResources();
    Stream<T> fetchDatabaseResources();
    

}
