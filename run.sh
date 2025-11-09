#!/bin/bash
# Run script for AWS Shell

# Check if AWS_REGION is set, if not use default
if [ -z "$AWS_REGION" ]; then
    export AWS_REGION=us-east-2
    echo "AWS_REGION not set, using default: us-east-2"
fi

# Display current configuration
echo "AWS Configuration:"
echo "  AWS_PROFILE: ${AWS_PROFILE:-default}"
echo "  AWS_REGION: $AWS_REGION"
echo ""

# Run the application
java -jar target/aws-shell-1.0.0.jar
