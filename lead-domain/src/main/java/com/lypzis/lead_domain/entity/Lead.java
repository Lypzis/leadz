package com.lypzis.lead_domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leads", indexes = {
                @Index(name = "idx_leads_tenant_phone", columnList = "tenant,phone"),
                @Index(name = "idx_leads_tenant_status", columnList = "tenant,status")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uk_leads_tenant_phone", columnNames = { "tenant", "phone" })
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseEntity {

        @Column(nullable = false)
        private String tenant;

        @Column(nullable = false)
        private String phone;

        private String campaign;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private LeadStatus status;
}
