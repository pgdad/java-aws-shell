package com.aws.shell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AWS Shell Application - A Java Spring Shell based AWS CLI
 *
 * This application provides AWS CLI-like functionality using AWS SDK for Java v2.
 * It reads AWS_PROFILE and AWS_REGION environment variables for configuration.
 */
@SpringBootApplication
public class AwsShellApplication {

    public static void main(String[] args) {
        SpringApplication.run(AwsShellApplication.class, args);
    }
}
