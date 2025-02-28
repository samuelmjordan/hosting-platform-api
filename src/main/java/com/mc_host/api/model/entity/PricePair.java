package com.mc_host.api.model.entity;

public class PricePair {
    private final ContentPrice oldPrice;
    private final ContentPrice newPrice;
    
    public PricePair(ContentPrice oldPrice, ContentPrice newPrice) {
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
    }
    
    public ContentPrice getOldPrice() {
        return oldPrice;
    }
    
    public ContentPrice getNewPrice() {
        return newPrice;
    }    
}
