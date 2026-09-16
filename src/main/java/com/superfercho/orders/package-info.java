/**
 * Orders module. Domain, application, persistence, REST, and cross-module adapters
 * are implemented. Checkout and cancel run inside Infrastructure transaction wrappers.
 * Post-checkout status updates are exposed via REST. Eligible PENDING orders are
 * confirmed by a scheduled job after the customer cancellation window.
 */
package com.superfercho.orders;
