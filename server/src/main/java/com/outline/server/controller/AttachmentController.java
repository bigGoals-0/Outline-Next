package com.outline.server.controller;

import com.outline.server.dto.AttachmentResponse;
import com.outline.server.security.CurrentUser;
import com.outline.server.service.AttachmentService;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping
    AttachmentResponse upload(@RequestPart MultipartFile file) {
        return attachmentService.upload(CurrentUser.get(), file);
    }

    @GetMapping
    List<AttachmentResponse> recent() {
        return attachmentService.recent(CurrentUser.get());
    }

    @GetMapping("/{id}")
    ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = attachmentService.download(CurrentUser.get(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
