package ru.netology.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.netology.cloudstorage.dto.FileResponse;
import ru.netology.cloudstorage.model.File;
import ru.netology.cloudstorage.model.User;
import ru.netology.cloudstorage.repository.FileRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;

    public void uploadFile(String filename, byte[] content, User user) {
        if (fileRepository.findByFilenameAndUser(filename, user).isPresent()) {
            throw new RuntimeException("File already exists");
        }
        File file = new File();
        file.setFilename(filename);
        file.setContent(content);
        file.setSize((long) content.length);
        file.setUser(user);
        fileRepository.save(file);
    }

    public void deleteFile(String filename, User user) {
        fileRepository.deleteByFilenameAndUser(filename, user);
    }

    public File downloadFile(String filename, User user) {
        return fileRepository.findByFilenameAndUser(filename, user)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    public byte[] getFileContent(File file) {
        return file.getContent();
    }

    public void renameFile(String oldFilename, String newFilename, User user) {
        File file = fileRepository.findByFilenameAndUser(oldFilename, user)
                .orElseThrow(() -> new RuntimeException("File not found"));
        file.setFilename(newFilename);
        fileRepository.save(file);
    }

    public List<FileResponse> listFiles(User user, int limit) {
        return fileRepository.findAllByUser(user).stream()
                .limit(limit)
                .map(file -> new FileResponse(file.getFilename(), file.getSize()))
                .toList();
    }
}