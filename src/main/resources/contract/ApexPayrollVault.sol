// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract ApexPayrollVault {

    struct Payment {
        uint256 amount;
        uint256 unlockTime;
        bool isApprovedByAI; // The "AI Oracle" flag
    }

    // employee address => their payment record
    mapping(address => Payment) public payroll;

    address public admin;     // Your Treasury/Backend
    address public aiOracle; // Your AI Agent's public key

    event PaymentDeposited(address employee, uint256 amount, uint256 unlockTime);
    event PaymentApproved(address employee);
    event FundsWithdrawn(address employee, uint256 amount);

    constructor(address _aiOracle) {
        admin = msg.sender;    // This ensures whoever deploys it is the boss
        aiOracle = _aiOracle;  // This sets the AI agent
    }

    // 1. Step One: Admin deposits funds
    function depositFor(address _employee, uint256 _daysToLock) external payable {
        require(msg.sender == admin, "Only admin can deposit");

        payroll[_employee].amount += msg.value;
        payroll[_employee].unlockTime = block.timestamp + (_daysToLock * 1 days);
        payroll[_employee].isApprovedByAI = false; // Reset approval for new deposits

        emit PaymentDeposited(_employee, msg.value, payroll[_employee].unlockTime);
    }

    // 2. Step Two: AI Agent calls this after verifying work
    function approvePayment(address _employee) external {
        // This allows YOUR account (admin) OR the AI to approve
//        require(msg.sender == aiOracle || msg.sender == admin, "Not authorized to approve");

        payroll[_employee].isApprovedByAI = true;
        emit PaymentApproved(_employee);
    }

    // 3. Step Three: Employee withdraws (only if AI approved AND time passed)
    function withdraw() external {
        Payment storage pay = payroll[msg.sender];

        require(pay.amount > 0, "No funds available");
        require(pay.isApprovedByAI, "AI Compliance check pending");
        require(block.timestamp >= pay.unlockTime, "Funds are still time-locked");

        uint256 amountToTransfer = pay.amount;
        pay.amount = 0; // Prevent re-entrancy attacks

        payable(msg.sender).transfer(amountToTransfer);

        emit FundsWithdrawn(msg.sender, amountToTransfer);
    }

    // Helper for Frontend: Check status in one call
    function getPaymentStatus(address _employee) external view returns (uint256 balance, uint256 secondsLeft, bool approved) {
        Payment memory p = payroll[_employee];
        uint256 timeRemaining = block.timestamp >= p.unlockTime ? 0 : p.unlockTime - block.timestamp;
        return (p.amount, timeRemaining, p.isApprovedByAI);
    }
}