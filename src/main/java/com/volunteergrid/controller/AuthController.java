package com.volunteergrid.controller;

import com.volunteergrid.dto.UserRegistrationDto;
import com.volunteergrid.entity.Role;
import com.volunteergrid.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles login, logout (delegated to Spring Security), and registration.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "Invalid email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "You have been logged out successfully.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationDto", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationDto") UserRegistrationDto dto,
                           BindingResult result,
                           RedirectAttributes redirectAttrs,
                           Model model) {
        // Always force self-registration to VOLUNTEER role
        dto.setRole(Role.VOLUNTEER);

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(dto);
            redirectAttrs.addFlashAttribute("successMsg",
                    "Account created! Please log in.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register";
        }
    }
}
