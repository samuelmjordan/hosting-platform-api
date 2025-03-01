package com.mc_host.api.model.cache;

public enum CacheNamespace {
    API,

    QUEUE,
    QUEUE_RETRY,

    SUBSCRIPTION_SYNC_DEBOUNCE,
    SUBSCRIPTION_SYNC_IN_PROGRESS,
    SUBSCRIPTION_SYNC_RETRY,

    SPECIFICATION_PLANS,
    USER_CURRENCY
}
