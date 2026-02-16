package com.ApexPay.backend.service;

import com.ApexPay.backend.model.Employee;
import com.ApexPay.backend.model.TransactionRecord;
import com.ApexPay.backend.repository.EmployeeRepository;
import com.ApexPay.backend.repository.TransactionRepository;
import com.hedera.hashgraph.sdk.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hedera.hashgraph.sdk.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;
import java.math.BigDecimal;

import java.util.List;

@Service
public class PayrollService {

    @Autowired
    private Client hederaClient;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ComplianceAgent complianceAgent;

    @Autowired
    private TransactionRepository transactionRepository; // You'll need to create this interface


    public String verifyConnection() throws Exception {
        AccountBalance balance = new AccountBalanceQuery()
                .setAccountId(hederaClient.getOperatorAccountId())
                .execute(hederaClient);
        return "Connected to Hedera Testnet. Treasury Balance: " + balance.hbars;
    }

    @Transactional
    public Employee createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public String checkEmployeeCompliance(Long id) {
        return employeeRepository.findById(id).map(emp -> {
            String details = String.format("Employee: %s, Country: %s, Salary: %s",
                    emp.getFullName(), emp.getTaxCountry(), emp.getBaseSalaryUsd());

            // Calling the AI Agent
            return complianceAgent.analyzeCompliance(details);
        }).orElse("Employee not found");
    }

    public String processPayment(String employeeAccountId, double amountHbar) {
        try {
            // Convert the double to BigDecimal to satisfy the Hedera SDK
            BigDecimal amount = BigDecimal.valueOf(amountHbar);

            TransferTransaction transaction = new TransferTransaction()
                    .addHbarTransfer(hederaClient.getOperatorAccountId(), Hbar.from(amount).negated())
                    .addHbarTransfer(AccountId.fromString(employeeAccountId), Hbar.from(amount));

            TransactionResponse response = transaction.execute(hederaClient);
            TransactionReceipt receipt = response.getReceipt(hederaClient);

            return "Payment successful! Status: " + receipt.status;

        } catch (Exception e) {
            return "Payment failed: " + e.getMessage();
        }
    }

    public String payEmployeeFromDb(Long id, double amountHbar) {
        return employeeRepository.findById(id).map(emp -> {
            // Automatically grab the Hedera ID from the Postgres record
            String hederaId = emp.getHederaAccountId();
            if (hederaId == null || hederaId.isEmpty()) {
                return "Error: Employee has no Hedera Account ID linked.";
            }
            return processPayment(hederaId, amountHbar);
        }).orElse("Employee with ID " + id + " not found.");
    }


    public String processPaymentWithLogging(Long employeeId, double amountHbar, String aiReason) {
        Employee emp = employeeRepository.findById(employeeId).orElseThrow();

        try {
            // 1. Execute Hedera Transfer
            BigDecimal amount = BigDecimal.valueOf(amountHbar);
            TransferTransaction transaction = new TransferTransaction()
                    .addHbarTransfer(hederaClient.getOperatorAccountId(), Hbar.from(amount).negated())
                    .addHbarTransfer(AccountId.fromString(emp.getHederaAccountId()), Hbar.from(amount));

            TransactionResponse response = transaction.execute(hederaClient);
            TransactionReceipt receipt = response.getReceipt(hederaClient);

            // 2. Log to Database on SUCCESS
            saveTransactionLog(emp, amountHbar, response.transactionId.toString(), "SUCCESS", aiReason);

            return "Payment Successful! Tx ID: " + response.transactionId;

        } catch (Exception e) {
            // 3. Log even on FAILURE so you know what went wrong
            saveTransactionLog(emp, amountHbar, "N/A", "FAILED: " + e.getMessage(), aiReason);
            return "Payment Failed: " + e.getMessage();
        }
    }

    private void saveTransactionLog(Employee emp, double amount, String txId, String status, String reason) {
        TransactionRecord record = new TransactionRecord();
        record.setEmployeeName(emp.getFullName());
        record.setHederaTransactionId(txId);
        record.setAmountHbar(amount);
        record.setStatus(status);
        record.setComplianceReason(reason);
        record.setTimestamp(LocalDateTime.now());
        transactionRepository.save(record);
    }

    public String createCompanyToken() throws Exception {
        TokenCreateTransaction transaction = new TokenCreateTransaction()
                .setTokenName("ApexPay Governance")
                .setTokenSymbol("APEX")
                .setDecimals(2)
                .setInitialSupply(1000000) // 1 Million tokens
                .setTreasuryAccountId(hederaClient.getOperatorAccountId())
                .setAdminKey(hederaClient.getOperatorPublicKey())
                .setSupplyKey(hederaClient.getOperatorPublicKey())
                .freezeWith(hederaClient);

        TransactionResponse response = transaction.execute(hederaClient);
        TokenId tokenId = response.getReceipt(hederaClient).tokenId;

        return "Token Created! ID: " + tokenId.toString();
    }

    public String scheduleCompliancePayment(String employeeId, double amountHbar) throws Exception {
        // 1. Create the transfer we WANT to happen
        TransferTransaction transfer = new TransferTransaction()
                .addHbarTransfer(hederaClient.getOperatorAccountId(), Hbar.from(BigDecimal.valueOf(amountHbar)).negated())
                .addHbarTransfer(AccountId.fromString(employeeId), Hbar.from(BigDecimal.valueOf(amountHbar)));

        // 2. Schedule it instead of executing it
        ScheduleCreateTransaction scheduleTx = new ScheduleCreateTransaction()
                .setScheduledTransaction(transfer)
                .setAdminKey(hederaClient.getOperatorPublicKey())
                .setPayerAccountId(hederaClient.getOperatorAccountId());

        TransactionResponse response = scheduleTx.execute(hederaClient);
        ScheduleId scheduleId = response.getReceipt(hederaClient).scheduleId;

        // 3. (Behind the scenes) Your AI Agent reviews and calls 'ScheduleSignTransaction'
        return scheduleId.toString();
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));
    }



    public String withdraw(String contractId, String employeeId, Double amount) {
        try {
            // ... Your existing Hedera logic ...

            // 1. FIND THE EMPLOYEE NAME (so we can link the history)
            Employee emp = employeeRepository.findByHederaAccountId(employeeId)
                    .orElse(null);
            String empName = (emp != null) ? emp.getFullName() : "UNKNOWN_NODE";

            // 2. CREATE THE PERMANENT RECORD
            TransactionRecord record = new TransactionRecord();
            record.setEmployeeName(empName);
            record.setHederaAccountId(employeeId); // Save the ID here!
            record.setAmountHbar(amount);
            record.setStatus("SUCCESS");
            record.setTimestamp(LocalDateTime.now());
            record.setHederaTransactionId("0.0." + System.currentTimeMillis()); // Mock ID or real one

            // 3. SAVE TO DATABASE
            transactionRepository.save(record);

            return "SUCCESS";
        } catch (Exception e) {
            // Save a failure record too!
            return "FAIL";
        }
    }



}