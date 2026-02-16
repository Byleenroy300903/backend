package com.ApexPay.backend.repository;

import com.ApexPay.backend.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // Custom query to find employee by their Hedera ID
    Optional<Employee> findByHederaAccountId(String hederaAccountId);
}