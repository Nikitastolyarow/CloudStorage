package ru.netology.cloudstorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudstorage.entity.File;
import ru.netology.cloudstorage.entity.User;
import ru.netology.cloudstorage.repository.FileRepository;
import ru.netology.cloudstorage.repository.UserRepository;
import ru.netology.cloudstorage.service.FileService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class FileServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("12345");

    @DynamicPropertySource
    static void setDatasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private FileService fileService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    @BeforeEach
    void setUpUser() {
        User user = new User();
        user.setLogin("testuser");
        user.setPasswordHash("12345"); // если у тебя включено шифрование, зашифруй
        user = userRepository.save(user);
        userId = user.getId();
    }

    @Test
    void testUploadFile_success() throws Exception {
        // given
        String filename = "test.txt";
        String fileContent = "hello world";
        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                fileContent.getBytes(StandardCharsets.UTF_8)
        );

        // when
        fileService.uploadFile(userId, filename, multipartFile);

        // then
        Optional<File> savedFileOpt = fileRepository.findByUserIdAndFilename(userId, filename);
        assertTrue(savedFileOpt.isPresent(), "Файл должен быть сохранён в БД");

        File savedFile = savedFileOpt.get();
        assertEquals(filename, savedFile.getFilename());
        assertEquals(fileContent.length(), savedFile.getSize());
        assertNotNull(savedFile.getPath());
    }
}
