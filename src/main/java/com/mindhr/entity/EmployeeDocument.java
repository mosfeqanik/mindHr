package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employee_documents", indexes = @Index(name = "idx_employee_documents_organization", columnList = "organization_id"))
public class EmployeeDocument extends Tenant {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "issued_at", nullable = true)
    private LocalDate issuedAt;

    @Column(name = "note", nullable = true, length = 500)
    private String note;

}
