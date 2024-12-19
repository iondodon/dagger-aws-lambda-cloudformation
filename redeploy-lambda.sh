#!/bin/bash

LAMBDA_FILE="${1:-product-lambda-0.0.1-SNAPSHOT.jar}"
LAMBDA_NAME="${2:-ProductSQSProcessor}"

 # Run Maven build
mvn clean package -DskipTests


echo "Redeploying Lambda"
aws lambda --profile localstack \
           --endpoint-url=http://localhost:4566 \
           update-function-code \
           --function-name "$LAMBDA_NAME" \
           --zip-file "fileb://target/$LAMBDA_FILE" | cat