package ru.netology.cloudstorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudstorage.entity.File;
import ru.netology.cloudstorage.entity.User;
import ru.netology.cloudstorage.repository.FileRepository;
import ru.netology.cloudstorage.repository.UserRepository;
import ru.netology.cloudstorage.service.FileService;
import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FileService fileService;

    @Test
    void testUploadFile_success() throws Exception {
        Long userId = 1L;
        String filename = "test.txt";
        byte[] content = "hello".getBytes();


        fileService.setStoragePath("C:/Java/CloudStorage/File");

        // Мокаем MultipartFile
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn((long) content.length);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        // Мокаем User
        User mockUser = new User();
        mockUser.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // Файл с таким именем ещё не существует
        when(fileRepository.findByUserIdAndFilename(userId, filename)).thenReturn(Optional.empty());
        when(fileRepository.existsByPath(anyString())).thenReturn(false);

        // Запуск метода
        fileService.uploadFile(userId, filename, mockFile);

        // Проверка, что save был вызван
        verify(fileRepository, times(1)).save(any(File.class));
    }

}