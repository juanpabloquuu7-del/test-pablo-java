package com.littlehorizons.erp.model;

/** Tipos de evento; la etiqueta es el valor del ENUM en la base de datos. */
public enum EventType {
    ACADEMIC("Académico"),
    CULTURAL("Cultural"),
    SPORTS("Deportivo"),
    SOCIAL("Social"),
    SCIENTIFIC("Científico");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static EventType fromLabel(String label) {
        for (EventType type : values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de evento desconocido: " + label);
    }
}
