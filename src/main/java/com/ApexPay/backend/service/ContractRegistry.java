package com.ApexPay.backend.service;

import org.springframework.stereotype.Service;

@Service
public class ContractRegistry {
    private String activeContractId;

    public void setActiveId(String id) {
        this.activeContractId = id;
        System.out.println("Registry updated with Contract: " + id);
    }

    public String getActiveId() {
        return this.activeContractId;
    }
}