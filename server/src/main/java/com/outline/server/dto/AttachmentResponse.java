package com.outline.server.dto;

import com.outline.server.attachment.Attachment;
import java.time.Instant;

public record AttachmentResponse(
        Long id,
        String originalName,
        String contentType,
        long sizeBytes,
        String downloadUrl,
        Instant uploadedAt
) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                "/api/files/" + attachment.getId(),
                attachment.getUploadedAt()
        );
    }
}
