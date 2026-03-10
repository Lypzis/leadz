package com.lypzis.lead_worker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leads", uniqueConstraints = {
        @UniqueConstraint(name = "uk_message_id", columnNames = { "tenant", "message_id" })
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseEntity {

    @Column(name = "message_id")
    private String messageId;

    private String phone;

    @Column(length = 1000)
    private String message;

    private String campaign;

    private String tenant;

}
