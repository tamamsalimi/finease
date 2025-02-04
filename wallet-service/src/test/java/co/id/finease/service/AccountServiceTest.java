package co.id.finease.service;

import co.id.finease.dto.AccountRequest;
import co.id.finease.entity.Account;
import co.id.finease.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    @InjectMocks
    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
    }

    // --- Test for getting account by reference ---
    @Test
    void testGetAccountRef_Success() {
        // Arrange
        String accountRef = "RFID0000000001";
        Long sessionId = 123L;

        Account mockAccount = new Account();
        mockAccount.setAccountRef(accountRef);
        mockAccount.setSessionId(sessionId);

        when(sessionService.getSessionIdFromSecurityContext()).thenReturn(sessionId);
        when(accountRepository.findByAccountRefAndSessionId(accountRef, sessionId))
                .thenReturn(Optional.of(mockAccount));

        // Act
        Optional<Account> result = accountService.getAccountRef(accountRef);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(accountRef, result.get().getAccountRef());
        verify(sessionService, times(1)).getSessionIdFromSecurityContext();
        verify(accountRepository, times(1)).findByAccountRefAndSessionId(accountRef, sessionId);
    }

    @Test
    void testGetAccountRef_NotFound() {
        // Arrange
        String accountRef = "RFID0000000001";
        Long sessionId = 123L;

        when(sessionService.getSessionIdFromSecurityContext()).thenReturn(sessionId);
        when(accountRepository.findByAccountRefAndSessionId(accountRef, sessionId))
                .thenReturn(Optional.empty());

        // Act
        Optional<Account> result = accountService.getAccountRef(accountRef);

        // Assert
        assertFalse(result.isPresent());
        verify(sessionService, times(1)).getSessionIdFromSecurityContext();
        verify(accountRepository, times(1)).findByAccountRefAndSessionId(accountRef, sessionId);
    }

    // --- Test for creating an account ---
    @Test
    void testCreateAccount_NewAccount() {
        // Arrange
        Long sessionId = 123L;
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountName("Test Account");

        when(sessionService.getSessionIdFromSecurityContext()).thenReturn(sessionId);
        when(accountRepository.findBySessionIdAndAccountName(sessionId, accountRequest.getAccountName()))
                .thenReturn(Optional.empty());
        when(accountRepository.getNextSequenceValue()).thenReturn(1L);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Account result = accountService.createAccount(accountRequest);

        // Assert
        assertNotNull(result);
        assertEquals("Test Account", result.getAccountName());
        assertEquals("RFID0000000001", result.getAccountRef());
        assertEquals(sessionId, result.getSessionId());
        assertEquals('A', result.getStatus());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testCreateAccount_ExistingAccount() {
        // Arrange
        Long sessionId = 123L;
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountName("Test Account");

        Account existingAccount = new Account();
        existingAccount.setAccountRef("RFID0000000001");
        existingAccount.setAccountName("Test Account");
        existingAccount.setSessionId(sessionId);

        when(sessionService.getSessionIdFromSecurityContext()).thenReturn(sessionId);
        when(accountRepository.findBySessionIdAndAccountName(sessionId, accountRequest.getAccountName()))
                .thenReturn(Optional.of(existingAccount));

        // Act
        Account result = accountService.createAccount(accountRequest);

        // Assert
        assertNotNull(result);
        assertEquals(existingAccount, result);
        verify(accountRepository, never()).save(any(Account.class));
    }

    // --- Test for generating account reference ID ---
    @Test
    void testGenerateAccountRefId_Success() {
        // Arrange
        when(accountRepository.getNextSequenceValue()).thenReturn(123L);

        // Act
        String result = accountService.generateAccountRefId();

        // Assert
        assertEquals("RFID0000000123", result);
        verify(accountRepository, times(1)).getNextSequenceValue();
    }
}
