package com.aws.shell.commands;

import com.aws.shell.util.OutputFormatter;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * STS Commands
 * <p>
 * Implements AWS STS CLI-like functionality
 */
@ShellComponent
public class StsCommands {

    private final StsClient stsClient;

    public StsCommands(StsClient stsClient) {
        this.stsClient = stsClient;
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
}
