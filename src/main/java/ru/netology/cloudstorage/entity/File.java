package ru.netology.cloudstorage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "files",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "filename"}),
        indexes = @Index(name = "idx_user_filename", columnList = "user_id, filename"))
@Getter
@Setter
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private Integer size;

    @Column(nullable = false, unique = true)
    private String path;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}
