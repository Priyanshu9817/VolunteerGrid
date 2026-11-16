package com.volunteergrid.controller;

import com.volunteergrid.entity.User;
import com.volunteergrid.security.SecurityUtils;
import com.volunteergrid.service.AssignmentService;
import com.volunteergrid.service.AttendanceService;
import com.volunteergrid.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles volunteer registration for shifts, withdrawal, approval/rejection,
 * and attendance management.
 */
@Controller
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AttendanceService attendanceService;
    private final ReportService reportService;
    private final SecurityUtils securityUtils;

    public AssignmentController(AssignmentService assignmentService,
                                AttendanceService attendanceService,
                                ReportService reportService,
                                SecurityUtils securityUtils) {
        this.assignmentService = assignmentService;
        this.attendanceService = attendanceService;
        this.reportService = reportService;
        this.securityUtils = securityUtils;
    }

    // ---- Volunteer: Register for a shift ----

    @PostMapping("/volunteer/shifts/{shiftId}/register")
    public String register(@PathVariable Long shiftId,
                           @RequestParam Long eventId,
                           RedirectAttributes redirectAttrs) {
        User volunteer = securityUtils.getCurrentUser();
        try {
            assignmentService.register(shiftId, volunteer);
            redirectAttrs.addFlashAttribute("successMsg",
                    "Successfully registered! Awaiting organizer approval.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/events/" + eventId;
    }

    // ---- Volunteer: Withdraw from a shift ----

    @PostMapping("/volunteer/assignments/{assignmentId}/withdraw")
    public String withdraw(@PathVariable Long assignmentId,
                           RedirectAttributes redirectAttrs) {
        User volunteer = securityUtils.getCurrentUser();
        try {
            assignmentService.withdraw(assignmentId, volunteer);
            redirectAttrs.addFlashAttribute("successMsg", "Registration withdrawn.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/volunteer/my-assignments";
    }

    // ---- Volunteer: My Assignments ----

    @GetMapping("/volunteer/my-assignments")
    public String myAssignments(Model model) {
        User volunteer = securityUtils.getCurrentUser();
        model.addAttribute("assignments",
                assignmentService.findByVolunteer(volunteer.getId()));
        model.addAttribute("summary",
                reportService.getVolunteerSummary(
                        volunteer.getId(), volunteer.getName()));
        return "assignments/my-assignments";
    }

    // ---- Organizer: View registrations for a shift ----

    @GetMapping("/organizer/shifts/{shiftId}/assignments")
    public String shiftAssignments(@PathVariable Long shiftId, Model model) {
        model.addAttribute("assignments", assignmentService.findByShift(shiftId));
        return "assignments/shift-assignments";
    }

    // ---- Organizer: Approve / Reject assignment ----

    @PostMapping("/organizer/assignments/{assignmentId}/approve")
    public String approve(@PathVariable Long assignmentId,
                          @RequestParam Long shiftId,
                          RedirectAttributes redirectAttrs) {
        User organizer = securityUtils.getCurrentUser();
        try {
            assignmentService.approve(assignmentId, organizer);
            redirectAttrs.addFlashAttribute("successMsg", "Assignment approved.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/organizer/shifts/" + shiftId + "/assignments";
    }

    @PostMapping("/organizer/assignments/{assignmentId}/reject")
    public String reject(@PathVariable Long assignmentId,
                         @RequestParam Long shiftId,
                         RedirectAttributes redirectAttrs) {
        User organizer = securityUtils.getCurrentUser();
        try {
            assignmentService.reject(assignmentId, organizer);
            redirectAttrs.addFlashAttribute("successMsg", "Assignment rejected.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/organizer/shifts/" + shiftId + "/assignments";
    }

    // ---- Organizer: Attendance management ----

    @GetMapping("/organizer/events/{eventId}/attendance")
    public String attendancePage(@PathVariable Long eventId, Model model) {
        model.addAttribute("assignments", assignmentService.findByEvent(eventId));
        model.addAttribute("eventId", eventId);
        return "assignments/attendance";
    }

    @PostMapping("/organizer/assignments/{assignmentId}/checkin")
    public String checkIn(@PathVariable Long assignmentId,
                          @RequestParam Long eventId,
                          RedirectAttributes redirectAttrs) {
        User organizer = securityUtils.getCurrentUser();
        try {
            attendanceService.checkIn(assignmentId, organizer);
            redirectAttrs.addFlashAttribute("successMsg", "Checked in successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/organizer/events/" + eventId + "/attendance";
    }

    @PostMapping("/organizer/assignments/{assignmentId}/checkout")
    public String checkOut(@PathVariable Long assignmentId,
                           @RequestParam Long eventId,
                           RedirectAttributes redirectAttrs) {
        User organizer = securityUtils.getCurrentUser();
        try {
            attendanceService.checkOut(assignmentId, organizer);
            redirectAttrs.addFlashAttribute("successMsg", "Checked out. Hours recorded.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/organizer/events/" + eventId + "/attendance";
    }
}
