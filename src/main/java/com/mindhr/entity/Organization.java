package com.mindhr.entity;

import com.mindhr.entity.base.BaseEntity;
import com.mindhr.entity.enums.OrganizationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    /** Set after the owner user is created during onboarding. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationStatus status;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @OneToMany(mappedBy = "organization")
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "organization")
    private List<Subscription> subscriptions = new ArrayList<>();
}
