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

#### Bucket Operations

```bash
# List all buckets
s3 ls

# Create a bucket
s3 mb s3://bucket-name

# Remove a bucket (must be empty)
s3 rb s3://bucket-name

# Check if bucket exists
s3 head-bucket s3://bucket-name

# Get bucket location/region
s3 get-bucket-location s3://bucket-name

# Get bucket versioning status
s3 get-bucket-versioning s3://bucket-name

# Enable/suspend bucket versioning
s3 put-bucket-versioning s3://bucket-name --status Enabled
s3 put-bucket-versioning s3://bucket-name --status Suspended
```

#### Object Operations

```bash
# List objects in a bucket
s3 ls s3://bucket-name

# List objects with a prefix
s3 ls s3://bucket-name/prefix

# Upload a file
s3 cp local-file.txt s3://bucket-name/key

# Download a file
s3 cp s3://bucket-name/key local-file.txt

# Copy between S3 locations
s3 cp s3://bucket1/key1 s3://bucket2/key2

# Move an object (copy and delete)
s3 mv s3://bucket/source s3://bucket/dest

# Remove an object
s3 rm s3://bucket-name/key

# Delete multiple objects
s3 delete-objects s3://bucket --keys "key1,key2,key3"

# Get object metadata
s3 head-object s3://bucket/key

# Get object URL
s3 get-object-url s3://bucket/key
```

#### Object Tagging

```bash
# Get object tags
s3 get-object-tagging s3://bucket/key

# Set object tags
s3 put-object-tagging s3://bucket/key --tags "Environment=prod,Team=backend"

# Delete object tags
s3 delete-object-tagging s3://bucket/key
```

#### Sync Operations

```bash
# Sync local directory to S3
s3 sync ./local-dir s3://bucket/prefix

# Sync S3 to local directory
s3 sync s3://bucket/prefix ./local-dir
```

#### Using Variables with S3

```bash
# Set bucket variable
set BUCKET my-production-bucket

# Use in commands
s3 ls s3://$BUCKET
s3 head-bucket s3://$BUCKET
s3 cp file.txt s3://$BUCKET/uploads/file.txt
s3 sync ./dist s3://$BUCKET/website

# Object operations with variables
set KEY data/file.json
s3 head-object s3://$BUCKET/$KEY
s3 get-object-tagging s3://$BUCKET/$KEY
```

### EC2 Commands

#### Instance Lifecycle

```bash
# Describe all instances
ec2 describe-instances

# Describe specific instances
ec2 describe-instances --instance-ids i-1234567890abcdef0

# Run (launch) new instances
ec2 run-instances --image-id ami-12345678 --instance-type t2.micro --count 1
ec2 run-instances --image-id ami-12345678 --instance-type t2.micro --key-name mykey --security-group-ids sg-12345678 --subnet-id subnet-12345678

# Start instances
ec2 start-instances --instance-ids i-1234567890abcdef0,i-0987654321fedcba0

# Stop instances
ec2 stop-instances --instance-ids i-1234567890abcdef0

# Reboot instances
ec2 reboot-instances --instance-ids i-1234567890abcdef0

# Terminate instances
ec2 terminate-instances --instance-ids i-1234567890abcdef0
```

#### Volume Operations

```bash
# Describe volumes
ec2 describe-volumes
ec2 describe-volumes --volume-ids vol-12345678

# Create a volume
ec2 create-volume --availability-zone us-east-2a --size 10
ec2 create-volume --availability-zone us-east-2a --size 10 --volume-type gp3

# Attach a volume
ec2 attach-volume --volume-id vol-12345678 --instance-id i-12345678 --device /dev/sdf

# Detach a volume
ec2 detach-volume --volume-id vol-12345678

# Delete a volume
ec2 delete-volume --volume-id vol-12345678
```

#### Snapshot Operations

```bash
# Describe snapshots
ec2 describe-snapshots
ec2 describe-snapshots --owner-ids self

# Create a snapshot
ec2 create-snapshot --volume-id vol-12345678 --description "My backup"

# Delete a snapshot
ec2 delete-snapshot --snapshot-id snap-12345678
```

#### AMI Operations

```bash
# Describe images
ec2 describe-images --owner-ids self
ec2 describe-images --image-ids ami-12345678

# Create an image
ec2 create-image --instance-id i-12345678 --name "My AMI" --description "My custom AMI"
```

#### Key Pairs

```bash
# Describe key pairs
ec2 describe-key-pairs

# Create a key pair
ec2 create-key-pair --key-name mykey

# Delete a key pair
ec2 delete-key-pair --key-name mykey
```

#### Security Groups

```bash
# Describe security groups
ec2 describe-security-groups
ec2 describe-security-groups --group-ids sg-12345678

# Create a security group
ec2 create-security-group --group-name mygroup --description "My security group" --vpc-id vpc-12345678

# Delete a security group
ec2 delete-security-group --group-id sg-12345678

# Authorize inbound traffic
ec2 authorize-security-group-ingress --group-id sg-12345678 --protocol tcp --port 22 --cidr 0.0.0.0/0
```

