package com.lypzis.lead_worker.entity;

import com.lypzis.lead_contracts.dto.AutomationActionTypeEnum;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "automation_rules", indexes = {
        @Index(name = "idx_rules_tenant", columnList = "tenant"),
        @Index(name = "idx_rules_keyword", columnList = "keyword")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationRule extends BaseEntity {

    private String tenant;

    private String keyword;

    @Column(nullable = false)
    private Integer priority;

    @Enumerated(EnumType.STRING)
    private AutomationActionTypeEnum actionType;

    @Column(length = 2000)
    private String actionPayload;
}
