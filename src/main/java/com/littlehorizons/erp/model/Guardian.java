package com.littlehorizons.erp.model;


public record Guardian(
        int id,
        String documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        String relationship,
        String phone,
        String secondaryPhone,
        String email,
        String address,
        String occupation,
        boolean financialResponsible) {

    public String fullName() {
        return Names.fullName(firstName, middleName, lastName, secondLastName);
    }
}