#### Elastic IPs

```bash
# Describe addresses
ec2 describe-addresses

# Allocate an address
ec2 allocate-address

# Associate an address
ec2 associate-address --instance-id i-12345678 --allocation-id eipalloc-12345678

# Release an address
ec2 release-address --allocation-id eipalloc-12345678
```

#### Tags

```bash
# Create tags
ec2 create-tags --resources i-12345678 vol-12345678 --tags "Name=MyInstance,Environment=prod"
```

#### Network Resources

```bash
# Describe VPCs
ec2 describe-vpcs

# Describe subnets
ec2 describe-subnets

# Describe subnets in a VPC
ec2 describe-subnets --vpc-id vpc-12345678
```

#### Using Variables with EC2

```bash
# Set instance variables
set INSTANCE_ID i-1234567890abcdef0
set AMI_ID ami-12345678

# Use in commands
ec2 describe-instances --instance-ids $INSTANCE_ID
ec2 stop-instances --instance-ids $INSTANCE_ID
ec2 run-instances --image-id $AMI_ID --instance-type t2.micro

# Volume workflow
set VOL_ID vol-12345678
ec2 describe-volumes --volume-ids $VOL_ID
ec2 attach-volume --volume-id $VOL_ID --instance-id $INSTANCE_ID --device /dev/sdf
```

### IAM Commands

#### User Management

```bash
# List users
iam list-users

# Get current user details
iam get-user

# Get specific user details
iam get-user --user-name username

# Create a user
iam create-user --user-name username
iam create-user --user-name username --path /division/ --tags "Department=Engineering,Team=Backend"

# Delete a user
iam delete-user --user-name username

# Update a user
iam update-user --user-name oldname --new-user-name newname
iam update-user --user-name username --new-path /new/path/

# Create login profile (console password)
iam create-login-profile --user-name username --password mypassword
iam create-login-profile --user-name username --password mypassword --password-reset-required true

# Delete login profile
iam delete-login-profile --user-name username
```

#### Role Management

```bash
# List roles
iam list-roles

# Get role details
iam get-role --role-name rolename

# Create a role
iam create-role --role-name rolename --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}'
iam create-role --role-name rolename --assume-role-policy-document file://trust-policy.json --description "My application role"

# Delete a role
iam delete-role --role-name rolename

# Update a role
iam update-role --role-name rolename --description "New description"
iam update-role --role-name rolename --max-session-duration 7200

# Attach managed policy to role
iam attach-role-policy --role-name rolename --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess

# Detach managed policy from role
iam detach-role-policy --role-name rolename --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess

# List attached role policies
iam list-attached-role-policies --role-name rolename

# Put inline policy on role
iam put-role-policy --role-name rolename --policy-name mypolicy --policy-document '{"Version":"2012-10-17",...}'

# Get inline policy from role
iam get-role-policy --role-name rolename --policy-name mypolicy

# Delete inline policy from role
iam delete-role-policy --role-name rolename --policy-name mypolicy

# List inline policies for role
iam list-role-policies --role-name rolename
```

#### Policy Management

```bash
# List policies
iam list-policies

# List local policies only
iam list-policies --scope Local

# Get policy details
iam get-policy --policy-arn arn:aws:iam::123456789012:policy/mypolicy

# Get policy version
iam get-policy-version --policy-arn arn:aws:iam::123456789012:policy/mypolicy --version-id v1

# Create a policy
iam create-policy --policy-name mypolicy --policy-document '{"Version":"2012-10-17","Statement":[...]}'
iam create-policy --policy-name mypolicy --policy-document file://policy.json --description "My custom policy"

# Delete a policy
iam delete-policy --policy-arn arn:aws:iam::123456789012:policy/mypolicy

# Attach policy to user
iam attach-user-policy --user-name username --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess

# Detach policy from user
iam detach-user-policy --user-name username --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess

# List attached user policies
iam list-attached-user-policies --user-name username

# Put inline policy on user
iam put-user-policy --user-name username --policy-name mypolicy --policy-document '{"Version":"2012-10-17",...}'

# Get inline policy from user
iam get-user-policy --user-name username --policy-name mypolicy

# Delete inline policy from user
iam delete-user-policy --user-name username --policy-name mypolicy

# List inline policies for user
iam list-user-policies --user-name username
```

#### Access Key Management

```bash
# List access keys
iam list-access-keys
iam list-access-keys --user-name username

# Create access key
iam create-access-key
iam create-access-key --user-name username

# Delete access key
iam delete-access-key --access-key-id AKIAIOSFODNN7EXAMPLE
iam delete-access-key --access-key-id AKIAIOSFODNN7EXAMPLE --user-name username

# Update access key status
iam update-access-key --access-key-id AKIAIOSFODNN7EXAMPLE --status Active
iam update-access-key --access-key-id AKIAIOSFODNN7EXAMPLE --status Inactive --user-name username
```

