package io.awssample.handler;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

@Tag("component")
@Testcontainers
// FIXME: TO BE FIXED, if possible???
class SQSProductHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQSProductHandlerTest.class);

    private static final String TABLE_NAME = "product";
    private static final String REGION = "eu-west-1";
    private static final String QUEUE_NAME = "product-sqs-queue";
    private static final String LAMBDA_NAME = "ProductLambda";

    @Container
    public static final LocalStackContainer localstack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.0.3"))
                    .withEnv("PRODUCT_TABLE_NAME", TABLE_NAME) // FIXME: this is skipped
                    .withEnv("DEBUG", "true")
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                    .withServices(SQS, LAMBDA, DYNAMODB, IAM, CLOUDWATCHLOGS, LocalStackContainer.EnabledService.named("events"));


    @Test
    void e2e() throws Exception {

        execInContainer("aws", "configure", "set", "aws_access_key_id", "key");
        execInContainer("aws", "configure", "set", "aws_secret_access_key", "secret");
        execInContainer("aws", "configure", "set", "region", REGION);

        try (// Create clients
             SqsClient sqsClient = buildSQSClient();
             DynamoDbClient dynamoDbClient = buildDDBClient();
             LambdaClient lambdaClient = buildLambdaClient()) {

            // Setup resources
            CreateQueueResponse queueResponse = sqsClient.createQueue(CreateQueueRequest.builder()
                    .queueName(QUEUE_NAME)
                    .build());

            createDynamoDBTable(dynamoDbClient);
            deployLambda(lambdaClient);

            // Trigger Lambda via SQS
            var payload = """
                    {
                        "name": "Test Product",
                        "price": 20.5
                    }
                    """;

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueResponse.queueUrl())
                    .messageBody(payload)
                    .build());

            // assert result
            var productTableResult = new ArrayList<Map<String, AttributeValue>>();
            Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .with()
                    .pollInterval(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        ScanResponse scanResponse = dynamoDbClient.scan(ScanRequest.builder()
                                .tableName("product")
                                .limit(1)
                                .build());
                        productTableResult.addAll(scanResponse.items());
                        assertThat(productTableResult).hasSize(1);
                    });

            assertThat(productTableResult)
                    .hasSize(1)
                    .extracting(
                            item -> item.get("name").s(),
                            item -> new BigDecimal(item.get("price").n())
                    )
                    .containsExactly(tuple("Test Product", BigDecimal.valueOf(20.5D)));
        }
    }

    private static LambdaClient buildLambdaClient() {
        return LambdaClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LAMBDA))
                .credentialsProvider(credentialsProvider())
                .region(Region.of(REGION))
                .build();
    }

    private static @NotNull StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
    }

    private static DynamoDbClient buildDDBClient() {
        return DynamoDbClient.builder()
                .endpointOverride(localstack.getEndpointOverride(DYNAMODB))
                .credentialsProvider(credentialsProvider())
                .region(Region.of(REGION))
                .build();
    }

    private static SqsClient buildSQSClient() {
        return SqsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(SQS))
                .credentialsProvider(credentialsProvider())
                .region(Region.of(REGION))
                .build();
    }

    private static void execInContainer(String... cmd) throws IOException, InterruptedException {
        org.testcontainers.containers.Container.ExecResult result = localstack.execInContainer(cmd);
        assertThat(result.getExitCode())
                .overridingErrorMessage("\n%s\n%s".formatted(result.getStdout(), result.getStderr()))
                .isZero();
    }

    private void createDynamoDBTable(DynamoDbClient dynamoDbClient) {
        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder()
                        .attributeName("id")
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("id")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(10L)
                        .build())
                .build());
    }

    private void deployLambda(LambdaClient lambdaClient) throws Exception {
        // Package Lambda code into a ZIP file (assumes you have a compiled JAR)
        byte[] lambdaCode = Files.readAllBytes(Paths.get("target/product-lambda-0.0.1-SNAPSHOT.jar"));

        // Create Lambda function
        lambdaClient.createFunction(CreateFunctionRequest.builder()
                .functionName(LAMBDA_NAME)
                .runtime(Runtime.JAVA17)
                .role("arn:aws:iam::000000000000:role/lambda-role") // Mock role for LocalStack
                .handler("io.awssample.handler.SQSProductHandler::handleRequest") // Your handler class
                .code(FunctionCode.builder().zipFile(SdkBytes.fromByteArray(lambdaCode)).build())
                .build());

        // Create an Event Source Mapping to link the SQS queue to the Lambda function
        lambdaClient.createEventSourceMapping(CreateEventSourceMappingRequest.builder()
                .eventSourceArn("arn:aws:sqs:%s:000000000000:%s".formatted(REGION, QUEUE_NAME))
                .functionName(LAMBDA_NAME)
                .batchSize(1)
                .enabled(true)
                .build());
    }
}
