package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record Student(
        int id,
        String documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        LocalDate birthDate,
        String gender,
        String address,
        String phone,
        String email,
        String bloodType,
        String allergies,
        String medicalConditions,
        LocalDate admissionDate,
        String status) {

    public String fullName() {
        return Names.fullName(firstName, middleName, lastName, secondLastName);
    }
}
