package com.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payment.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
  Optional<Subscription> findTopByUserIdOrderByEndDateDesc(Long userId);
  Optional<Subscription> findByRazorpayPaymentId(String razorpayPaymentId);
}