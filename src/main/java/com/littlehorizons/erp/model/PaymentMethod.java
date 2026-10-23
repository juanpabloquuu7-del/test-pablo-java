package com.littlehorizons.erp.model;

/** Métodos de pago; la etiqueta es el valor del ENUM en la base de datos. */
public enum PaymentMethod {
    CASH("Efectivo"),
    TRANSFER("Transferencia"),
    DEBIT_CARD("Tarjeta Débito"),
    CREDIT_CARD("Tarjeta Crédito"),
    CHECK("Cheque");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static PaymentMethod fromLabel(String label) {
        for (PaymentMethod method : values()) {
            if (method.label.equals(label)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Método de pago desconocido: " + label);
    }
}
