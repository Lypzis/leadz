package com.lypzis.lead_worker.entity;

import com.lypzis.lead_contracts.dto.MessageDirectionEnum;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lead_messages", indexes = {
        @Index(name = "idx_lead_messages_lead_created_at", columnList = "lead_id,createdAt"),
        @Index(name = "idx_lead_messages_tenant_message_id", columnList = "tenant,messageId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_lead_messages_tenant_message_id", columnNames = { "tenant", "messageId" })
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadMessage extends BaseEntity {

    @Column(nullable = false)
    private String tenant;

    @Column(nullable = false)
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
