package ru.practicum.core.user.service;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.core.common.dto.user.UserAverageScore;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.user.dto.NewUserRequest;
import ru.practicum.core.user.dto.UserDto;
import ru.practicum.core.user.interservice.ReviewInterservice;
import ru.practicum.core.user.mapper.UserMapper;
import ru.practicum.core.user.model.User;
import ru.practicum.core.user.repository.UserRepository;
import ru.practicum.core.user.util.TestDataUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository repository;
    @Mock
    private ReviewInterservice reviewInterservice;

    private UserServiceImpl service;
    private UserMapper mapper;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserMapper.class);
        service = new UserServiceImpl(repository, mapper, reviewInterservice);
        random = TestDataUtils.createEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(repository, reviewInterservice);
    }

    @Test
    @DisplayName("createUser returns the saved user with an initial rating of zero")
    void createUserReturnsSavedDtoWithZeroRating() {
        NewUserRequest request = random.nextObject(NewUserRequest.class);
        when(repository.existsByEmail(request.getEmail())).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        UserDto result = service.createUser(request);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getName()).isEqualTo(request.getName());
        assertThat(result.getEmail()).isEqualTo(request.getEmail());
        assertThat(result.getRating()).isZero();
        verify(repository, times(1)).existsByEmail(request.getEmail());
        verify(repository, times(1)).save(any(User.class));
        verifyNoInteractions(reviewInterservice);
    }

    @Test
    @DisplayName("createUser rejects a duplicate email without saving")
    void createUserRejectsDuplicateEmail() {
        NewUserRequest request = random.nextObject(NewUserRequest.class);
        when(repository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(request.getEmail());
        verify(repository, times(0)).save(any());
        verify(repository, times(1)).existsByEmail(request.getEmail());
        verifyNoInteractions(reviewInterservice);
    }

    @Test
    @DisplayName("getUsers uses offset pagination when IDs are omitted and enriches ratings")
    void getUsersWithoutIdsUsesPageAndRatings() {
        User first = user(11L);
        User second = user(12L);
        when(repository.getUsersWithoutIds(20, 10)).thenReturn(List.of(first, second));
        when(reviewInterservice.findAverageUsersRatingByIds(List.of(11L, 12L)))
                .thenReturn(List.of(new UserAverageScore(11L, 4.5)));

        List<UserDto> result = service.getUsers(null, 20, 10);

        assertThat(result).extracting(UserDto::getId).containsExactly(11L, 12L);
        assertThat(result).extracting(UserDto::getRating).containsExactly(4.5, 0.0);
        verify(repository, times(1)).getUsersWithoutIds(20, 10);
        verify(reviewInterservice, times(1)).findAverageUsersRatingByIds(List.of(11L, 12L));
    }

    @Test
    @DisplayName("getUsers returns only users found by the supplied IDs")
    void getUsersWithIdsReturnsMatchingUsers() {
        List<Long> ids = List.of(9L, 3L, 100L);
        User found = user(9L);
        when(repository.findAllById(ids)).thenReturn(List.of(found));
        when(reviewInterservice.findAverageUsersRatingByIds(List.of(9L)))
                .thenReturn(List.of(new UserAverageScore(9L, 3.75)));

        List<UserDto> result = service.getUsers(ids, 0, 10);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(9L);
            assertThat(dto.getRating()).isEqualTo(3.75);
        });
        verify(repository, times(1)).findAllById(ids);
    }

    @Test
    @DisplayName("getUsers returns an empty list without calling review-service when no users are found")
    void getUsersEmptyDoesNotRequestRatings() {
        when(repository.findAllById(List.of(77L))).thenReturn(List.of());

        assertThat(service.getUsers(List.of(77L), 0, 10)).isEmpty();

        verify(reviewInterservice, times(0)).findAverageUsersRatingByIds(any());
        verify(repository, times(1)).findAllById(List.of(77L));
    }

    @Test
    @DisplayName("deleteUser deletes an existing user")
    void deleteExistingUser() {
        when(repository.findById(13L)).thenReturn(Optional.of(user(13L)));

        service.deleteUser(13L);

        verify(repository, times(1)).findById(13L);
        verify(repository, times(1)).deleteById(13L);
        verifyNoInteractions(reviewInterservice);
    }

    @Test
    @DisplayName("deleteUser reports a missing user and does not delete")
    void deleteMissingUser() {
        when(repository.findById(13L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUser(13L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User with id = 13 was not found");
        verify(repository, times(0)).deleteById(13L);
        verify(repository, times(1)).findById(13L);
        verifyNoInteractions(reviewInterservice);
    }

    private User user(Long id) {
        User user = random.nextObject(User.class);
        user.setId(id);
        return user;
    }
}
