package com.ApexPay.backend.config;

import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class HederaConfig {

    @Value("${hedera.account.id}")
    private String operatorId;

    @Value("${hedera.private.key}")
    private String operatorKey;

    @Bean
    public Client hederaClient() {
        try {
            // Log to your console (remove in production!)
            System.out.println("Initializing Hedera Client for: " + operatorId);

            // 1. Parse the key safely
            PrivateKey key = PrivateKey.fromStringECDSA(operatorKey);

            // 2. Setup Client
            Client client = Client.forTestnet();
            client.setOperator(AccountId.fromString(operatorId), key);
            client.setRequestTimeout(Duration.ofMinutes(15));
            client.setMaxAttempts(20);

            // 3. Optional: If you are behind a strict firewall, try this:
            client.setTransportSecurity(false);

            return client;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Hedera Client. Check your keys!", e);
        }
    }
}