#!/bin/bash

# Configure AWS LocalStack profile
PROFILE_NAME="localstack"

# Function to check if AWS profile exists
profile_exists() {
  aws configure list-profiles | grep -q "^$PROFILE_NAME$"
}

echo "Checking if AWS LocalStack profile '$PROFILE_NAME' exists..."

if profile_exists; then
  echo "AWS LocalStack profile '$PROFILE_NAME' already exists. Skipping configuration."
else
  echo "Configuring AWS LocalStack profile..."

  # Set AWS credentials for LocalStack profile
  aws configure set aws_access_key_id "key" --profile $PROFILE_NAME
  aws configure set aws_secret_access_key "secret" --profile $PROFILE_NAME
  aws configure set region "eu-west-1" --profile $PROFILE_NAME
  aws configure set output "json" --profile $PROFILE_NAME

  # Check if the configuration was successful
  if [ $? -eq 0 ]; then
    echo "AWS LocalStack profile '$PROFILE_NAME' configured successfully."
  else
    echo "Failed to configure AWS LocalStack profile. Exiting."
    exit 1
  fi
fi

# Generate a random bucket ID (using base64 encoding for better readability)
BUCKET_ID=$(dd if=/dev/random bs=8 count=1 2>/dev/null | od -An -tx1 | tr -d ' \t\n' | base64 | head -c 12)

# Set a fixed bucket name (can use $BUCKET_ID if dynamic naming is preferred)
BUCKET_NAME="artifact-storage-bucket-$BUCKET_ID"

# Create an S3 bucket using LocalStack (ensure endpoint and profile are correct)
echo 'Creating LocalStack S3 bucket for artifact uploading...'
aws --profile localstack --endpoint-url=http://localhost:4566 s3 mb s3://$BUCKET_NAME

## Check if bucket creation was successful
#if [ $? -ne 0 ]; then
#  echo 'Failed to create S3 bucket. Exiting.'
#  exit 1
#fi

echo "$BUCKET_NAME created"

# Function to install SDKMAN!
install_sdkman() {
  echo "Installing SDKMAN..."
  curl -s "https://get.sdkman.io" | bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdkman_installed_now=true
}

# Check if SDKMAN! is installed
if [ -z "$SDKMAN_DIR" ]; then
  install_sdkman
elif [ ! -d "$SDKMAN_DIR" ]; then
  install_sdkman
else
  echo "SDKMAN! is already installed."
  source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

# Check if Java 17.0.13-tem is installed, if not, install it+
if ! sdk list java | grep -q "17.0.13-tem.*installed"; then
  echo "Installing Java 17.0.13-tem..."
  sdk install java 17.0.13-tem
else
  echo "Java 17.0.13-tem is already installed."
fi

# Use Java 17.0.13-tem for the current directory
echo "Using Java 17.0.13-tem for the current directory..."
sdk use java 17.0.13-tem

# Display the start of the artifact generation process
echo 'Generating product lambda jar...'

# Build the product lambda jar file using Maven
mvn clean package

# Check if Maven build was successful
if [ $? -ne 0 ]; then
  echo 'Maven build failed. Exiting.'
  exit 1
fi

echo 'Artifact created!'

## Upload the Lambda artifact to the created S3 bucket
#echo 'Uploading product lambda artifact to S3 bucket...'
#aws --profile localstack --endpoint-url=http://localhost:4566 s3 cp target/product-lambda-0.0.1-SNAPSHOT.jar s3://$BUCKET_NAME/product-lambda-0.0.1-SNAPSHOT.jar
#
## Check if upload was successful
#if [ $? -ne 0 ]; then
#  echo 'Failed to upload artifact to S3. Exiting.'
#  exit 1
#fi
#
#echo "Artifact uploaded to $BUCKET_NAME"


sleep 5
set -eo pipefail

# Set CloudFormation template and bucket variable
ARTIFACT_BUCKET=$BUCKET_NAME
TEMPLATE="template-mvn.yaml"

# Deploy CloudFormation resources using LocalStack
echo 'Deploying LocalStack CloudFormation resources...'

aws --profile localstack --endpoint-url=http://localhost:4566 cloudformation package --template-file $TEMPLATE --s3-bucket $ARTIFACT_BUCKET --output-template-file out.yml
aws --profile localstack --endpoint-url=http://localhost:4566 cloudformation deploy --template-file out.yml --stack-name product-lambda-sample --capabilities CAPABILITY_NAMED_IAM

# Check if CloudFormation deployment was successful
if [ $? -ne 0 ]; then
  echo 'CloudFormation deployment failed. Exiting.'
  exit 1
fi

echo 'Deployment completed successfully!'
