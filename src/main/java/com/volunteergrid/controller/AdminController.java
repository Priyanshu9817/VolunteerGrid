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
 * Admin panel: user management (create, toggle, role change).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto());
        model.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    @PostMapping("/users/new")
    public String createUser(@Valid @ModelAttribute("userDto") UserRegistrationDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttrs,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }
        try {
            userService.registerUser(dto);
            redirectAttrs.addFlashAttribute("successMsg", "User created successfully.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("roles", Role.values());
            return "admin/user-form";
        }
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            userService.toggleEnabled(id);
            redirectAttrs.addFlashAttribute("successMsg", "User status toggled.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam Role role,
                             RedirectAttributes redirectAttrs) {
        try {
            userService.updateRole(id, role);
            redirectAttrs.addFlashAttribute("successMsg", "Role updated to " + role + ".");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
