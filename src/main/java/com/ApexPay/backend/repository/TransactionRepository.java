package com.ApexPay.backend.repository;

import com.ApexPay.backend.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    // You can add custom queries later, like findByEmployeeName()
    List<TransactionRecord> findByEmployeeNameOrderByTimestampDesc(String employeeName);
    // TransactionRepository.java
    List<TransactionRecord> findByHederaAccountIdOrderByTimestampDesc(String hederaAccountId);
}