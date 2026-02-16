package com.ApexPay.backend.service;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service

public class MirrorNodeService {



    // Inside your Service
    public List<Map<String, Object>> getFullBlockchainHistory(String accountId) {
        String url = "https://testnet.mirrornode.hedera.com/api/v1/transactions?account.id=" + accountId;
        RestTemplate restTemplate = new RestTemplate();

        // Fetch the raw JSON from Hedera's public Mirror Node
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        // Extract the list of transactions
        return (List<Map<String, Object>>) response.get("transactions");
    }
}
