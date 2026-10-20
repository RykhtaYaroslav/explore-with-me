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

@Table(name = "users_reviews")
@Entity
@IdClass(UserReviewId.class)
@Getter
@Setter
@ToString(exclude = {"rater", "target", "event"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReview {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rater_id")
    private User rater;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    private User target;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "comment", length = 200)
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
        UserReview that = (UserReview) o;
        return getRater() != null && Objects.equals(getRater(), that.getRater())
                && getTarget() != null && Objects.equals(getTarget(), that.getTarget())
                && getEvent() != null && Objects.equals(getEvent(), that.getEvent());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(rater, target, event);
    }
}
