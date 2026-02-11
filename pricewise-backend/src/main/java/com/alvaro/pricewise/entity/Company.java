package com.alvaro.pricewise.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "companies",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "taxId"),
           @UniqueConstraint(columnNames = "companyCode")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 8, updatable = false)
    private String companyCode;

    @Size(max = 50, message = "El tipo de negocio no puede exceder 50 caracteres")
    @Column(length = 50)
    private String businessType;  // retail, ecommerce, etc.

    @Size(max = 20, message = "El CIF/NIF no puede exceder 20 caracteres")
    @Column(length = 20)
    private String taxId;  // CIF/NIF

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanType plan = PlanType.FREE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean sharedStockEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Product> products = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    private void generateCompanyCode() {
        if (this.companyCode == null) {
            this.companyCode = UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8).toUpperCase();
        }
    }

    public enum PlanType {
        FREE,
        PRO,
        ENTERPRISE
    }
}
