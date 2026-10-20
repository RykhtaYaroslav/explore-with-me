package ru.practicum.main.request.model;

public enum RequestStatus {
    PENDING, // заявка ожидает подтверждения
    CONFIRMED, // заявка подтверждена
    REJECTED, // заявка отклонена инициатором
    CANCELED, // заявка отменена самим заявителем
}