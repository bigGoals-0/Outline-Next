package com.outline.server.repository;

import com.outline.server.attachment.Attachment;
import com.outline.server.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findTop20ByOwnerOrderByUploadedAtDesc(User owner);
}
