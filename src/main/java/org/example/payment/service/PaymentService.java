package org.example.payment.service;

import org.example.payment.dto.PaymentRequest;
import org.example.payment.dto.PaymentResponse;
import org.example.payment.exception.InsufficientFundsException;
import org.example.payment.model.Account;
import org.example.payment.model.Transaction;
import org.example.payment.repository.AccountRepository;
import org.example.payment.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private static final String TRANSACTION_TOPIC = "transaction.notifications";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        // Lock accounts to handle concurrency
        Account sender = accountRepository.findByIdForUpdate(request.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Sender account not found"));

        Account recipient = accountRepository.findByIdForUpdate(request.getRecipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient account not found"));

        BigDecimal amount = request.getAmount();

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        recipient.setBalance(recipient.getBalance().add(amount));

        accountRepository.save(sender);
        accountRepository.save(recipient);

        Transaction transaction = new Transaction();
        transaction.setSenderId(sender.getId());
        transaction.setRecipientId(recipient.getId());
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());

        Transaction savedTransaction = transactionRepository.save(transaction);

        String message = "Transaction " + savedTransaction.getId() + " completed: Sender "
                + sender.getId() + " sent " + amount + " to Recipient " + recipient.getId();
        kafkaTemplate.send(TRANSACTION_TOPIC, message);

        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(savedTransaction.getId());
        response.setMessage("Payment processed successfully");
        return response;
    }
}
