package io.awssample.repository;

import io.awssample.model.Product;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.util.Map;

final class ProductMapper {

    private static final String PK = "id";
    private static final String NAME = "name";
    private static final String PRICE = "price";

    static Product fromDDB(Map<String, AttributeValue> items) {
        return new Product(items.get(PK).s(), items.get(NAME).s(), new BigDecimal(items.get(PRICE).n()));
    }

    static Map<String, AttributeValue> toDDB(Product product) {
        return Map.of(
                PK, AttributeValue.builder().s(product.id()).build(),
                NAME, AttributeValue.builder().s(product.name()).build(),
                PRICE, AttributeValue.builder().n(product.price().toString()).build()
        );
    }
}
