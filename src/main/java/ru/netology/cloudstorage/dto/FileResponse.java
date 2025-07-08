package ru.netology.cloudstorage.dto;

import lombok.Data;

@Data
public class FileResponse {
    private final String filename;
    private final long size;
}