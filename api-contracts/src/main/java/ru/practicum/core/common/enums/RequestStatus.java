package ru.practicum.core.common.enums;

public enum RequestStatus {
    PENDING, // заявка ожидает подтверждения
    CONFIRMED, // заявка подтверждена
    REJECTED, // заявка отклонена инициатором
    CANCELED, // заявка отменена самим заявителем
}