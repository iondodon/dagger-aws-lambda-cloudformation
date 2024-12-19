package io.awssample;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import io.awssample.service.CreateProductService;
import io.awssample.service.CreateProductUseCase;
import io.awssample.repository.DynamoProductRepository;
import io.awssample.repository.ProductRepository;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.inject.Singleton;

@Module
public class ProductModule {

    @Provides
    @Singleton
    public static DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
//      .overrideConfiguration(ClientOverrideConfiguration.builder()
//        .addExecutionInterceptor(new TracingInterceptor())
//        .build())
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
    }

    @Provides
    @Singleton
    public static ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Provides
    @Singleton
    public static ProductRepository productRepository(DynamoDbClient dynamoDbClient) {
        return new DynamoProductRepository(dynamoDbClient);
    }

    @Provides
    @Singleton
    public static CreateProductUseCase createProductUseCase(ProductRepository productRepository) {
        return new CreateProductService(productRepository);
    }
}
