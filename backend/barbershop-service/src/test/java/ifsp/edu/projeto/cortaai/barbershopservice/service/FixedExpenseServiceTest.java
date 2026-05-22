package ifsp.edu.projeto.cortaai.barbershopservice.service;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.FixedExpenseRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.FixedExpenseResponseDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.barbershopservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershopservice.model.FixedExpense;
import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.FixedExpenseCategory;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.FixedExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixedExpenseServiceTest {

    @Mock
    private FixedExpenseRepository fixedExpenseRepository;

    @Mock
    private BarbershopRepository barbershopRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private FixedExpenseService service;

    @Test
    void shouldListMonthlyExpensesForOwnerShop() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("owner-firebase")).thenReturn(owner(ownerId));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop(shopId, ownerId)));
        when(fixedExpenseRepository.findActiveForMonth(shopId, 5, 2026))
                .thenReturn(List.of(expense(shopId, FixedExpenseCategory.ALUGUEL, "Sala", "1200.00", 5, 2026, true)));

        List<FixedExpenseResponseDTO> result = service.list("owner-firebase", 5, 2026);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(FixedExpenseCategory.ALUGUEL);
        assertThat(result.get(0).categoryLabel()).isEqualTo("Aluguel");
        assertThat(result.get(0).recurringMonthly()).isTrue();
        verify(fixedExpenseRepository).findActiveForMonth(shopId, 5, 2026);
        verify(fixedExpenseRepository, never()).findActiveForYear(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldListYearExpensesWhenMonthIsMissing() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("owner-firebase")).thenReturn(owner(ownerId));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop(shopId, ownerId)));
        when(fixedExpenseRepository.findActiveForYear(shopId, 2026)).thenReturn(List.of());

        List<FixedExpenseResponseDTO> result = service.list("owner-firebase", null, 2026);

        assertThat(result).isEmpty();
        verify(fixedExpenseRepository).findActiveForYear(shopId, 2026);
    }

    @Test
    void shouldCreateExpenseForOwnerShop() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        FixedExpenseRequestDTO request = new FixedExpenseRequestDTO(
                FixedExpenseCategory.INTERNET,
                "Fibra",
                new BigDecimal("149.90"),
                5,
                2026,
                false
        );

        when(userServiceClient.getUserByFirebaseUid("owner-firebase")).thenReturn(owner(ownerId));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop(shopId, ownerId)));
        when(fixedExpenseRepository.save(org.mockito.ArgumentMatchers.any(FixedExpense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FixedExpenseResponseDTO result = service.create("owner-firebase", request);

        ArgumentCaptor<FixedExpense> captor = ArgumentCaptor.forClass(FixedExpense.class);
        verify(fixedExpenseRepository).save(captor.capture());
        assertThat(captor.getValue().getBarbershopId()).isEqualTo(shopId);
        assertThat(captor.getValue().getCategory()).isEqualTo(FixedExpenseCategory.INTERNET);
        assertThat(captor.getValue().getRecurringMonthly()).isFalse();
        assertThat(result.amount()).isEqualByComparingTo("149.90");
    }

    @Test
    void shouldDeleteExpenseFromOwnerShop() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        FixedExpense expense = expense(shopId, FixedExpenseCategory.LUZ, null, "300.00", 5, 2026, false);

        when(userServiceClient.getUserByFirebaseUid("owner-firebase")).thenReturn(owner(ownerId));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop(shopId, ownerId)));
        when(fixedExpenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        service.delete("owner-firebase", expenseId);

        verify(fixedExpenseRepository).delete(expense);
    }

    @Test
    void shouldRejectDeleteForExpenseFromAnotherShop() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();

        when(userServiceClient.getUserByFirebaseUid("owner-firebase")).thenReturn(owner(ownerId));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop(shopId, ownerId)));
        when(fixedExpenseRepository.findById(expenseId))
                .thenReturn(Optional.of(expense(UUID.randomUUID(), FixedExpenseCategory.LUZ, null, "300.00", 5, 2026, false)));

        assertThatThrownBy(() -> service.delete("owner-firebase", expenseId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Você não tem permissão para excluir este gasto fixo.");

        verify(fixedExpenseRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectNonBarberUser() {
        UserInfoDTO customer = owner(UUID.randomUUID());
        customer.setUserType("CUSTOMER");
        when(userServiceClient.getUserByFirebaseUid("customer-firebase")).thenReturn(customer);

        assertThatThrownBy(() -> service.list("customer-firebase", 5, 2026))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Apenas barbeiros podem gerenciar gastos fixos.");
    }

    @Test
    void shouldRejectOwnerWithoutShop() {
        UUID ownerId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("owner-firebase")).thenReturn(owner(ownerId));
        when(barbershopRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list("owner-firebase", 5, 2026))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Você não possui uma barbearia cadastrada.");
    }

    private UserInfoDTO owner(UUID ownerId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(ownerId);
        user.setUserType("BARBER");
        return user;
    }

    private Barbershop shop(UUID shopId, UUID ownerId) {
        Barbershop shop = new Barbershop();
        shop.setId(shopId);
        shop.setOwnerId(ownerId);
        return shop;
    }

    private FixedExpense expense(UUID shopId,
                                 FixedExpenseCategory category,
                                 String customName,
                                 String amount,
                                 Integer month,
                                 Integer year,
                                 Boolean recurringMonthly) {
        FixedExpense expense = new FixedExpense();
        expense.setId(UUID.randomUUID());
        expense.setBarbershopId(shopId);
        expense.setCategory(category);
        expense.setCustomName(customName);
        expense.setAmount(new BigDecimal(amount));
        expense.setMonth(month);
        expense.setYear(year);
        expense.setRecurringMonthly(recurringMonthly);
        return expense;
    }
}
