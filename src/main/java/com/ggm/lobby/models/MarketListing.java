package com.ggm.lobby.models;

import com.ggm.lobby.managers.MarketManager.MarketCategory;
import java.time.LocalDateTime;
import java.util.UUID;

public class MarketListing {
    private final String listingId;
    private final UUID sellerId;
    private final String sellerName;
    private final String itemData;
    private final long price;
    private final MarketCategory category;
    private final LocalDateTime listedTime;
    private final LocalDateTime expiryTime;
    private final boolean sold;
    private final UUID buyerId;
    private final String buyerName;
    private final LocalDateTime soldTime;

    public MarketListing(String listingId, UUID sellerId, String sellerName, String itemData,
                         long price, MarketCategory category, LocalDateTime listedTime,
                         LocalDateTime expiryTime, boolean sold, UUID buyerId, String buyerName,
                         LocalDateTime soldTime) {
        this.listingId = listingId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemData = itemData;
        this.price = price;
        this.category = category;
        this.listedTime = listedTime;
        this.expiryTime = expiryTime;
        this.sold = sold;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.soldTime = soldTime;
    }

    // Getters
    public String getListingId() { return listingId; }
    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public String getItemData() { return itemData; }
    public long getPrice() { return price; }
    public MarketCategory getCategory() { return category; }
    public LocalDateTime getListedTime() { return listedTime; }
    public LocalDateTime getExpiryTime() { return expiryTime; }
    public boolean isSold() { return sold; }
    public UUID getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public LocalDateTime getSoldTime() { return soldTime; }
}
