package com.littlehorizons.erp.model;

/** Estados de un recurso; la etiqueta es el valor del ENUM en la base de datos. */
public enum ResourceStatus {
    AVAILABLE("Disponible"),
    ON_LOAN("Prestado"),
    MAINTENANCE("En Mantenimiento"),
    RETIRED("Dado de Baja");

    private final String label;

    ResourceStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ResourceStatus fromLabel(String label) {
        for (ResourceStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de recurso desconocido: " + label);
    }
}
