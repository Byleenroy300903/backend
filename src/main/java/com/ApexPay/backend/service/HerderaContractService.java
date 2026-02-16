package com.ApexPay.backend.service;

import com.ApexPay.backend.model.Employee;
import com.ApexPay.backend.repository.TransactionRepository;
import com.hedera.hashgraph.sdk.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import com.ApexPay.backend.repository.EmployeeRepository;


import java.math.BigDecimal; // Add this import
import java.math.BigInteger; // Add this import

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class HerderaContractService {

    @Autowired
    private Client hederaClient;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Deploys the ApexPayrollVault contract to the Hedera Testnet.
     * @return The new ContractId as a String
     */
    @Value("${apexpay.ai.oracle.id}")
    private String defaultOracleId;

    // You can now make the parameter optional
    public String deployVault(String aiAddress) throws Exception {

        // If no address is passed to the method, use the one from properties
        String oracleToUse = (aiAddress != null && !aiAddress.isEmpty())
                ? aiAddress
                : defaultOracleId;

        ClassPathResource resource = new ClassPathResource("contract/ApexPayrollVault.bin");
        byte[] bdata = FileCopyUtils.copyToByteArray(resource.getInputStream());
        String bytecodeHex = new String(bdata, StandardCharsets.UTF_8).trim();

        String solidityOracleAddr = AccountId.fromString(oracleToUse).toSolidityAddress();

        System.out.println("DEBUG: Bytecode length is: " + bytecodeHex.length());
        String myAddress = hederaClient.getOperatorAccountId().toSolidityAddress();

        ContractCreateFlow flow = new ContractCreateFlow()
                .setBytecode(bytecodeHex)
                .setGas(2_000_000)
                .setConstructorParameters(new ContractFunctionParameters()
                        .addAddress(myAddress));

        TransactionResponse response = flow.execute(hederaClient);
        return response.getReceipt(hederaClient).contractId.toString();
    }

    // 1. Method to fund the vault
    public String depositToVault(String contractId, String employeeId, double amountHbar) throws Exception {
        // 1. Force the employee ID to a clean Solidity Address
        String employeeSolidityAddr = AccountId.fromString(employeeId).toSolidityAddress();

        ContractExecuteTransaction transaction = new ContractExecuteTransaction()
                .setContractId(ContractId.fromString(contractId))
                .setGas(300_000)
                .setPayableAmount(Hbar.from(BigDecimal.valueOf(amountHbar)))
                // Make sure "depositFor" is the exact function name in Solidity
                .setFunction("depositFor", new ContractFunctionParameters()
                        .addAddress(employeeSolidityAddr) // Ensure this is the SAME conversion as approve
                        .addUint256(java.math.BigInteger.ZERO));

        TransactionResponse response = transaction.execute(hederaClient);
        return response.getReceipt(hederaClient).status.toString();
    }


    // 2. Method to pay an employee (The AI Oracle Trigger)

    public String releasePayroll(String contractId, String employeeAddress, double amountHbar) throws Exception {
        // 1. Convert Hedera ID (0.0.x) to Solidity Address for the contract
        String employeeSolidityAddr = AccountId.fromString(employeeAddress).toSolidityAddress();

        ContractExecuteTransaction transaction = new ContractExecuteTransaction()
                .setContractId(ContractId.fromString(contractId))
                .setGas(300_000)
                .setFunction("releaseFunds", new ContractFunctionParameters()
                        .addAddress(employeeSolidityAddr)
                        .addUint256(BigInteger.valueOf((long) (amountHbar * 100_000_000))));

        TransactionResponse response = transaction.execute(hederaClient);
        String status = response.getReceipt(hederaClient).status.toString();

        // 2. NEW: If the blockchain transaction succeeded, record it in our local DB
        if (status.equalsIgnoreCase("SUCCESS")) {
            saveToLocalHistory(employeeAddress, amountHbar, response.transactionId.toString());
        }

        return status;
    }

    // Helper method to handle the database persistence
    private void saveToLocalHistory(String hAccountId, double amount, String txId) {
        // Attempt to find the employee to get their Full Name
        Employee emp = employeeRepository.findByHederaAccountId(hAccountId).orElse(null);

        com.ApexPay.backend.model.TransactionRecord record = new com.ApexPay.backend.model.TransactionRecord();

        record.setEmployeeName(emp != null ? emp.getFullName() : "UNKNOWN_NODE");
        record.setHederaAccountId(hAccountId);
        record.setAmountHbar(amount);
        record.setStatus("SUCCESS");
        record.setTimestamp(LocalDateTime.now());
        record.setHederaTransactionId(txId);

        transactionRepository.save(record);
    }

    public String approveEmployeePayment(String contractId, String employeeId) throws Exception {
        // 1. Convert the ID to a hex address
        String solidityAddress = AccountId.fromString(employeeId).toSolidityAddress();

        // 2. Debug lines (The ones you were trying to add)
        System.out.println("--- APPROVAL DEBUG ---");
        System.out.println("Using Contract: " + contractId);
        System.out.println("Target Employee (Hedera): " + employeeId);
        System.out.println("Target Employee (Solidity Hex): " + solidityAddress);
        System.out.println("Operator/Oracle Hex: " + hederaClient.getOperatorAccountId().toSolidityAddress());

        // 3. The actual contract call
        ContractExecuteTransaction transaction = new ContractExecuteTransaction()
                .setContractId(ContractId.fromString(contractId))
                .setGas(400_000)
                .setFunction("approvePayment", new ContractFunctionParameters()
                        .addAddress(solidityAddress));

        TransactionResponse response = transaction.execute(hederaClient);
        return response.getReceipt(hederaClient).status.toString();
    }

    public String withdrawFunds(String contractId) throws Exception {
        // Note: This must be called by the client using the EMPLOYEE'S keys
        // For testing, if you use your Admin client, it will only work
        // if the Admin also has a payroll balance in the contract.
        ContractExecuteTransaction transaction = new ContractExecuteTransaction()
                .setContractId(ContractId.fromString(contractId))
                .setGas(300_000)
                .setFunction("withdraw");

        TransactionResponse response = transaction.execute(hederaClient);
        return response.getReceipt(hederaClient).status.toString();
    }

    public Map<String, Object> getVaultStatus(String contractId, String employeeId) throws Exception {
        String employeeSolidityAddr = AccountId.fromString(employeeId).toSolidityAddress();

        ContractCallQuery query = new ContractCallQuery()
                .setContractId(ContractId.fromString(contractId))
                .setGas(100_000)
                .setFunction("getPaymentStatus", new ContractFunctionParameters()
                        .addAddress(employeeSolidityAddr));

        // Execute the query to get the raw result
        ContractFunctionResult result = query.execute(hederaClient);

        // Solidity: returns (uint256 balance, uint256 secondsLeft, bool approved)
        // We extract them by index (0, 1, 2)
        java.math.BigInteger balanceTinybars = result.getUint256(0);
        java.math.BigInteger secondsLeft = result.getUint256(1);
        boolean isApproved = result.getBool(2);

        // Convert to a Map so the Controller can send it as JSON
        Map<String, Object> statusMap = new java.util.HashMap<>();
        statusMap.put("balanceHbar", balanceTinybars.doubleValue() / 100_000_000.0);
        statusMap.put("secondsRemaining", secondsLeft.longValue());
        statusMap.put("isApprovedByAI", isApproved);

        return statusMap;
    }

    public void debugPermissions(String contractId) throws Exception {
        // 1. Query the 'admin' variable from Solidity
        ContractCallQuery adminQuery = new ContractCallQuery()
                .setContractId(ContractId.fromString(contractId))
                .setGas(100_000)
                .setFunction("admin");

        // 2. Query the 'aiOracle' variable from Solidity
        ContractCallQuery oracleQuery = new ContractCallQuery()
                .setContractId(ContractId.fromString(contractId))
                .setGas(100_000)
                .setFunction("aiOracle");

        // 3. Get your own address (the one Java is using)
        String actualAdmin = adminQuery.execute(hederaClient).getAddress(0);
        String actualOracle = oracleQuery.execute(hederaClient).getAddress(0);
        String myAddress = hederaClient.getOperatorAccountId().toSolidityAddress();

        System.out.println("==========================================");
        System.out.println("DEBUG: CONTRACT PERMISSIONS");
        System.out.println("Contract Admin (Hex):  " + actualAdmin);
        System.out.println("Contract Oracle (Hex): " + actualOracle);
        System.out.println("Your App Address (Hex): " + myAddress);
        System.out.println("Match Admin? " + actualAdmin.equalsIgnoreCase(myAddress));
        System.out.println("Match Oracle? " + actualOracle.equalsIgnoreCase(myAddress));
        System.out.println("==========================================");
       }

    public String deployContract(String aiOracleId) throws Exception {
        // 1. Read the bytecode from the .bin file in src/main/resources
        ClassPathResource resource = new ClassPathResource("ApexPayrollVault.bin");
        String bytecode = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        // 2. Convert the AI Oracle ID to Solidity format for the Constructor
        String oracleAddress = AccountId.fromString(aiOracleId).toSolidityAddress();

        // 3. Deploy the contract
        // ContractCreateFlow is the modern way to deploy on Hedera
        ContractCreateFlow flow = new ContractCreateFlow()
                .setBytecode(bytecode)
                .setGas(1_500_000) // Deployment requires significant gas
                .setConstructorParameters(new ContractFunctionParameters()
                        .addAddress(oracleAddress));

        // Execute and wait for the receipt
        TransactionResponse response = flow.execute(hederaClient);
        TransactionReceipt receipt = response.getReceipt(hederaClient);

        if (receipt.contractId == null) {
            throw new Exception("Deployment failed: Contract ID not found in receipt.");
        }

        String newContractId = receipt.contractId.toString();
        System.out.println("==========================================");
        System.out.println("NEW CONTRACT DEPLOYED: " + newContractId);
        System.out.println("==========================================");

        return newContractId;
    }



}