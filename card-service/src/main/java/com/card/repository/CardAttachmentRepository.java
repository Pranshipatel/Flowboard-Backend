package com.card.repository;

import com.card.entity.CardAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardAttachmentRepository extends JpaRepository<CardAttachment, Long> {

    List<CardAttachment> findByCardIdOrderByCreatedAtDesc(Long cardId);

    void deleteByCardId(Long cardId);
}
