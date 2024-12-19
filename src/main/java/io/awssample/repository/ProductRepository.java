package io.awssample.repository;

import io.awssample.model.Product;

import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(String id);

    void put(Product product);

}
