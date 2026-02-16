package com.ApexPay.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;


@Entity
@Data
@Table(name = "transaction_history")
public class TransactionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeName;
    private String hederaTransactionId;
    private Double amountHbar;
    private String status;

    // TransactionRecord.java
    private String hederaAccountId; // Add this field + Getter/Setter

    @Column(length = 1000)
    private String complianceReason;

    private LocalDateTime timestamp;

    // Standard Getters and Setters
}
