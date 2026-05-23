package com.outline.server.service;

import com.outline.server.attachment.Attachment;
import com.outline.server.config.StorageProperties;
import com.outline.server.dto.AttachmentResponse;
import com.outline.server.exception.ApiException;
import com.outline.server.repository.AttachmentRepository;
import com.outline.server.user.User;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {
    private final AttachmentRepository attachments;
    private final Path uploadDir;

    public AttachmentService(AttachmentRepository attachments, StorageProperties properties) {
        this.attachments = attachments;
        this.uploadDir = Path.of(properties.uploadDir() == null ? "uploads" : properties.uploadDir());
    }

    @Transactional
    public AttachmentResponse upload(User owner, MultipartFile file) {
        try {
            Files.createDirectories(uploadDir);
            String safeName = UUID.randomUUID() + "-" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = uploadDir.resolve(safeName);
            file.transferTo(target);
            Attachment attachment = new Attachment();
            attachment.setOwner(owner);
            attachment.setOriginalName(file.getOriginalFilename());
            attachment.setContentType(file.getContentType());
            attachment.setSizeBytes(file.getSize());
            attachment.setStoragePath(target.toAbsolutePath().toString());
            attachments.save(attachment);
            return AttachmentResponse.from(attachment);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }
    }

    public Resource download(User user, Long id) {
        Attachment attachment = attachments.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found"));
        try {
            return new UrlResource(Path.of(attachment.getStoragePath()).toUri());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "File not available");
        }
    }

    public List<AttachmentResponse> recent(User user) {
        return attachments.findTop20ByOwnerOrderByUploadedAtDesc(user).stream().map(AttachmentResponse::from).toList();
    }
}
