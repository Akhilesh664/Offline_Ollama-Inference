package com.example.ollamaAi.demo_ollama;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configuration class for setting up retry behavior in the application.
 * This class configures the retry mechanism with exponential backoff policy
 * to handle transient failures when making external API calls.
 */
@Configuration
@EnableRetry // Enables Spring's retry functionality
public class RetryConfig {

    /**
     * Creates and configures a RetryTemplate bean with exponential backoff policy.
     * This template will be used to add retry capabilities to methods annotated with @Retryable.
     *
     * @return Configured RetryTemplate instance with exponential backoff and retry policy
     */
    @Bean
    public RetryTemplate retryTemplate() {

        // Create a RetryTemplate instance
        RetryTemplate retryTemplate = new RetryTemplate();

        // For BackOffPolicy from trying
        // Configure exponential backoff policy to progressively increase delay between retries
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        // Initial delay before the first retry (in milliseconds)
        backOffPolicy.setInitialInterval(1000); // 1 second
        // Multiplier to apply to the previous retry interval
        // e.g., 1st retry after 1s, 2nd after 2s, 3rd after 4s, etc.
        backOffPolicy.setMultiplier(2.0);
        // Maximum time to wait between retries (in milliseconds)
        backOffPolicy.setMaxInterval(10000); // 10 seconds


        // Apply the backoff policy to the retry template
        retryTemplate.setBackOffPolicy(backOffPolicy);


        // For RetryPolicy
        // Configure retry policy to define when retries should occur
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        // Maximum number of retry attempts (initial attempt + 2 retries = 3 total attempts)
        retryPolicy.setMaxAttempts(3);


        // AtLast saving in retryTemplate
        // Apply the retry policy to the retry template
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}