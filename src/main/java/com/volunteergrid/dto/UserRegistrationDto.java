package com.volunteergrid.dto;

import com.volunteergrid.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO used when registering a new user.
 * Keeps raw password out of the entity layer.
 */
@Data
public class UserRegistrationDto {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be under 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /** For self-registration, this will always be VOLUNTEER. Admins set other roles. */
    @NotNull
    private Role role = Role.VOLUNTEER;
}
