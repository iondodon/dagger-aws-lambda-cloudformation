package io.awssample.repository;

import io.awssample.model.Product;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;
import java.util.Optional;

public final class DynamoProductRepository implements ProductRepository {

    private static final String PRODUCT_TABLE_NAME = System.getenv("PRODUCT_TABLE_NAME");

    private final DynamoDbClient dynamoDbClient;

    public DynamoProductRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Optional<Product> findById(String id) {
        GetItemResponse getItemResponse = dynamoDbClient.getItem(GetItemRequest.builder()
                .key(Map.of("PK", AttributeValue.builder().s(id).build()))
                .tableName(PRODUCT_TABLE_NAME)
                .build());

        if (getItemResponse.hasItem()) {
            return Optional.of(ProductMapper.fromDDB(getItemResponse.item()));
        }

        return Optional.empty();
    }

    @Override
    public void put(Product product) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(PRODUCT_TABLE_NAME)
                .item(ProductMapper.toDDB(product))
                .build());
    }
}
