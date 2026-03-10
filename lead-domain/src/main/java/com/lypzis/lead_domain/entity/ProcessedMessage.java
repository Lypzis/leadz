package com.lypzis.lead_domain.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_messages", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "message_id" }))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMessage extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    private Instant processedAt = Instant.now();
}
