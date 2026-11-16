package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.ContractStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contracts", indexes = @Index(name = "idx_contracts_organization", columnList = "organization_id"))
public class Contract extends Tenant {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "counterparty_name", length = 150)
    private String counterpartyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContractStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** Wider than payment amounts: client contract values can be large. */
    @Column(name = "contract_value", precision = 14, scale = 2)
    private BigDecimal value;

    @Column(length = 3)
    private String currency;

    @Column(columnDefinition = "text")
    private String terms;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContractHistory> history = new ArrayList<>();
}
