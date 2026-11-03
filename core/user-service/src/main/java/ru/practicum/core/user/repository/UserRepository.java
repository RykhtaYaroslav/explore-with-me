package ru.practicum.core.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.core.user.model.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select u from User u order by u.id limit :size offset :from")
    List<User> getUsersWithoutIds(@Param("from") int from,
                                  @Param("size") int size);

    boolean existsByEmail(String email);
}
