package com.flowboard.auth.entity;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer userId;
	@Column(unique = true)
	private String userName;
	@Column(unique = true)
	private String email;
	
	private String passwordHash;
	
	private String fullName;
	
	private String role;
	
	private String avatarUrl;
	
	private String provider;
	
	private Boolean isActive;
	
	private LocalDateTime createAt;
	
	private String bio;
	
	
	
	
}