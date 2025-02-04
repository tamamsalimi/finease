package co.id.finease.service;

import co.id.finease.dto.OwedTransactionItem;
import co.id.finease.entity.Account;
import co.id.finease.repository.OwedTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwedTransactionServiceTest {

    private OwedTransactionService owedTransactionService;
    private SessionService sessionService;
    private OwedTransactionRepository owedTransactionRepository;

    @BeforeEach
    void setUp() {
        sessionService = mock(SessionService.class);
        owedTransactionRepository = mock(OwedTransactionRepository.class);

        owedTransactionService = new OwedTransactionService(
                sessionService,
                owedTransactionRepository
        );
    }

    // --- Test for Generating Owed Transaction Reference ID ---
    @Test
    void testGenerateRefTransactionId_Success() {
        // Arrange
        when(owedTransactionRepository.getNextSequenceValue()).thenReturn(1234L);

        // Act
        String refId = owedTransactionService.generateRefTransactionId();

        // Assert
        String expectedPrefix = "OWED" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        assertTrue(refId.startsWith(expectedPrefix));
        assertTrue(refId.endsWith("1234"));
        assertEquals(expectedPrefix.length() + 4, refId.length());

        verify(owedTransactionRepository, times(1)).getNextSequenceValue();
    }

    // --- Test for Getting Owed Transaction Summary ---
    @Test
    void testGetOwedTransactionSummary_WithAccount() {
        // Arrange
        Account account = new Account();
        List<Object[]> mockResults = List.of(
                new Object[]{"recipient-1", "Recipient Name 1", new BigDecimal("100.50")},
                new Object[]{"recipient-2", "Recipient Name 2", new BigDecimal("200.75")}
        );

        when(owedTransactionRepository.findTotalOwedGroupedByRecipient(account)).thenReturn(mockResults);

        // Act
        List<OwedTransactionItem> result = owedTransactionService.getOwedTransactionSummary(account);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("recipient-1", result.get(0).getAccountId());
        assertEquals("Recipient Name 1", result.get(0).getAccountName());
        assertEquals(new BigDecimal("100.50"), result.get(0).getAmount());

        assertEquals("recipient-2", result.get(1).getAccountId());
        assertEquals("Recipient Name 2", result.get(1).getAccountName());
        assertEquals(new BigDecimal("200.75"), result.get(1).getAmount());

        verify(owedTransactionRepository, times(1)).findTotalOwedGroupedByRecipient(account);
    }

    @Test
    void testGetOwedTransactionSummary_WithoutAccount_FetchFromSession() {
        // Arrange
        Account mockAccount = new Account();
        when(sessionService.getAccountIdFromSecurityContext()).thenReturn(mockAccount);
        Object[] row = new Object[]{"recipient-1", "Payer Name 1", new BigDecimal("50.25")};
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(row);

        when(owedTransactionRepository.findTotalOwedGroupedByRecipient(mockAccount)).thenReturn(mockResults);

        // Act
        List<OwedTransactionItem> result = owedTransactionService.getOwedTransactionSummary(null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("recipient-1", result.get(0).getAccountId());
        assertEquals(new BigDecimal("50.25"), result.get(0).getAmount());

        verify(sessionService, times(1)).getAccountIdFromSecurityContext();
        verify(owedTransactionRepository, times(1)).findTotalOwedGroupedByRecipient(mockAccount);
    }

    // --- Test for Getting Amounts Owed By Me ---
    @Test
    void testGetAmountsOwedByMe_WithAccount() {
        // Arrange
        Account account = new Account();
        List<Object[]> mockResults = List.of(
                new Object[]{"payer-1", "Payer Name 1", new BigDecimal("300.00")},
                new Object[]{"payer-2", "Payer Name 2", new BigDecimal("150.50")}
        );

        when(owedTransactionRepository.findTotalOwedGroupedByPayer(account)).thenReturn(mockResults);

        // Act
        List<OwedTransactionItem> result = owedTransactionService.getAmountsOwedByMe(account);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("payer-1", result.get(0).getAccountId());
        assertEquals(new BigDecimal("300.00"), result.get(0).getAmount());

        assertEquals("payer-2", result.get(1).getAccountId());
        assertEquals(new BigDecimal("150.50"), result.get(1).getAmount());

        verify(owedTransactionRepository, times(1)).findTotalOwedGroupedByPayer(account);
    }

    @Test
    void testGetAmountsOwedByMe_WithoutAccount_FetchFromSession() {
        // Arrange
        Account mockAccount = new Account();
        when(sessionService.getAccountIdFromSecurityContext()).thenReturn(mockAccount);

        Object[] row = new Object[]{"payer-1", "Payer Name 1", new BigDecimal("75.00")};
        List<Object[]> mockResults = new ArrayList<>();
        mockResults.add(row);


        when(owedTransactionRepository.findTotalOwedGroupedByPayer(mockAccount)).thenReturn(mockResults);

        // Act
        List<OwedTransactionItem> result = owedTransactionService.getAmountsOwedByMe(null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("payer-1", result.get(0).getAccountId()); // Correct field name
        assertEquals("Payer Name 1", result.get(0).getAccountName()); // Ensure name matches
        assertEquals(new BigDecimal("75.00"), result.get(0).getAmount()); // Correct field name

        verify(sessionService, times(1)).getAccountIdFromSecurityContext();
        verify(owedTransactionRepository, times(1)).findTotalOwedGroupedByPayer(mockAccount);
    }

    @Test
    void testGetOwedTransactionSummary_EmptyResults() {
        // Arrange
        Account account = new Account();
        when(owedTransactionRepository.findTotalOwedGroupedByRecipient(account)).thenReturn(List.of());

        // Act
        List<OwedTransactionItem> result = owedTransactionService.getOwedTransactionSummary(account);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(owedTransactionRepository, times(1)).findTotalOwedGroupedByRecipient(account);
    }

    @Test
    void testGetAmountsOwedByMe_EmptyResults() {
        // Arrange
        Account account = new Account();
        when(owedTransactionRepository.findTotalOwedGroupedByPayer(account)).thenReturn(List.of());

        // Act
        List<OwedTransactionItem> result = owedTransactionService.getAmountsOwedByMe(account);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(owedTransactionRepository, times(1)).findTotalOwedGroupedByPayer(account);
    }
}
