package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record Teacher(
        int id,
        String documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        String email,
        String phone,
        String specialization,
        LocalDate hireDate,
        String status) {

    public String fullName() {
        return Names.fullName(firstName, middleName, lastName, secondLastName);
    }
}
