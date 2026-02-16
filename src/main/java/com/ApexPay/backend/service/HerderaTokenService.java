package com.ApexPay.backend.service;

import com.hedera.hashgraph.sdk.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HerderaTokenService { // Class starts here

    @Autowired
    private Client hederaClient;

    public String createCompanyToken() throws Exception {
        // Build the transaction
        TokenCreateTransaction transaction = new TokenCreateTransaction()
                .setTokenName("ApexPay Equity")
                .setTokenSymbol("APEX")
                .setDecimals(2)
                .setInitialSupply(1_000_000)
                .setTreasuryAccountId(hederaClient.getOperatorAccountId())
                .setAdminKey(hederaClient.getOperatorPublicKey())
                .setSupplyKey(hederaClient.getOperatorPublicKey())
                .freezeWith(hederaClient);

        // Sign and execute
        TransactionResponse response = transaction.execute(hederaClient);

        // Get the Receipt to find the new Token ID
        TokenId tokenId = response.getReceipt(hederaClient).tokenId;

        return tokenId.toString();
    }
} // Class ends here