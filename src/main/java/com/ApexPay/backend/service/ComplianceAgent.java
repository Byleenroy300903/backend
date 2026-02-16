package com.ApexPay.backend.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ComplianceAgent {

    @SystemMessage("""
        You are a Global Payroll Compliance Officer. 
        Analyze the employee details and provide a 'COMPLIANT' or 'NON_COMPLIANT' status.
        If NON_COMPLIANT, provide a brief reason why (e.g., tax jurisdiction issues).
        Format: STATUS | REASON
        """)
    String analyzeCompliance(@UserMessage String employeeDetails);
}