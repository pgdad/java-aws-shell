package com.aws.shell.commands;

import com.aws.shell.context.SessionContext;
import com.aws.shell.util.OutputFormatter;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * STS Commands
 * <p>
 * Implements AWS STS CLI-like functionality with variable support
 */
@ShellComponent
public class StsCommands {

    private final StsClient stsClient;
    private final SessionContext sessionContext;

    public StsCommands(StsClient stsClient, SessionContext sessionContext) {
        this.stsClient = stsClient;
        this.sessionContext = sessionContext;
    }

    /**
     * Get caller identity
     * <p>
     * Usage:
     * sts get-caller-identity
     */
    @ShellMethod(key = "sts get-caller-identity", value = "Get caller identity information")
    public String getCallerIdentity() {
        try {
            GetCallerIdentityResponse response = stsClient.getCallerIdentity();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Account", response.account()});
            pairs.add(new String[]{"User ID", response.userId()});
            pairs.add(new String[]{"ARN", response.arn()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Show current AWS configuration
     * <p>
     * Usage:
     * aws configure show
     */
    @ShellMethod(key = "aws configure show", value = "Show current AWS configuration")
    public String configureShow() {
        List<String[]> pairs = new ArrayList<>();

        String profile = System.getenv("AWS_PROFILE");
        String region = System.getenv("AWS_REGION");
        String defaultRegion = System.getenv("AWS_DEFAULT_REGION");

        pairs.add(new String[]{"AWS_PROFILE", profile != null ? profile : "(not set - using default)"});
        pairs.add(new String[]{"AWS_REGION", region != null ? region : "(not set)"});
        pairs.add(new String[]{"AWS_DEFAULT_REGION", defaultRegion != null ? defaultRegion : "(not set)"});
        pairs.add(new String[]{"Effective Region", region != null ? region : (defaultRegion != null ? defaultRegion : "us-east-2 (default)")});

        return OutputFormatter.toKeyValue(pairs);
    }

    // ==================== Assume Role Operations ====================

    /**
     * Assume an IAM role
     * <p>
     * Usage:
     * sts assume-role --role-arn arn:aws:iam::123456789012:role/MyRole --role-session-name MySession
     * sts assume-role --role-arn $ROLE_ARN --role-session-name $SESSION_NAME --duration-seconds 3600
     */
    @ShellMethod(key = "sts assume-role", value = "Assume an IAM role")
    public String assumeRole(String roleArn,
                             String roleSessionName,
                             @ShellOption(defaultValue = "3600") int durationSeconds,
                             @ShellOption(defaultValue = "") String externalId,
                             @ShellOption(defaultValue = "") String serialNumber,
                             @ShellOption(defaultValue = "") String tokenCode,
                             @ShellOption(defaultValue = "") String policy) {
        try {
            roleArn = sessionContext.resolveVariables(roleArn);
            roleSessionName = sessionContext.resolveVariables(roleSessionName);
            externalId = sessionContext.resolveVariables(externalId);
            serialNumber = sessionContext.resolveVariables(serialNumber);
            tokenCode = sessionContext.resolveVariables(tokenCode);
            policy = sessionContext.resolveVariables(policy);

            AssumeRoleRequest.Builder requestBuilder = AssumeRoleRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName(roleSessionName)
                    .durationSeconds(durationSeconds);

            if (!externalId.isEmpty()) {
                requestBuilder.externalId(externalId);
            }
            if (!serialNumber.isEmpty()) {
                requestBuilder.serialNumber(serialNumber);
            }
            if (!tokenCode.isEmpty()) {
                requestBuilder.tokenCode(tokenCode);
            }
            if (!policy.isEmpty()) {
                requestBuilder.policy(policy);
            }

            AssumeRoleResponse response = stsClient.assumeRole(requestBuilder.build());
            Credentials credentials = response.credentials();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Access Key ID", credentials.accessKeyId()});
            pairs.add(new String[]{"Secret Access Key", credentials.secretAccessKey()});
            pairs.add(new String[]{"Session Token", credentials.sessionToken()});
            pairs.add(new String[]{"Expiration", credentials.expiration().toString()});
            pairs.add(new String[]{"Assumed Role ARN", response.assumedRoleUser().arn()});
            pairs.add(new String[]{"Assumed Role ID", response.assumedRoleUser().assumedRoleId()});

            return "Role assumed successfully:\n" + OutputFormatter.toKeyValue(pairs) +
                    "\n\nTo use these credentials, export them as environment variables:\n" +
                    "export AWS_ACCESS_KEY_ID=" + credentials.accessKeyId() + "\n" +
                    "export AWS_SECRET_ACCESS_KEY=" + credentials.secretAccessKey() + "\n" +
                    "export AWS_SESSION_TOKEN=" + credentials.sessionToken();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Assume a role using SAML assertion
     * <p>
     * Usage:
     * sts assume-role-with-saml --role-arn arn:aws:iam::123456789012:role/MyRole --principal-arn arn:aws:iam::123456789012:saml-provider/MyProvider --saml-assertion base64-encoded-saml
     */
    @ShellMethod(key = "sts assume-role-with-saml", value = "Assume a role using SAML assertion")
    public String assumeRoleWithSaml(String roleArn,
                                     String principalArn,
                                     String samlAssertion,
                                     @ShellOption(defaultValue = "3600") int durationSeconds,
                                     @ShellOption(defaultValue = "") String policy) {
        try {
            roleArn = sessionContext.resolveVariables(roleArn);
            principalArn = sessionContext.resolveVariables(principalArn);
            samlAssertion = sessionContext.resolveVariables(samlAssertion);
            policy = sessionContext.resolveVariables(policy);

            AssumeRoleWithSamlRequest.Builder requestBuilder = AssumeRoleWithSamlRequest.builder()
                    .roleArn(roleArn)
                    .principalArn(principalArn)
                    .samlAssertion(samlAssertion)
                    .durationSeconds(durationSeconds);

            if (!policy.isEmpty()) {
                requestBuilder.policy(policy);
            }

            AssumeRoleWithSamlResponse response = stsClient.assumeRoleWithSAML(requestBuilder.build());
            Credentials credentials = response.credentials();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Access Key ID", credentials.accessKeyId()});
            pairs.add(new String[]{"Secret Access Key", credentials.secretAccessKey()});
            pairs.add(new String[]{"Session Token", credentials.sessionToken()});
            pairs.add(new String[]{"Expiration", credentials.expiration().toString()});
            pairs.add(new String[]{"Assumed Role ARN", response.assumedRoleUser().arn()});
            pairs.add(new String[]{"Assumed Role ID", response.assumedRoleUser().assumedRoleId()});
            pairs.add(new String[]{"Subject", response.subject()});
            pairs.add(new String[]{"Issuer", response.issuer()});

            return "Role assumed with SAML successfully:\n" + OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Assume a role using web identity token
     * <p>
     * Usage:
     * sts assume-role-with-web-identity --role-arn arn:aws:iam::123456789012:role/MyRole --role-session-name MySession --web-identity-token token
     */
    @ShellMethod(key = "sts assume-role-with-web-identity", value = "Assume a role using web identity token")
    public String assumeRoleWithWebIdentity(String roleArn,
                                            String roleSessionName,
                                            String webIdentityToken,
                                            @ShellOption(defaultValue = "3600") int durationSeconds,
                                            @ShellOption(defaultValue = "") String providerId,
                                            @ShellOption(defaultValue = "") String policy) {
        try {
            roleArn = sessionContext.resolveVariables(roleArn);
            roleSessionName = sessionContext.resolveVariables(roleSessionName);
            webIdentityToken = sessionContext.resolveVariables(webIdentityToken);
            providerId = sessionContext.resolveVariables(providerId);
            policy = sessionContext.resolveVariables(policy);

            AssumeRoleWithWebIdentityRequest.Builder requestBuilder = AssumeRoleWithWebIdentityRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName(roleSessionName)
                    .webIdentityToken(webIdentityToken)
                    .durationSeconds(durationSeconds);

            if (!providerId.isEmpty()) {
                requestBuilder.providerId(providerId);
            }
            if (!policy.isEmpty()) {
                requestBuilder.policy(policy);
            }

            AssumeRoleWithWebIdentityResponse response = stsClient.assumeRoleWithWebIdentity(requestBuilder.build());
            Credentials credentials = response.credentials();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Access Key ID", credentials.accessKeyId()});
            pairs.add(new String[]{"Secret Access Key", credentials.secretAccessKey()});
            pairs.add(new String[]{"Session Token", credentials.sessionToken()});
            pairs.add(new String[]{"Expiration", credentials.expiration().toString()});
            pairs.add(new String[]{"Assumed Role ARN", response.assumedRoleUser().arn()});
            pairs.add(new String[]{"Assumed Role ID", response.assumedRoleUser().assumedRoleId()});
            pairs.add(new String[]{"Subject From Web Identity Token", response.subjectFromWebIdentityToken()});
            pairs.add(new String[]{"Provider", response.provider()});

            return "Role assumed with web identity successfully:\n" + OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ==================== Session Token Operations ====================

    /**
     * Get temporary credentials for the AWS account
     * <p>
     * Usage:
     * sts get-session-token
     * sts get-session-token --duration-seconds 3600
     * sts get-session-token --serial-number arn:aws:iam::123456789012:mfa/user --token-code 123456
     */
    @ShellMethod(key = "sts get-session-token", value = "Get temporary credentials for the AWS account")
    public String getSessionToken(@ShellOption(defaultValue = "3600") int durationSeconds,
                                  @ShellOption(defaultValue = "") String serialNumber,
                                  @ShellOption(defaultValue = "") String tokenCode) {
        try {
            serialNumber = sessionContext.resolveVariables(serialNumber);
            tokenCode = sessionContext.resolveVariables(tokenCode);

            GetSessionTokenRequest.Builder requestBuilder = GetSessionTokenRequest.builder()
                    .durationSeconds(durationSeconds);

            if (!serialNumber.isEmpty()) {
                requestBuilder.serialNumber(serialNumber);
            }
            if (!tokenCode.isEmpty()) {
                requestBuilder.tokenCode(tokenCode);
            }

            GetSessionTokenResponse response = stsClient.getSessionToken(requestBuilder.build());
            Credentials credentials = response.credentials();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Access Key ID", credentials.accessKeyId()});
            pairs.add(new String[]{"Secret Access Key", credentials.secretAccessKey()});
            pairs.add(new String[]{"Session Token", credentials.sessionToken()});
            pairs.add(new String[]{"Expiration", credentials.expiration().toString()});

            return "Session token obtained successfully:\n" + OutputFormatter.toKeyValue(pairs) +
                    "\n\nTo use these credentials, export them as environment variables:\n" +
                    "export AWS_ACCESS_KEY_ID=" + credentials.accessKeyId() + "\n" +
                    "export AWS_SECRET_ACCESS_KEY=" + credentials.secretAccessKey() + "\n" +
                    "export AWS_SESSION_TOKEN=" + credentials.sessionToken();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get temporary credentials for a federated user
     * <p>
     * Usage:
     * sts get-federation-token --name MyFederatedUser
     * sts get-federation-token --name MyFederatedUser --policy '{"Version":"2012-10-17",...}'
     */
    @ShellMethod(key = "sts get-federation-token", value = "Get temporary credentials for a federated user")
    public String getFederationToken(String name,
                                     @ShellOption(defaultValue = "43200") int durationSeconds,
                                     @ShellOption(defaultValue = "") String policy) {
        try {
            name = sessionContext.resolveVariables(name);
            policy = sessionContext.resolveVariables(policy);

            GetFederationTokenRequest.Builder requestBuilder = GetFederationTokenRequest.builder()
                    .name(name)
                    .durationSeconds(durationSeconds);

            if (!policy.isEmpty()) {
                requestBuilder.policy(policy);
            }

            GetFederationTokenResponse response = stsClient.getFederationToken(requestBuilder.build());
            Credentials credentials = response.credentials();
            FederatedUser federatedUser = response.federatedUser();

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Access Key ID", credentials.accessKeyId()});
            pairs.add(new String[]{"Secret Access Key", credentials.secretAccessKey()});
            pairs.add(new String[]{"Session Token", credentials.sessionToken()});
            pairs.add(new String[]{"Expiration", credentials.expiration().toString()});
            pairs.add(new String[]{"Federated User ARN", federatedUser.arn()});
            pairs.add(new String[]{"Federated User ID", federatedUser.federatedUserId()});

            return "Federation token obtained successfully:\n" + OutputFormatter.toKeyValue(pairs) +
                    "\n\nTo use these credentials, export them as environment variables:\n" +
                    "export AWS_ACCESS_KEY_ID=" + credentials.accessKeyId() + "\n" +
                    "export AWS_SECRET_ACCESS_KEY=" + credentials.secretAccessKey() + "\n" +
                    "export AWS_SESSION_TOKEN=" + credentials.sessionToken();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ==================== Utility Operations ====================

    /**
     * Decode an authorization message
     * <p>
     * Usage:
     * sts decode-authorization-message --encoded-message "encoded-message-string"
     */
    @ShellMethod(key = "sts decode-authorization-message", value = "Decode an authorization message")
    public String decodeAuthorizationMessage(String encodedMessage) {
        try {
            encodedMessage = sessionContext.resolveVariables(encodedMessage);

            DecodeAuthorizationMessageRequest request = DecodeAuthorizationMessageRequest.builder()
                    .encodedMessage(encodedMessage)
                    .build();

            DecodeAuthorizationMessageResponse response = stsClient.decodeAuthorizationMessage(request);

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Decoded Message", response.decodedMessage()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get access key info
     * <p>
     * Usage:
     * sts get-access-key-info --access-key-id AKIAIOSFODNN7EXAMPLE
     */
    @ShellMethod(key = "sts get-access-key-info", value = "Get access key info")
    public String getAccessKeyInfo(String accessKeyId) {
        try {
            accessKeyId = sessionContext.resolveVariables(accessKeyId);

            GetAccessKeyInfoRequest request = GetAccessKeyInfoRequest.builder()
                    .accessKeyId(accessKeyId)
                    .build();

            GetAccessKeyInfoResponse response = stsClient.getAccessKeyInfo(request);

            List<String[]> pairs = new ArrayList<>();
            pairs.add(new String[]{"Account", response.account()});

            return OutputFormatter.toKeyValue(pairs);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
