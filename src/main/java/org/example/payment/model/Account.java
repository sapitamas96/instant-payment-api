package org.example.payment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private Long id;

    @Column(nullable = false)
    private BigDecimal balance;

    @Version
    private Long version;

    public Account(Long id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }
}
