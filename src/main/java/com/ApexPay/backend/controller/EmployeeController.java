package com.ApexPay.backend.controller;

import com.ApexPay.backend.model.Employee;
import com.ApexPay.backend.model.TransactionRecord;
import com.ApexPay.backend.repository.EmployeeRepository;
import com.ApexPay.backend.service.ContractRegistry;
import com.ApexPay.backend.service.HerderaContractService;
import com.ApexPay.backend.service.MirrorNodeService;
import com.ApexPay.backend.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ApexPay.backend.repository.TransactionRepository;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private PayrollService payrollService;

    @Autowired
    private HerderaContractService contractService;

    @Autowired
    private ContractRegistry contractRegistry;

    @Autowired
    private TransactionRepository transactionRepository;

    // Fetch the deployed contract ID from application.properties
    @Value("${apexpay.contract.id:0.0.7925123}")
    private String vaultId;

    @PostMapping
    public Employee addEmployee(@RequestBody Employee employee) {
        return payrollService.createEmployee(employee);
    }

    @GetMapping
    public List<Employee> listEmployees() {
        return payrollService.getAllEmployees();
    }

    @GetMapping("/{id}/compliance")
    public String checkCompliance(@PathVariable Long id) {
        return payrollService.checkEmployeeCompliance(id);
    }

    /**
     * DEPLOY: Deploys a new vault instance
     */
    @PostMapping("/deploy")
    public ResponseEntity<String> deployVault(@RequestParam(required = false) String aiAddress) {
        try {
            String contractId = contractService.deployVault(aiAddress);
            return ResponseEntity.ok("New Vault Deployed at: " + contractId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Deployment failed: " + e.getMessage());
        }
    }

    /**
     * DEPOSIT: Send HBAR from your wallet to the Smart Contract
     */
    @PostMapping("/vault/deposit")
    public ResponseEntity<String> deposit(@RequestParam String contractId, @RequestParam String employeeId, @RequestParam double amount) {
        try {
            // Change vaultId to contractId (the parameter)
            String status = contractService.depositToVault(contractId, employeeId, amount);
            return ResponseEntity.ok("Deposit Status: " + status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /**
     * PAYOUT: Triggers the Smart Contract to release funds to an employee
     * This is the "Full Flow" endpoint
     */
    @PostMapping("/{id}/pay-via-vault")
    public ResponseEntity<String> payEmployeeViaVault(@PathVariable Long id, @RequestParam double amountHbar) {
        try {
            // 1. Get Employee details from DB (to get their Hedera Wallet Address)
            Employee employee = payrollService.getEmployeeById(id);
            String walletAddress = employee.getHederaAccountId(); // Ensure your Model has this field

            // 2. Call the Smart Contract release function
            String status = contractService.releasePayroll(vaultId, walletAddress, amountHbar);

            return ResponseEntity.ok("Payroll Released! Status: " + status + " to " + walletAddress);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Vault Payout failed: " + e.getMessage());
        }
    }

    // Original direct payment method (kept for your reference)
    @PostMapping("/{id}/pay")
    public String triggerPayment(@PathVariable Long id, @RequestParam double amount) {
        return payrollService.payEmployeeFromDb(id, amount);
    }

    @PostMapping("/vault/approve")
    public ResponseEntity<String> approve(@RequestParam String contractId, @RequestParam String employeeId) {
        try {
            // Change vaultId to contractId (the parameter)
            String status = contractService.approveEmployeePayment(contractId, employeeId);
            return ResponseEntity.ok("AI Approval Status: " + status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Approval failed: " + e.getMessage());
        }
    }

    @GetMapping("/vault/status/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeeVaultStatus(
            @PathVariable String employeeId,
            @RequestParam(required = false) String contractId) {

        // If no ID in URL, get the one from our Registry
        String targetId = (contractId != null) ? contractId : contractRegistry.getActiveId();

        if (targetId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No active contract found. Please deploy or provide an ID."));
        }

        try {
            Map<String, Object> status = contractService.getVaultStatus(targetId, employeeId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/vault/debug")
    public ResponseEntity<String> debug() {
        try {
            contractService.debugPermissions(vaultId);
            return ResponseEntity.ok("Debug info printed to IDE console!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Debug failed: " + e.getMessage());
        }
    }

    @PostMapping("/vault/deploy")
    public ResponseEntity<String> deployVault(@RequestBody Map<String, String> request) {
        try {
            String aiOracleId = request.get("aiOracleId");
            String contractId = contractService.deployContract(aiOracleId);

            // Save it so other endpoints can find it!
            contractRegistry.setActiveId(contractId);

            return ResponseEntity.ok(contractId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Deploy failed: " + e.getMessage());
        }
    }

    @PostMapping("/vault/withdraw")
    public ResponseEntity<String> withdraw(
            @RequestParam String contractId,
            @RequestParam String employeeId,
            @RequestParam(defaultValue = "2.0") double amount) { // Add amount here!
        try {
            // Now you are providing all 3 arguments: ID, Address, and Amount
            String status = contractService.releasePayroll(contractId, employeeId, amount);

            return ResponseEntity.ok("Withdrawal Successful! Status: " + status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Withdrawal failed: " + e.getMessage());
        }
    }

    // Add to EmployeeController.java
    // EmployeeController.java

    @GetMapping("/transactions/{id}")
    public List<TransactionRecord> getHistoryByAccountId(@PathVariable String id) {
        System.out.println("--- LEDGER DEBBUG START ---");
        System.out.println("Fetching history for Account ID: " + id);

        List<TransactionRecord> history = transactionRepository.findByHederaAccountIdOrderByTimestampDesc(id);

        System.out.println("Records found in DB: " + history.size());

        // Print each record details to the console
        history.forEach(record -> {
            System.out.println(String.format(" >> [TX FOUND] Name: %s | Amt: %s | Status: %s | Date: %s",
                    record.getEmployeeName(),
                    record.getAmountHbar(),
                    record.getStatus(),
                    record.getTimestamp()));
        });

        System.out.println("--- LEDGER DEBBUG END ---");
        return history;
    }

    @Autowired
    private MirrorNodeService mirrorNodeService;
    @GetMapping("/transactions/blockchain/{id}")
    public ResponseEntity<List<Map<String, Object>>> getRealHistory(@PathVariable String id) {
        try {
            List<Map<String, Object>> transactions = mirrorNodeService.getFullBlockchainHistory(id);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // EmployeeController.java

    @PutMapping("/{id}")
    public Employee updateEmployee(@PathVariable Long id, @RequestBody Employee details) {
        Employee emp = payrollService.getEmployeeById(id);
        emp.setBaseSalaryUsd(details.getBaseSalaryUsd());
        emp.setTaxCountry(details.getTaxCountry());
        // Note: Usually we don't allow changing Hedera IDs once set to keep history valid
        return payrollService.createEmployee(emp); // save() works for updates too
    }

    @Autowired
    private EmployeeRepository employeeRepository;
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        employeeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

}
