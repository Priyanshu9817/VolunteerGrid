package com.volunteergrid.entity;

/**
 * Lifecycle states for an Event.
 * DRAFT      – created, not yet published
 * OPEN       – accepting volunteer registrations
 * CLOSED     – no new registrations allowed
 * COMPLETED  – event has taken place
 */
public enum EventStatus {
    DRAFT,
    OPEN,
    CLOSED,
    COMPLETED
}
