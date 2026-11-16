package com.volunteergrid.controller;

import com.volunteergrid.dto.ShiftDto;
import com.volunteergrid.entity.User;
import com.volunteergrid.security.SecurityUtils;
import com.volunteergrid.service.EventService;
import com.volunteergrid.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Shift management endpoints (organizer-only creation, all users can view).
 */
@Controller
public class ShiftController {

    private final ShiftService shiftService;
    private final EventService eventService;
    private final SecurityUtils securityUtils;

    public ShiftController(ShiftService shiftService,
                           EventService eventService,
                           SecurityUtils securityUtils) {
        this.shiftService = shiftService;
        this.eventService = eventService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/organizer/events/{eventId}/shifts/new")
    public String newShiftForm(@PathVariable Long eventId, Model model) {
        ShiftDto dto = new ShiftDto();
        dto.setEventId(eventId);
        model.addAttribute("shiftDto", dto);
        model.addAttribute("event", eventService.findById(eventId));
        return "shifts/form";
    }

    @PostMapping("/organizer/events/{eventId}/shifts/new")
    public String createShift(@PathVariable Long eventId,
                              @Valid @ModelAttribute("shiftDto") ShiftDto dto,
                              BindingResult result,
                              RedirectAttributes redirectAttrs,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("event", eventService.findById(eventId));
            return "shifts/form";
        }
        User organizer = securityUtils.getCurrentUser();
        try {
            shiftService.createShift(eventId, dto, organizer);
            redirectAttrs.addFlashAttribute("successMsg", "Shift created successfully!");
            return "redirect:/events/" + eventId;
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("event", eventService.findById(eventId));
            return "shifts/form";
        }
    }

    @GetMapping("/organizer/shifts/{shiftId}/edit")
    public String editShiftForm(@PathVariable Long shiftId, Model model) {
        ShiftDto dto = shiftService.findById(shiftId);
        model.addAttribute("shiftDto", dto);
        model.addAttribute("event", eventService.findById(dto.getEventId()));
        return "shifts/form";
    }

    @PostMapping("/organizer/shifts/{shiftId}/edit")
    public String updateShift(@PathVariable Long shiftId,
                              @Valid @ModelAttribute("shiftDto") ShiftDto dto,
                              BindingResult result,
                              RedirectAttributes redirectAttrs,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("event", eventService.findById(dto.getEventId()));
            return "shifts/form";
        }
        User organizer = securityUtils.getCurrentUser();
        try {
            shiftService.updateShift(shiftId, dto, organizer);
            redirectAttrs.addFlashAttribute("successMsg", "Shift updated!");
            return "redirect:/events/" + dto.getEventId();
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("event", eventService.findById(dto.getEventId()));
            return "shifts/form";
        }
    }

    @PostMapping("/organizer/shifts/{shiftId}/delete")
    public String deleteShift(@PathVariable Long shiftId,
                              @RequestParam Long eventId,
                              RedirectAttributes redirectAttrs) {
        User organizer = securityUtils.getCurrentUser();
        try {
            shiftService.deleteShift(shiftId, organizer);
            redirectAttrs.addFlashAttribute("successMsg", "Shift deleted.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/events/" + eventId;
    }
}
