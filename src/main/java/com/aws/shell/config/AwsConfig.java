package com.aws.shell.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * AWS Configuration
 * <p>
 * Configures AWS SDK clients using environment variables:
 * - AWS_PROFILE: The AWS profile to use (from ~/.aws/credentials)
 * - AWS_REGION: The AWS region to use (e.g., us-east-2)
 * <p>
 * If AWS_PROFILE is not set, uses default credentials provider chain.
 * If AWS_REGION is not set, defaults to us-east-2.
 */
@Configuration
public class AwsConfig {

    /**
     * Get the AWS region from environment variable or default to us-east-2
     */
    private Region getRegion() {
        String regionName = System.getenv("AWS_REGION");
        if (regionName == null || regionName.isEmpty()) {
            regionName = System.getenv("AWS_DEFAULT_REGION");
        }
        if (regionName == null || regionName.isEmpty()) {
            regionName = "us-east-2"; // Default region as per user instructions
        }
        return Region.of(regionName);
    }

    /**
     * Get the AWS credentials provider based on AWS_PROFILE environment variable
     */
    private AwsCredentialsProvider getCredentialsProvider() {
        String profileName = System.getenv("AWS_PROFILE");
        if (profileName != null && !profileName.isEmpty()) {
            return ProfileCredentialsProvider.create(profileName);
        }
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(getRegion())
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    @Bean
    public Ec2Client ec2Client() {
        return Ec2Client.builder()
                .region(getRegion())
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    @Bean
    public IamClient iamClient() {
        // IAM is global, so we use a fixed region
        return IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    @Bean
    public StsClient stsClient() {
        return StsClient.builder()
                .region(getRegion())
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    @Bean
    public CloudFormationClient cloudFormationClient() {
        return CloudFormationClient.builder()
                .region(getRegion())
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(getRegion())
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(getRegion())
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(getRegion())
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(getRegion())
                .credentialsProvider(getCredentialsProvider())
                .build();
    }
}
