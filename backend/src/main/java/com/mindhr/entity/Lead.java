package com.mindhr.entity;

import com.mindhr.entity.base.BaseEntity;
import com.mindhr.entity.enums.LeadStage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "leads")
public class Lead extends BaseEntity {

    /** Sales staff member owning this lead. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    /** Set once the lead reaches CONVERTED and an organization is created. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_organization_id")
    private Organization convertedOrganization;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "contact_name", nullable = false, length = 150)
    private String contactName;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadStage stage;

    @Column(length = 50)
    private String source;

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesActivity> activities = new ArrayList<>();
}
