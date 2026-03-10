package com.lypzis.lead_worker.entity;

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

    @Column(length = 1000)
    private String responseMessage;
}
