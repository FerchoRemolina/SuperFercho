package com.superfercho.orders.application.dto;

public record ListAdminRecentBuyersCommand(int limit) {

    public static final int DEFAULT_LIMIT = 8;
    public static final int MAX_LIMIT = 20;

    public ListAdminRecentBuyersCommand {
        if (limit < 1) {
            limit = DEFAULT_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
    }

    public static ListAdminRecentBuyersCommand of(Integer limit) {
        return new ListAdminRecentBuyersCommand(limit == null ? DEFAULT_LIMIT : limit);
    }
}
