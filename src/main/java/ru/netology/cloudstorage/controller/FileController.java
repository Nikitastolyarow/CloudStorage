package ru.netology.cloudstorage.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudstorage.dto.ErrorResponse;
import ru.netology.cloudstorage.dto.FileResponse;
import ru.netology.cloudstorage.entity.User;
import ru.netology.cloudstorage.service.FileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


@RestController
@RequestMapping("/cloud")
public class FileController {

    private final FileService fileService;
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }


    @GetMapping("/list")
    public ResponseEntity<?> listFiles(
            @RequestHeader("auth-token") String authToken,
            @RequestParam(value = "limit", defaultValue = "3") int limit
    ) {
        try {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            List<FileResponse> files = fileService.listFiles(user.getId(), limit);
            return ResponseEntity.ok(files);
        }catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
        }
    }

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String filename,
            @RequestPart MultipartFile file
    ){
        try{
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            fileService.uploadFile(user.getId(),filename,file);
            return ResponseEntity.ok().build();
        } catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
        }
    }


    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
    @RequestHeader("auth-token") String authToken,
    @RequestParam("filename") String filename
        ) {
            try{
                User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                fileService.deleteFile(user.getId(),filename);
                return ResponseEntity.ok().build();
            } catch(IllegalArgumentException e){
                return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
            } catch (IOException e) {
        return ResponseEntity.status(500).body(new ErrorResponse("Ошибка удаления файла", 500));
    }
    }

    @GetMapping("/file")
    public Object downloadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String filename
    ) {
        try {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            InputStreamResource resource = fileService.downloadFile(user.getId(), filename);
            String filePath = fileService.getFilePath(user.getId(), filename);
            Path path = Paths.get(filePath);
            String mimeType = Files.probeContentType(path);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType(mimeType != null ? mimeType : "application/octet-stream"))
                    .contentLength(Files.size(path))
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Ошибка скачивания файла", 500));
        }
    }
    @PutMapping("/file")
    public ResponseEntity<?> renameFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String filename
    ) {
        try {
            String newFilename = "newFile.txt";
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            logger.info("Переименование файла {} в {} для пользователя {}", filename, newFilename, user.getId());
            fileService.renameFile(user.getId(), filename, newFilename);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("Неверный запрос: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
        } catch (IOException e) {
            logger.error("Ошибка при переименовании файла: {}", e.getMessage());
            return ResponseEntity.status(500).body(new ErrorResponse("Ошибка переименования файла", 500));
        }
    }

}