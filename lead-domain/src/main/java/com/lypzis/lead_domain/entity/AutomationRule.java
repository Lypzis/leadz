package com.lypzis.lead_domain.entity;

import com.lypzis.lead_contracts.dto.AutomationActionTypeEnum;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "automation_rules", indexes = {
        @Index(name = "idx_rules_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_rules_keyword", columnList = "keyword")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationRule extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    private String keyword;

    @Column(nullable = false)
    private Integer priority;

    @Enumerated(EnumType.STRING)
    private AutomationActionTypeEnum actionType;

    @Column(length = 2000)
    private String actionPayload;
}
