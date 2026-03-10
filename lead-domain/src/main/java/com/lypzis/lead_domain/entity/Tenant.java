package com.lypzis.lead_domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenants")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @Column(nullable = false)
    private String plan;

    @Column(nullable = false)
    private Integer requestsPerMinute;

    @Column(nullable = false)
    private Boolean active = true;
}
