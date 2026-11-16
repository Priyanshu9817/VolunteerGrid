package com.volunteergrid.controller;

import com.volunteergrid.entity.Role;
import com.volunteergrid.entity.User;
import com.volunteergrid.security.SecurityUtils;
import com.volunteergrid.service.EventService;
import com.volunteergrid.service.ReportService;
import com.volunteergrid.service.AssignmentService;
import com.volunteergrid.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Routes the logged-in user to the appropriate dashboard based on their role.
 */
@Controller
public class DashboardController {

    private final SecurityUtils securityUtils;
    private final EventService eventService;
    private final AssignmentService assignmentService;
    private final ReportService reportService;
    private final UserService userService;

    public DashboardController(SecurityUtils securityUtils,
                               EventService eventService,
                               AssignmentService assignmentService,
                               ReportService reportService,
                               UserService userService) {
        this.securityUtils = securityUtils;
        this.eventService = eventService;
        this.assignmentService = assignmentService;
        this.reportService = reportService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = securityUtils.getCurrentUser();
        model.addAttribute("currentUser", userService.toDto(currentUser));

        if (currentUser.getRole() == Role.ADMIN) {
            model.addAttribute("stats", reportService.getAdminDashboardStats());
            model.addAttribute("allEvents", eventService.findAll());
            model.addAttribute("allUsers", userService.findAll());
            return "dashboard/admin";
        }

        if (currentUser.getRole() == Role.ORGANIZER) {
            model.addAttribute("myEvents",
                    eventService.findByOrganizer(currentUser.getId()));
            return "dashboard/organizer";
        }

        // VOLUNTEER dashboard
        model.addAttribute("myAssignments",
                assignmentService.findByVolunteer(currentUser.getId()));
        model.addAttribute("openEvents", eventService.findOpenEvents());
        model.addAttribute("summary",
                reportService.getVolunteerSummary(
                        currentUser.getId(), currentUser.getName()));
        return "dashboard/volunteer";
    }

    @GetMapping("/dashboard/data")
    @ResponseBody
    public java.util.Map<String, Object> dashboardData() {
        return reportService.getDashboardSnapshot(securityUtils.getCurrentUser());
    }
}
