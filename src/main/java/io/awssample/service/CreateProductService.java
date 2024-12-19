package io.awssample.service;

import io.awssample.model.Product;
import io.awssample.repository.ProductRepository;

import java.math.BigDecimal;

import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;

public final class CreateProductService implements CreateProductUseCase {

    private final ProductRepository productRepository;

    public CreateProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product create(String productName, BigDecimal price) {
        // This could be removed and relay on Product constructor validations: DDD style
        requireValidName(productName);
        requireValidPrice(price);

        Product product = new Product(randomUUID().toString(), productName.trim(), price);
        productRepository.put(product);

        return product;
    }

    private String requireValidName(String productName) {
        if (isNull(productName) || productName.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank");
        }

        if (productName.length() > 255) {
            throw new IllegalArgumentException("Product name cannot exceed 255 characters");
        }

        return productName;
    }

    private BigDecimal requireValidPrice(BigDecimal price) {
        if (isNull(price)) {
            throw new NullPointerException("Price cannot be null");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }

        return price;
    }
}
