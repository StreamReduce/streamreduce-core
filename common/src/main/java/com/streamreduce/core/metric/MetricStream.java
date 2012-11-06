package com.streamreduce.core.metric;

/**
 * <p>Author: Nick Heudecker</p>
 * <p>Created: 8/28/12 12:17</p>
 */
public class MetricStream {

    private String accountId;
    private String connectionId;
    private String inventoryItemId;

    public MetricStream(String metricAccount, String metricConnection, String metricInventoryItem) {
        this.accountId = metricAccount;
        this.connectionId = metricConnection;
        this.inventoryItemId = metricInventoryItem;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(String inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }
}