#### Group Management

```bash
# List groups
iam list-groups

# Get group details
iam get-group --group-name groupname

# Create a group
iam create-group --group-name groupname
iam create-group --group-name groupname --path /division/

# Delete a group
iam delete-group --group-name groupname

# Add user to group
iam add-user-to-group --group-name groupname --user-name username

# Remove user from group
iam remove-user-from-group --group-name groupname --user-name username

# List groups for user
iam list-groups-for-user --user-name username

# Attach policy to group
iam attach-group-policy --group-name groupname --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess

# Detach policy from group
iam detach-group-policy --group-name groupname --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess

# List attached group policies
iam list-attached-group-policies --group-name groupname

# Put inline policy on group
iam put-group-policy --group-name groupname --policy-name mypolicy --policy-document '{"Version":"2012-10-17",...}'

# Get inline policy from group
iam get-group-policy --group-name groupname --policy-name mypolicy

# Delete inline policy from group
iam delete-group-policy --group-name groupname --policy-name mypolicy

# List inline policies for group
iam list-group-policies --group-name groupname
```

#### Using Variables with IAM

```bash
# Set IAM variables
set USER_NAME john.doe
set ROLE_NAME MyAppRole
set POLICY_ARN arn:aws:iam::aws:policy/ReadOnlyAccess

# Use in commands
iam get-user --user-name $USER_NAME
iam list-groups-for-user --user-name $USER_NAME
iam get-role --role-name $ROLE_NAME
iam attach-role-policy --role-name $ROLE_NAME --policy-arn $POLICY_ARN

# Create and configure user workflow
set NEW_USER alice
iam create-user --user-name $NEW_USER
iam create-login-profile --user-name $NEW_USER --password TempPass123!
iam attach-user-policy --user-name $NEW_USER --policy-arn $POLICY_ARN
iam list-attached-user-policies --user-name $NEW_USER
```

### STS Commands

#### Identity and Configuration

```bash
# Get caller identity (shows current AWS account and user)
sts get-caller-identity

# Show current AWS configuration
aws configure show
```

#### Assume Role Operations

```bash
# Assume an IAM role
sts assume-role --role-arn arn:aws:iam::123456789012:role/MyRole --role-session-name MySession
sts assume-role --role-arn arn:aws:iam::123456789012:role/MyRole --role-session-name MySession --duration-seconds 3600

# Assume role with external ID
sts assume-role --role-arn arn:aws:iam::123456789012:role/MyRole --role-session-name MySession --external-id MyExternalId

# Assume role with MFA
sts assume-role --role-arn arn:aws:iam::123456789012:role/MyRole --role-session-name MySession --serial-number arn:aws:iam::123456789012:mfa/user --token-code 123456

# Assume role with policy
sts assume-role --role-arn arn:aws:iam::123456789012:role/MyRole --role-session-name MySession --policy '{"Version":"2012-10-17","Statement":[...]}'

# Assume role with SAML assertion
sts assume-role-with-saml --role-arn arn:aws:iam::123456789012:role/MyRole --principal-arn arn:aws:iam::123456789012:saml-provider/MyProvider --saml-assertion base64-encoded-saml

# Assume role with web identity token
sts assume-role-with-web-identity --role-arn arn:aws:iam::123456789012:role/MyRole --role-session-name MySession --web-identity-token token
```

#### Session Token Operations

```bash
# Get session token
sts get-session-token
sts get-session-token --duration-seconds 3600

# Get session token with MFA
sts get-session-token --serial-number arn:aws:iam::123456789012:mfa/user --token-code 123456 --duration-seconds 43200

# Get federation token
sts get-federation-token --name MyFederatedUser
sts get-federation-token --name MyFederatedUser --duration-seconds 43200 --policy '{"Version":"2012-10-17","Statement":[...]}'
```

#### Utility Operations

```bash
# Decode authorization message
sts decode-authorization-message --encoded-message "encoded-message-string"

# Get access key info
sts get-access-key-info --access-key-id AKIAIOSFODNN7EXAMPLE
```

#### Using Variables with STS

```bash
# Set STS variables
set ROLE_ARN arn:aws:iam::123456789012:role/MyRole
set SESSION_NAME MySession
set MFA_SERIAL arn:aws:iam::123456789012:mfa/user

# Use in commands
sts assume-role --role-arn $ROLE_ARN --role-session-name $SESSION_NAME
sts get-session-token --serial-number $MFA_SERIAL --token-code 123456

# Assume role workflow
set ROLE_ARN arn:aws:iam::123456789012:role/MyRole
set SESSION_NAME dev-session-$(date +%s)
sts assume-role --role-arn $ROLE_ARN --role-session-name $SESSION_NAME --duration-seconds 3600
# Use the returned credentials in subsequent AWS operations
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
