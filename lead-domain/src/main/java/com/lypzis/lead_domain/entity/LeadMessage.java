package com.lypzis.lead_domain.entity;

import com.lypzis.lead_contracts.dto.MessageDirectionEnum;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lead_messages", indexes = {
        @Index(name = "idx_lead_messages_lead_created_at", columnList = "lead_id,created_at"),
        @Index(name = "idx_lead_messages_tenant_id_message_id", columnList = "tenant_id,message_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_lead_messages_tenant_id_message_id", columnNames = { "tenant_id", "message_id" })
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadMessage extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageDirectionEnum direction;

    @Column(nullable = false, length = 2000)
    private String content;
}
