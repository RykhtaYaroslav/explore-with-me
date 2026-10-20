package ru.practicum.main.review.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.user.model.User;

import java.util.Objects;

import static ru.practicum.main.util.EwmConstants.MAX_REVIEW_COMMENT_LENGTH;

@IdClass(EventReviewId.class)
@Table(name = "events_reviews")
@Entity
@Getter
@Setter
@ToString(exclude = {"rater", "event"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventReview {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rater_id")
    private User rater;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "comment", length = MAX_REVIEW_COMMENT_LENGTH)
    private String comment;

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
        EventReview that = (EventReview) o;
        return getRater() != null && Objects.equals(getRater(), that.getRater())
                && getEvent() != null && Objects.equals(getEvent(), that.getEvent())
                && getScore() != null && Objects.equals(getScore(), that.getScore())
                && getComment() != null && Objects.equals(getComment(), that.getComment());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(rater,
                event,
                score,
                comment);
    }
}
