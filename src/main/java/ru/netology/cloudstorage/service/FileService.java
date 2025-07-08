package ru.netology.cloudstorage.service;
import jakarta.transaction.Transactional;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudstorage.dto.FileResponse;
import ru.netology.cloudstorage.entity.File;
import ru.netology.cloudstorage.entity.User;
import ru.netology.cloudstorage.repository.FileRepository;
import ru.netology.cloudstorage.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    @Setter
    private  String storagePath;
    private final UserRepository userRepository;

    public FileService(FileRepository fileRepository, @Value("${app.storage.path:/var/storage}") String storagePath, UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.storagePath = storagePath;
        this.userRepository = userRepository;
        logger.info("Путь к хранилищу инициализирован: {}", storagePath);
    }

    public List<FileResponse> listFiles(Long userId, int limit) {
        logger.info("Список файлов для пользователя {} с ограничением {}", userId, limit);
        if (userId == null) {
            logger.warn("Идентификатор пользователя  null");
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }
        if (limit <= 0 || limit > 1000) {
            logger.warn("Недопустимое предельное значение : {}", limit);
            throw new IllegalArgumentException("Ошибка во входных данных: предел должен быть от 1 до 1000");
        }
        Pageable pageable = PageRequest.of(0, limit);
        List<File> files = fileRepository.findByUserId(userId, pageable);
        List<FileResponse> response = files.stream()
                .map(file -> new FileResponse(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());
        logger.info("Найдены {} файлы для пользователя {}", response.size(), userId);
        return response;
    }
    @Transactional
    public void uploadFile(Long userId, String filename, MultipartFile file) {

        logger.info("Загрузка файла {} для пользователя {}", filename, userId);
        if (userId == null) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }
        if (!isValidFilename(filename)) {
            throw new IllegalArgumentException("Недопустимое имя файла");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пуст");
        }
        if (file.getSize() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Размер файла превышает максимально допустимый размер");
        }
        if (fileRepository.findByUserIdAndFilename(userId, filename).isPresent()) {
            throw new IllegalArgumentException("Файл уже существует");
        }

        String uniqueFilename = System.currentTimeMillis() + "_" + filename;
        Path basePath = Paths.get(storagePath).toAbsolutePath().normalize();
        Path resolvedPath = basePath.resolve(userId.toString()).resolve(uniqueFilename).normalize();

        if (!resolvedPath.startsWith(basePath)) {
            throw new IllegalArgumentException("Неверный путь к файлу");
        }
        if (fileRepository.existsByPath(resolvedPath.toString())) {
            throw new IllegalArgumentException("Путь к файлу уже существует");
        }
        // сохранение файлов
        try {
            Path userDir = resolvedPath.getParent();
            Files.createDirectories(userDir); // Создает C:/Java/CloudStorage/File/
            Files.copy(file.getInputStream(), resolvedPath, StandardCopyOption.REPLACE_EXISTING);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            File entity = new File();
            entity.setFilename(filename);
            entity.setSize((int) file.getSize());
            entity.setPath(resolvedPath.toString());
            entity.setUser(user);
            fileRepository.save(entity);
            logger.info("Файл {} успешно загружен для пользователя {}", filename, userId);
        } catch (IOException e) {
            logger.error("Не удалось сохранить файл:  {}", resolvedPath, e);
            throw new IllegalStateException("Ошибка сохранения файла");
        }

    }

    private boolean isValidFilename(String filename) {
        return filename != null && filename.matches("^[a-zA-Z0-9._-]+$");
    }

    @Transactional
    public void deleteFile(Long userId, String filename) throws IOException {

        File file = fileRepository.findByUserIdAndFilename(userId, filename)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));

        Path filePath = Paths.get(file.getPath());
        Files.deleteIfExists(filePath);
        fileRepository.delete(file);
    }
    @Transactional
    public InputStreamResource downloadFile(Long userId, String filename) throws IOException {
        logger.info("Загрузка файла {} для пользователя {}", filename, userId);
        if (userId == null) {
            throw new IllegalArgumentException("Идентификатор пользователя не может быть пустым");
        }
        if (filename == null || !isValidFilename(filename)) {
            throw new IllegalArgumentException("Недопустимое имя файла");
        }

        File file = fileRepository.findByUserIdAndFilename(userId, filename)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));

        Path filePath = Paths.get(file.getPath()).toAbsolutePath().normalize();
        Path basePath = Paths.get(storagePath).toAbsolutePath().normalize();
        if (!filePath.startsWith(basePath)) {
            throw new IllegalArgumentException("Неверный путь к файлу");
        }
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("Файл не найден на диске");
        }

        return new InputStreamResource(Files.newInputStream(filePath));
    }
    @Transactional
    public void renameFile(Long userId, String oldFilename, String newFilename) throws IOException {
        logger.info("Переименование файла {} в {} для пользователя {}", oldFilename, newFilename, userId);
        if (userId == null || oldFilename == null || newFilename == null) {
            throw new IllegalArgumentException("Параметры не могут быть пустыми");
        }
        if (!isValidFilename(oldFilename) || !isValidFilename(newFilename)) {
            throw new IllegalArgumentException("Недопустимое имя файла");
        }
        if (fileRepository.findByUserIdAndFilename(userId, newFilename).isPresent()) {
            throw new IllegalArgumentException("Файл с новым именем уже существует");
        }
        File file = fileRepository.findByUserIdAndFilename(userId, oldFilename)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));
        Path oldPath = Paths.get(file.getPath());
        Path newPath = oldPath.getParent().resolve(newFilename);
        Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        file.setFilename(newFilename);
        file.setPath(newPath.toString());
        fileRepository.save(file);
        logger.info("Файл {} переименован в {} для пользователя {}", oldFilename, newFilename, userId);
    }


    public String getFilePath(Long userId, String filename) {
        if (userId == null || filename == null || !isValidFilename(filename)) {
            throw new IllegalArgumentException("Недопустимые параметры");
        }
        return fileRepository.findByUserIdAndFilename(userId, filename)
                .map(File::getPath)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));
    }

}