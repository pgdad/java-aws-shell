# AWS Shell - Java Spring Shell CLI

A Java Spring Shell based AWS CLI that provides AWS CLI-like functionality using AWS SDK for Java v2.

## Features

- **Environment-based Configuration**: Automatically reads `AWS_PROFILE` and `AWS_REGION` environment variables
- **Interactive Shell**: Built on Spring Shell for an intuitive command-line experience
- **Shell Variables**: Store and reuse values across commands with `$VAR` syntax
- **Variable Interpolation**: Use variables in any command parameter
- **AWS Service Support**: Implements common AWS operations for:
  - S3 (Simple Storage Service)
  - EC2 (Elastic Compute Cloud)
  - IAM (Identity and Access Management)
  - STS (Security Token Service)

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- AWS credentials configured in `~/.aws/credentials`

## Building

```bash
mvn clean package
```

## Running

### Using Maven

```bash
# Set environment variables
export AWS_PROFILE=your-profile-name
export AWS_REGION=us-east-2

# Run the application
mvn spring-boot:run
```

### Using the JAR

```bash
# Build the JAR
mvn clean package

# Run the JAR
export AWS_PROFILE=your-profile-name
export AWS_REGION=us-east-2
java -jar target/aws-shell-1.0.0.jar
```

## Environment Variables

- `AWS_PROFILE`: The AWS credentials profile to use (from `~/.aws/credentials`)
- `AWS_REGION`: The AWS region to use (e.g., `us-east-2`)
- `AWS_DEFAULT_REGION`: Alternative to `AWS_REGION`

If not set, the application defaults to:
- Profile: `default`
- Region: `us-east-2`

## Shell Variables

The shell supports variables for storing and reusing values across commands. Variables can be referenced using `$VAR_NAME` or `${VAR_NAME}` syntax.

### Variable Management Commands

```bash
# Set a variable
set MY_BUCKET my-test-bucket
set INSTANCE_ID i-1234567890abcdef0

# Get a variable value
get MY_BUCKET

# List all variables
vars
# or
variables

# Unset a variable
unset MY_BUCKET

# Clear all variables
clear-vars

# Export (alias for set)
export REGION us-east-2

# Echo with variable substitution
echo My bucket is $MY_BUCKET
```

### Using Variables in Commands

Variables can be used in any command that accepts string parameters:

```bash
# S3 examples
set BUCKET my-test-bucket
s3 ls s3://$BUCKET
s3 cp file.txt s3://$BUCKET/uploads/file.txt
s3 rm s3://${BUCKET}/old-file.txt

# EC2 examples
set INSTANCE_ID i-1234567890abcdef0
ec2 describe-instances --instance-ids $INSTANCE_ID
ec2 stop-instances --instance-ids $INSTANCE_ID

set VPC_ID vpc-12345678
ec2 describe-subnets --vpc-id $VPC_ID

# IAM examples
set USER_NAME john.doe
iam get-user --user-name $USER_NAME
iam list-groups-for-user --user-name $USER_NAME

set ROLE_NAME MyAppRole
iam get-role --role-name $ROLE_NAME
```

### Variable Workflow Example

```bash
# Workflow: Create bucket, upload file, then clean up
set BUCKET test-bucket-$(date +%s)
s3 mb s3://$BUCKET
s3 cp myfile.txt s3://$BUCKET/myfile.txt
s3 ls s3://$BUCKET

# Later, clean up
s3 rm s3://$BUCKET/myfile.txt
s3 rb s3://$BUCKET
unset BUCKET
```

## Available Commands

### S3 Commands

```bash
# List all buckets
s3 ls

# List objects in a bucket
s3 ls s3://bucket-name

# List objects with a prefix
s3 ls s3://bucket-name/prefix

# Create a bucket
s3 mb s3://bucket-name

# Remove a bucket
s3 rb s3://bucket-name

# Upload a file
s3 cp local-file.txt s3://bucket-name/key

# Download a file
s3 cp s3://bucket-name/key local-file.txt

# Copy between S3 locations
s3 cp s3://bucket1/key1 s3://bucket2/key2

# Remove an object
s3 rm s3://bucket-name/key
```

### EC2 Commands

```bash
# Describe all instances
ec2 describe-instances

# Describe specific instances
ec2 describe-instances --instance-ids i-1234567890abcdef0

# Start instances
ec2 start-instances --instance-ids i-1234567890abcdef0,i-0987654321fedcba0

# Stop instances
ec2 stop-instances --instance-ids i-1234567890abcdef0

# Terminate instances
ec2 terminate-instances --instance-ids i-1234567890abcdef0

# Describe VPCs
ec2 describe-vpcs

# Describe subnets
ec2 describe-subnets

# Describe subnets in a VPC
ec2 describe-subnets --vpc-id vpc-12345678

# Describe security groups
ec2 describe-security-groups
```

### IAM Commands

```bash
# List users
iam list-users

# Get current user details
iam get-user

# Get specific user details
iam get-user --user-name username

# List roles
iam list-roles

# Get role details
iam get-role --role-name rolename

# List policies
iam list-policies

# List local policies only
iam list-policies --scope Local

# List groups for a user
iam list-groups-for-user --user-name username

# List attached user policies
iam list-attached-user-policies --user-name username
```

### STS Commands

```bash
# Get caller identity (shows current AWS account and user)
sts get-caller-identity

# Show current AWS configuration
aws configure show
```

### Built-in Shell Commands

```bash
# Show help
help

# Show command history
history

# Clear screen
clear

# Show stack trace for last error
stacktrace

# Exit the shell
quit
# or
exit
```

## Project Structure

```
src/main/java/com/aws/shell/
├── AwsShellApplication.java           # Main application class
├── commands/                          # Command classes
│   ├── Ec2Commands.java              # EC2 operations
│   ├── IamCommands.java              # IAM operations
│   ├── S3Commands.java               # S3 operations
│   ├── StsCommands.java              # STS operations
│   └── VariableCommands.java         # Variable management
├── config/                            # Configuration classes
│   ├── AwsConfig.java                # AWS SDK client beans
│   └── ShellPromptProvider.java      # Custom shell prompt
├── context/                           # Session context
│   └── SessionContext.java           # Variable storage and resolution
└── util/                              # Utility classes
    └── OutputFormatter.java           # Output formatting utilities
```

## Extending

To add support for additional AWS services:

1. Add the AWS SDK dependency in `pom.xml`:
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>service-name</artifactId>
    <version>${aws.sdk.version}</version>
</dependency>
```

2. Create a client bean in `AwsConfig.java`:
```java
@Bean
public ServiceClient serviceClient() {
    return ServiceClient.builder()
            .region(getRegion())
            .credentialsProvider(getCredentialsProvider())
            .build();
}
```

3. Create a commands class in `commands/`:
```java
@ShellComponent
public class ServiceCommands {
    private final ServiceClient client;

    @ShellMethod(key = "service command", value = "Description")
    public String command() {
        // Implementation
    }
}
```

## License

This project is provided as-is for educational and development purposes.
