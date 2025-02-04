package co.id.finease.service;

import co.id.finease.config.SessionAuthenticationToken;
import co.id.finease.dto.SessionRequest;
import co.id.finease.dto.SessionResponse;
import co.id.finease.entity.Account;
import co.id.finease.entity.Session;
import co.id.finease.repository.SessionRepository;
import co.id.finease.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionServiceTest {

    private SessionService sessionService;
    private SessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        sessionService = new SessionService(sessionRepository);
    }

    // --- Test for generating a new session ---
    @Test
    void testGenerateSession_Success() {
        // Arrange
        Session session = Session.builder()
                .apiKey(UUID.randomUUID().toString())
                .applicationId(UUID.randomUUID().toString())
                .status(Constants.STATUS_ACTIVE)
                .build();

        when(sessionRepository.save(any(Session.class))).thenReturn(session);

        // Act
        SessionResponse response = sessionService.generateSession();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertNotNull(response.getApiKey());
        assertNotNull(response.getApplicationId());
        assertEquals("Session created successfully.", response.getStatusMessage());

        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    // --- Test for setting a session to inactive ---
    @Test
    void testInActiveSession_Success() {
        // Arrange
        String apiKey = "test-api-key";
        String applicationId = "test-application-id";
        SessionRequest request = new SessionRequest();
        request.setApiKey(apiKey);
        request.setApplicationID(applicationId);
        Session session = Session.builder()
                .apiKey(apiKey)
                .applicationId(applicationId)
                .status(Constants.STATUS_ACTIVE)
                .build();

        when(sessionRepository.findByApplicationIdAndApiKeyAndStatus(applicationId, apiKey, Constants.STATUS_ACTIVE))
                .thenReturn(Optional.of(session));

        // Capture the session object when saved
        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SessionResponse response = sessionService.inActiveSession(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("Session updated to inactive successfully", response.getStatusMessage());
        assertEquals(applicationId, response.getApplicationId());
        assertEquals(apiKey, response.getApiKey());

        verify(sessionRepository).save(sessionCaptor.capture());
        assertEquals(Constants.STATUS_INACTIVE, sessionCaptor.getValue().getStatus());
    }

    @Test
    void testInActiveSession_Failure_NoActiveSession() {
        // Arrange
        String apiKey = "invalid-api-key";
        String applicationId = "invalid-application-id";
        SessionRequest request = new SessionRequest();
        request.setApiKey(apiKey);
        request.setApplicationID(applicationId);

        when(sessionRepository.findByApplicationIdAndApiKeyAndStatus(applicationId, apiKey, Constants.STATUS_ACTIVE))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> sessionService.inActiveSession(request));
        assertEquals("No active Session found with the provided details", exception.getMessage());

        verify(sessionRepository, never()).save(any(Session.class));
    }

    // --- Test finding session by applicationId and API key ---
    @Test
    void testFindByApplicationIdAndApiKey_Success() {
        // Arrange
        String applicationId = "app-123";
        String apiKey = "key-123";
        Session session = new Session();
        session.setApplicationId(applicationId);
        session.setApiKey(apiKey);

        when(sessionRepository.findByApplicationIdAndApiKey(applicationId, apiKey))
                .thenReturn(Optional.of(session));

        // Act
        Session result = sessionService.findByApplicationIdAndApiKey(applicationId, apiKey);

        // Assert
        assertNotNull(result);
        assertEquals(applicationId, result.getApplicationId());
        assertEquals(apiKey, result.getApiKey());
    }

    @Test
    void testFindByApplicationIdAndApiKey_NotFound() {
        // Arrange
        String applicationId = "invalid-app";
        String apiKey = "invalid-key";

        when(sessionRepository.findByApplicationIdAndApiKey(applicationId, apiKey))
                .thenReturn(Optional.empty());

        // Act
        Session result = sessionService.findByApplicationIdAndApiKey(applicationId, apiKey);

        // Assert
        assertNull(result);
    }

    // --- Test get session ID from security context ---
    @Test
    void testGetSessionIdFromSecurityContext_Success() {
        // Arrange
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        Authentication authentication = mock(SessionAuthenticationToken.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getCredentials()).thenReturn("123");

        // Act
        Long sessionId = sessionService.getSessionIdFromSecurityContext();

        // Assert
        assertEquals(123L, sessionId);
    }

    @Test
    void testGetSessionIdFromSecurityContext_Failure_NotAuthenticated() {
        // Arrange
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> sessionService.getSessionIdFromSecurityContext());
        assertEquals("Session not authenticated", exception.getMessage());
    }

    // --- Test get Account from security context ---
    @Test
    void testGetAccountIdFromSecurityContext_Success() {
        // Arrange
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        Account mockAccount = new Account();
        Authentication authentication = mock(SessionAuthenticationToken.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getDetails()).thenReturn(mockAccount);

        // Act
        Account account = sessionService.getAccountIdFromSecurityContext();

        // Assert
        assertNotNull(account);
        assertEquals(mockAccount, account);
    }

    @Test
    void testGetAccountIdFromSecurityContext_Failure_NotAuthenticated() {
        // Arrange
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(mock(Authentication.class));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> sessionService.getAccountIdFromSecurityContext());
        assertEquals("Session not authenticated", exception.getMessage());
    }
}
