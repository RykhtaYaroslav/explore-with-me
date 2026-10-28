package ru.practicum.core.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.proxy.HibernateProxy;
import ru.practicum.core.category.model.Category;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString(exclude = {"category"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
@Check(constraints = "confirmed_requests >= 0 AND (participant_limit = 0 OR confirmed_requests <= participant_limit)")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation;

    @Column(name = "description", nullable = false, length = 7000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "initiator_id", nullable = false)
    private Long initiatorId;

    @Embedded
    private Location location;

    @Builder.Default
    @Column(name = "paid", nullable = false)
    private Boolean paid = false;

    @Builder.Default
    @Column(name = "participant_limit", nullable = false)
    private Integer participantLimit = 0;

    @Builder.Default
    @Column(name = "confirmed_requests", nullable = false)
    private Long confirmedRequests = 0L;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Builder.Default
    @Column(name = "request_moderation", nullable = false)
    private Boolean requestModeration = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private EventState state = EventState.PENDING;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> objectEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != objectEffectiveClass) {
            return false;
        }
        Event event = (Event) o;
        return getId() != null && Objects.equals(getId(), event.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
