//package com.ApexPay.backend;
//
//
//import com.ApexPay.backend.service.HerderaContractService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
////@Component
//public class DeployRunner implements CommandLineRunner {
//
//    @Autowired
//    private HerderaContractService contractService;
//
//    @Override
//    public void run(String... args) throws Exception {
//        System.out.println("Runner starting deployment using properties configuration...");
//
//        try {
//            // We pass 'null' so the service uses the default from application.properties
//            String id = contractService.deployVault(null);
//
//            System.out.println("-------------------------------------------");
//            System.out.println("VAULT DEPLOYED: " + id);
//            System.out.println("-------------------------------------------");
//        }catch (Exception e) {
//        System.err.println("Deployment failed!");
//        e.printStackTrace(); // This will print the full technical error in RED
//    }
//    }
//}