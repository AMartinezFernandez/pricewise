package com.alvaro.pricewise.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_api_keys",
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "provider"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Provider provider;

    @Column(name = "encrypted_key", nullable = false, length = 500)
    private String encryptedKey;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Provider {
        KEEPA
    }
}
