package ru.netology.cloudstorage.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudstorage.dto.ErrorResponse;
import ru.netology.cloudstorage.dto.FileRenameRequest;
import ru.netology.cloudstorage.dto.FileResponse;
import ru.netology.cloudstorage.model.File;
import ru.netology.cloudstorage.model.User;
import ru.netology.cloudstorage.service.FileService;
import ru.netology.cloudstorage.service.UserService;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;
    private final UserService userService;

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(@RequestParam("filename") @NotBlank String filename,
                                        @RequestParam("file") MultipartFile file,
                                        Principal principal) throws IOException {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", 2));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("File is empty", 1));
        }
        try {
            User user = userService.findUserByLogin(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            fileService.uploadFile(filename, file.getBytes(), user);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Upload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", 2));
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(@RequestParam("filename") @NotBlank String filename,
                                        Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", 2));
        }
        try {
            User user = userService.findUserByLogin(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            fileService.deleteFile(filename, user);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Delete failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", 2));
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> downloadFile(@RequestParam("filename") @NotBlank String filename,
                                          Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", 2));
        }
        try {
            User user = userService.findUserByLogin(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            File file = fileService.downloadFile(filename, user);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new ByteArrayResource(file.getContent()));
        } catch (RuntimeException e) {
            logger.error("Download failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", 2));
        }
    }

    @PutMapping("/file")
    public ResponseEntity<?> renameFile(@RequestParam("filename") @NotBlank String filename,
                                        @RequestBody FileRenameRequest request,
                                        Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", 2));
        }
        try {
            User user = userService.findUserByLogin(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            fileService.renameFile(filename, request.getName(), user);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Rename failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", 2));
        }
    }

    @GetMapping({"/list", "/cloud/list"}) // Поддержка обоих маршрутов
    public ResponseEntity<?> listFiles(@RequestParam("limit") @Min(1) int limit,
                                       Principal principal) {
        logger.debug("listFiles request: principal={}, limit={}", principal, limit);
        if (principal == null) {
            logger.warn("Unauthorized request to /list");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", 2));
        }
        try {
            User user = userService.findUserByLogin(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            List<FileResponse> files = fileService.listFiles(user, limit);
            logger.info("Returning {} files for user {}", files.size(), principal.getName());
            return ResponseEntity.ok(files);
        } catch (RuntimeException e) {
            logger.error("Error in listFiles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Unauthorized", 2));
        }
    }
}