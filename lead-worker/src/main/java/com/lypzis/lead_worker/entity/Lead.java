package com.lypzis.lead_worker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leads", uniqueConstraints = {
        @UniqueConstraint(name = "uk_message_id", columnNames = "messageId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String messageId;

    private String phone;

    @Column(length = 1000)
    private String message;

    private String campaign;

}