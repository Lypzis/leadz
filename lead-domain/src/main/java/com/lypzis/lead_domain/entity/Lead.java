package com.lypzis.lead_domain.entity;

import com.lypzis.lead_contracts.dto.LeadStatusEnum;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leads", indexes = {
                @Index(name = "idx_leads_tenant_id_phone", columnList = "tenant_id,phone"),
                @Index(name = "idx_leads_tenant_id_status", columnList = "tenant_id,status")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uk_leads_tenant_id_phone", columnNames = { "tenant_id", "phone" })
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseEntity {

        @Column(name = "tenant_id", nullable = false)
        private String tenantId;

        @Column(nullable = false)
        private String phone;

        private String campaign;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private LeadStatusEnum status;
}
