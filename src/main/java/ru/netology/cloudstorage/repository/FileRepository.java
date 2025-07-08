package ru.netology.cloudstorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloudstorage.entity.File;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByUserId(Long userId, Pageable pageable);
    Optional<File> findByUserIdAndFilename(Long userId, String filename);
    boolean existsByPath(String path);
}