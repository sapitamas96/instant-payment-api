package org.example.payment;

import org.example.payment.dto.PaymentRequest;
import org.example.payment.model.Account;
import org.example.payment.repository.AccountRepository;
import org.example.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class PaymentApplicationTests {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    public void setup() {
        accountRepository.deleteAll();

        Account sender = new Account(1L, new BigDecimal("1000.00"));
        accountRepository.save(sender);

        Account recipient = new Account(2L, new BigDecimal("500.00"));
        accountRepository.save(recipient);
    }

    @Test
    public void testSuccessfulPayment() {
        PaymentRequest request = new PaymentRequest();
        request.setSenderId(1L);
        request.setRecipientId(2L);
        request.setAmount(new BigDecimal("200.00"));

        var response = paymentService.processPayment(request);
        assertNotNull(response.getTransactionId());
        assertEquals("Payment processed successfully", response.getMessage());

        Account updatedSender = accountRepository.findById(1L).orElseThrow();
        Account updatedRecipient = accountRepository.findById(2L).orElseThrow();
        assertEquals(new BigDecimal("800.00"), updatedSender.getBalance());
        assertEquals(new BigDecimal("700.00"), updatedRecipient.getBalance());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(anyString(), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Transaction"));
    }

    @Test
    public void testInsufficientFunds() {
        PaymentRequest request = new PaymentRequest();
        request.setSenderId(1L);
        request.setRecipientId(2L);
        request.setAmount(new BigDecimal("2000.00"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            paymentService.processPayment(request);
        });
        assertTrue(exception.getMessage().contains("Insufficient funds"));

        Account sender = accountRepository.findById(1L).orElseThrow();
        Account recipient = accountRepository.findById(2L).orElseThrow();
        assertEquals(new BigDecimal("1000.00"), sender.getBalance());
        assertEquals(new BigDecimal("500.00"), recipient.getBalance());

        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    public void testInvalidSenderAccount() {
        PaymentRequest request = new PaymentRequest();
        request.setSenderId(999L); // non-existent account
        request.setRecipientId(2L);
        request.setAmount(new BigDecimal("100.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPayment(request);
        });
        assertTrue(exception.getMessage().contains("Sender account not found"));
    }

    @Test
    public void testInvalidRecipientAccount() {
        PaymentRequest request = new PaymentRequest();
        request.setSenderId(1L);
        request.setRecipientId(999L); // non-existent account
        request.setAmount(new BigDecimal("100.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPayment(request);
        });
        assertTrue(exception.getMessage().contains("Recipient account not found"));
    }
}
