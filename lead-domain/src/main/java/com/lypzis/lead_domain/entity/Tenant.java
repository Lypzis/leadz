package com.lypzis.lead_domain.entity;

import com.lypzis.lead_contracts.dto.TenantPlanEnum;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenants")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @Column(unique = true)
    private String whatsappPhoneNumberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantPlanEnum plan;

    @Column(nullable = false)
    private Integer requestsPerMinute;

    @Column(nullable = false)
    private Boolean active = true;
}
