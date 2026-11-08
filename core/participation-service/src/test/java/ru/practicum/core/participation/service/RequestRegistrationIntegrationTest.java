package ru.practicum.core.participation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.participation.client.EventFeignClient;
import ru.practicum.core.participation.dto.mapper.RequestMapperImpl;
import ru.practicum.core.participation.model.ParticipationRequest;
import ru.practicum.core.participation.repository.RequestRepository;
import ru.practicum.core.participation.validation.RequestValidator;
import ru.practicum.ewm.stats.model.action.ActionType;
import ru.practicum.ewm.stats.model.action.UserAction;
import ru.practicum.stats.client.grpc.CollectorGrpcClient;
import ru.practicum.stats.client.grpc.StatsGrpcException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DataJpaTest(showSql = false, properties = {
        "spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate", "spring.sql.init.mode=always"
})
@ContextConfiguration(classes = RequestRegistrationIntegrationTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RequestRegistrationIntegrationTest {
    private static final long USER_ID = 67L;
    private static final long EVENT_ID = 42L;

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = ParticipationRequest.class)
    @EnableJpaRepositories(basePackageClasses = RequestRepository.class)
    @Import({RequestServiceImpl.class, RequestRegistrationListener.class, RequestMapperImpl.class})
    static class Config {
    }

    @Autowired
    private RequestService requests;
    @Autowired
    private RequestRepository repository;
    @Autowired
    private TransactionTemplate transaction;
    @MockBean
    private RequestValidator validator;
    @MockBean
    private EventFeignClient events;
    @MockBean
    private CollectorGrpcClient collector;

    @BeforeEach
    void clearRequests() {
        repository.deleteAll();
    }

    @AfterEach
    void verifyInteractions() {
        verify(validator, times(1)).validateCreateRequestAndGetStatus(USER_ID, EVENT_ID);
        verifyNoMoreInteractions(validator, events, collector);
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"PENDING", "CONFIRMED"})
    @DisplayName("Pending and confirmed requests send REGISTER once after commit")
    void sendsRegisterAfterCommit(RequestStatus requestStatus) {
        when(validator.validateCreateRequestAndGetStatus(USER_ID, EVENT_ID)).thenReturn(requestStatus);

        transaction.executeWithoutResult(status -> {
            assertThat(requests.createRequest(USER_ID, EVENT_ID).getStatus()).isEqualTo(requestStatus);
            verifyNoInteractions(collector);
        });

        assertThat(repository.findAll()).extracting(ParticipationRequest::getStatus).containsExactly(requestStatus);
        verify(collector, times(1)).collectUserAction(registrationAction());
        verify(events, times(requestStatus == RequestStatus.CONFIRMED ? 1 : 0))
                .adjustConfirmedRequestCount(EVENT_ID, 1);
    }

    @Test
    @DisplayName("A rejected request sends no REGISTER")
    void skipsRegisterOnRejection() {
        when(validator.validateCreateRequestAndGetStatus(USER_ID, EVENT_ID))
                .thenThrow(new ConflictException("Participation denied"));

        assertThatThrownBy(() -> requests.createRequest(USER_ID, EVENT_ID)).isInstanceOf(ConflictException.class);

        assertThat(repository.findAll()).isEmpty();
        verifyNoInteractions(events, collector);
    }

    @Test
    @DisplayName("A rolled-back request sends no REGISTER")
    void skipsRegisterOnRollback() {
        when(validator.validateCreateRequestAndGetStatus(USER_ID, EVENT_ID)).thenReturn(RequestStatus.PENDING);

        transaction.executeWithoutResult(status -> {
            requests.createRequest(USER_ID, EVENT_ID);
            status.setRollbackOnly();
        });

        assertThat(repository.findAll()).isEmpty();
        verifyNoInteractions(events, collector);
    }

    @Test
    @DisplayName("Collector failure leaves the committed request available")
    void keepsRequestWhenCollectorFails() {
        when(validator.validateCreateRequestAndGetStatus(USER_ID, EVENT_ID)).thenReturn(RequestStatus.PENDING);
        doThrow(StatsGrpcException.class).when(collector).collectUserAction(registrationAction());

        Long requestId = requests.createRequest(USER_ID, EVENT_ID).getId();

        assertThat(repository.findById(requestId)).isPresent();
        verify(collector, times(1)).collectUserAction(registrationAction());
        verifyNoInteractions(events);
    }

    private UserAction registrationAction() {
        return argThat(action -> action.userId() == USER_ID && action.eventId() == EVENT_ID
                && action.actionType() == ActionType.REGISTER && action.timestamp() != null);
    }
}
