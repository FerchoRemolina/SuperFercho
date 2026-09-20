package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.ShippingAddressResult;

public record ShippingAddressRestResponse(
        String recipientName,
        String addressLine,
        String additionalInfo,
        String city,
        String department,
        String phone) {

    public static ShippingAddressRestResponse from(ShippingAddressResult address) {
        return new ShippingAddressRestResponse(
                address.recipientName(),
                address.addressLine(),
                address.additionalInfo(),
                address.city(),
                address.department(),
                address.phone());
    }
}
