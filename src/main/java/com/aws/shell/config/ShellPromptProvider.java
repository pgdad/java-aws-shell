package com.aws.shell.config;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

/**
 * Custom shell prompt provider
 */
@Component
public class ShellPromptProvider implements PromptProvider {

    @Override
    public AttributedString getPrompt() {
        String region = System.getenv("AWS_REGION");
        if (region == null || region.isEmpty()) {
            region = System.getenv("AWS_DEFAULT_REGION");
        }
        if (region == null || region.isEmpty()) {
            region = "us-east-2";
        }

        String profile = System.getenv("AWS_PROFILE");
        if (profile == null || profile.isEmpty()) {
            profile = "default";
        }

        String prompt = String.format("aws [%s|%s]> ", profile, region);
        return new AttributedString(prompt,
                AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
    }
}
