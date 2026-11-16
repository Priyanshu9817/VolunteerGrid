package com.volunteergrid.entity;

/**
 * Status of a volunteer's Assignment to a Shift.
 * PENDING   – awaiting organizer approval (if approval is required)
 * APPROVED  – organizer has accepted the registration
 * REJECTED  – organizer has rejected the registration
 * WITHDRAWN – volunteer withdrew their own registration
 * COMPLETED – volunteer attended and shift is done
 */
public enum AssignmentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    WITHDRAWN,
    COMPLETED
}
