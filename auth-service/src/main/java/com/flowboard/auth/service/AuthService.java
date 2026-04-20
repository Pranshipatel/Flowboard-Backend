package com.flowboard.auth.service;





import java.util.List;

import com.flowboard.auth.dto.ChangePasswordDto;
import com.flowboard.auth.dto.RegisterResponseDto;
import com.flowboard.auth.dto.RegisterUserDto;
import com.flowboard.auth.dto.UpdateUserProfileDto;
import com.flowboard.auth.dto.UserProfileDto;
import com.flowboard.auth.entity.User;

public interface AuthService {


	RegisterResponseDto register(RegisterUserDto registerUserDto);

    String login(String email, String password);

    void logout(String token);

    String refreshToken(String token);


    UserProfileDto getUserById(Integer userId);

    UserProfileDto updateProfile(Integer userId, UpdateUserProfileDto dto);

    void changePassword(Integer userId, ChangePasswordDto dto);

    List<User> searchUsers(String username);


	
	     
}