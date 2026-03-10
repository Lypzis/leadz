package com.lypzis.lead_worker.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_messages", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant", "message_id" }))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMessage extends BaseEntity {

    private String tenant;

    private String messageId;

    private Instant processedAt = Instant.now();
}
