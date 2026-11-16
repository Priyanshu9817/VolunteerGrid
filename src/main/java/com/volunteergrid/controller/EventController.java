package com.volunteergrid.controller;

import com.volunteergrid.dto.EventDto;
import com.volunteergrid.entity.EventStatus;
import com.volunteergrid.entity.User;
import com.volunteergrid.security.SecurityUtils;
import com.volunteergrid.service.AssignmentService;
import com.volunteergrid.service.EventService;
import com.volunteergrid.service.ReportService;
import com.volunteergrid.service.ShiftService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Event CRUD for organizers, and event browsing for volunteers.
 */
@Controller
public class EventController {

    private final EventService eventService;
    private final ShiftService shiftService;
    private final AssignmentService assignmentService;
    private final ReportService reportService;
    private final SecurityUtils securityUtils;

    public EventController(EventService eventService,
                           ShiftService shiftService,
                           AssignmentService assignmentService,
                           ReportService reportService,
                           SecurityUtils securityUtils) {
        this.eventService = eventService;
        this.shiftService = shiftService;
        this.assignmentService = assignmentService;
        this.reportService = reportService;
        this.securityUtils = securityUtils;
    }

    // ---- Public event listing (accessible to all logged-in users) ----

    @GetMapping("/events")
    public String listEvents(Model model) {
        model.addAttribute("events", eventService.findAll());
        return "events/list";
    }

    @GetMapping("/events/{id}")
    public String viewEvent(@PathVariable Long id, Model model) {
        User currentUser = securityUtils.getCurrentUser();
        model.addAttribute("event", eventService.findById(id));
        model.addAttribute("shifts", shiftService.findByEvent(id));
        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("currentUserRole", currentUser.getRole());

        // Show volunteer's existing registrations for this event
        if (currentUser.getRole().name().equals("VOLUNTEER")) {
            model.addAttribute("myAssignments",
                    assignmentService.findByVolunteer(currentUser.getId())
                            .stream()
                            .filter(a -> a.getEventId().equals(id))
                            .toList());
        }
        return "events/detail";
    }

    // ---- Organizer: Create Event ----

    @GetMapping("/organizer/events/new")
    public String newEventForm(Model model) {
        model.addAttribute("eventDto", new EventDto());
        model.addAttribute("statuses", EventStatus.values());
        return "events/form";
    }

    @PostMapping("/organizer/events/new")
    public String createEvent(@Valid @ModelAttribute("eventDto") EventDto dto,
                              BindingResult result,
                              RedirectAttributes redirectAttrs,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("statuses", EventStatus.values());
            return "events/form";
        }
        User organizer = securityUtils.getCurrentUser();
        try {
            EventDto created = eventService.createEvent(dto, organizer);
            redirectAttrs.addFlashAttribute("successMsg", "Event created successfully!");
            return "redirect:/events/" + created.getId();
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("statuses", EventStatus.values());
            return "events/form";
        }
    }

    // ---- Organizer: Edit Event ----

    @GetMapping("/organizer/events/{id}/edit")
    public String editEventForm(@PathVariable Long id, Model model) {
        model.addAttribute("eventDto", eventService.findById(id));
        model.addAttribute("statuses", EventStatus.values());
        return "events/form";
    }

    @PostMapping("/organizer/events/{id}/edit")
    public String updateEvent(@PathVariable Long id,
                              @Valid @ModelAttribute("eventDto") EventDto dto,
                              BindingResult result,
                              RedirectAttributes redirectAttrs,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("statuses", EventStatus.values());
            return "events/form";
        }
        User organizer = securityUtils.getCurrentUser();
        try {
            eventService.updateEvent(id, dto, organizer);
            redirectAttrs.addFlashAttribute("successMsg", "Event updated successfully!");
            return "redirect:/events/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("statuses", EventStatus.values());
            return "events/form";
        }
    }

    // ---- Organizer: Change Event Status ----

    @PostMapping("/organizer/events/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam EventStatus status,
                               RedirectAttributes redirectAttrs) {
        User organizer = securityUtils.getCurrentUser();
        try {
            eventService.changeStatus(id, status, organizer);
            redirectAttrs.addFlashAttribute("successMsg",
                    "Event status changed to " + status + ".");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/events/" + id;
    }

    // ---- Organizer: Event Report ----

    @GetMapping("/organizer/events/{id}/report")
    public String eventReport(@PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.findById(id));
        model.addAttribute("shifts", shiftService.findByEvent(id));
        model.addAttribute("report", reportService.getEventReport(id));
        model.addAttribute("assignments", assignmentService.findByEvent(id));
        return "events/report";
    }
}
