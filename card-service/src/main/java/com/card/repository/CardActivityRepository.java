package com.card.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.card.entity.CardActivity;

import java.util.List;

public interface CardActivityRepository extends JpaRepository<CardActivity, Long> {

    List<CardActivity> findByCardIdOrderByCreatedAtDesc(Long cardId);

    List<CardActivity> findByActorIdOrderByCreatedAtDesc(Long actorId);

    List<CardActivity> findTop10ByCardIdOrderByCreatedAtDesc(Long cardId);

    List<CardActivity> findAllByOrderByCreatedAtDesc();

}
