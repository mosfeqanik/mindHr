package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import com.mindhr.entity.enums.PaymentMethodType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_methods")
public class PaymentMethod extends Tenant {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethodType type;

    /** Masked display value such as **** 4242 */
    @Column(name = "masked_details", length = 50)
    private String maskedDetails;

    @Column(name = "gateway_token", length = 255)
    private String gatewayToken;

    @Column(name = "is_default", nullable = false)
    private boolean defaultMethod = false;
}
