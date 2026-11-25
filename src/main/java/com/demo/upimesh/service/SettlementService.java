package com.demo.upimesh.service;

import com.demo.upimesh.model.Account;
import com.demo.upimesh.model.AccountRepository;
import com.demo.upimesh.model.Transaction;
import com.demo.upimesh.model.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SettlementService {
    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public SettlementService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @Transactional
    public Transaction settle(String sender, String receiver, BigDecimal amount, String packetHash) {
        Account senderAccount = accounts.findByAccountName(sender)
                .orElseThrow(() -> new IllegalArgumentException("Unknown sender: " + sender));
        Account receiverAccount = accounts.findByAccountName(receiver)
                .orElseThrow(() -> new IllegalArgumentException("Unknown receiver: " + receiver));

        if (senderAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
        receiverAccount.setBalance(receiverAccount.getBalance().add(amount));

        try {
            return transactions.saveAndFlush(new Transaction(sender, receiver, amount, packetHash));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateSettlementException("Packet hash already settled", ex);
        }
    }

    public static class DuplicateSettlementException extends RuntimeException {
        public DuplicateSettlementException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
