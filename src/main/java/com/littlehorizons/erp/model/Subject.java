package com.littlehorizons.erp.model;

public record Subject(int id, String code, String name, String description, int gradeId, int weeklyHours,
                      boolean active) {
}
