package com.notification.entity;

//Represents all supported notification categories in the system
public enum NotificationType {

 // Task or item assigned to a user
 ASSIGNMENT,

 // User mentioned in a comment or description
 MENTION,

 // Upcoming due date reminder
 DUE_DATE,

 // New comment added to a task or list
 COMMENT,

 // Task or list moved between boards/positions
 MOVE,

 // System-wide or workspace-wide announcement
 BROADCAST,

 // Task has passed its due date
 OVERDUE
}