package io.awssample.service;

import io.awssample.model.Product;

import java.math.BigDecimal;

/**
 * Defines a use case for creating a new product.
 */
public interface CreateProductUseCase {

    /**
     * Creates a new product with the given name and price.
     *
     * @param productName the name of the product
     * @param price the price of the product
     * @return the newly created product
     * @throws NullPointerException if either the product name or price is null
     * @throws IllegalArgumentException if the price is negative or zero
     */
    Product create(String productName, BigDecimal price)
            throws NullPointerException, IllegalArgumentException;

}
