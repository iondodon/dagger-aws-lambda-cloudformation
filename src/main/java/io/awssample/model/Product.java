package io.awssample.model;

import java.math.BigDecimal;
import java.util.UUID;

import static java.util.Objects.isNull;

public record Product(String id, String name, BigDecimal price) {

    // this is more a DDD style of validating the object
    public Product {
        requireValidId(id);
        requireValidName(name);
        requireValidPrice(price);
    }

    private void requireValidId(String id) {
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Product ID should be a valid UUID value");
        }
    }

    private void requireValidName(String productName) {
        if (isNull(productName) || productName.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank");
        }

        if (productName.length() > 255) {
            throw new IllegalArgumentException("Product name cannot exceed 255 characters");
        }
    }

    private void requireValidPrice(BigDecimal price) {
        if (isNull(price)) {
            throw new NullPointerException("Price cannot be null");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
    }
}
