package ru.practicum.ewm.stats.model.action;


import lombok.Getter;

@Getter
public enum ActionType {
    VIEW(0.4),
    REGISTER(0.8),
    LIKE(1.0);

    private final double weight;

    ActionType(double weight) {
        this.weight = weight;
    }

}
