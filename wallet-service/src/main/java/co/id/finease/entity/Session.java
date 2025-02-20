package co.id.finease.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data // Combines @Getter, @Setter, @ToString, @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "session")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    @Column(nullable = false, unique = true, updatable = false)
    private String apiKey;

    @Column(nullable = false, unique = true, updatable = false)
    private String applicationId;

    @Column(nullable = false)
    @Builder.Default // Sets default value in the builder
    private String status = "active";

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdDatetime = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedDatetime = LocalDateTime.now();

    @PreUpdate
    public void updateTimestamp() {
        this.updatedDatetime = LocalDateTime.now();
    }
}
