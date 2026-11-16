package com.volunteergrid.dto;

import com.volunteergrid.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read-only DTO returned to the UI when displaying user information.
 * Never exposes the password hash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;
}
