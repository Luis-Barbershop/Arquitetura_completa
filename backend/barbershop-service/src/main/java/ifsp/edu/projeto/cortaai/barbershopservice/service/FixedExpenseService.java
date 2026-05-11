package ifsp.edu.projeto.cortaai.barbershopservice.service;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.FixedExpenseRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.FixedExpenseResponseDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.UserServiceUnavailableException;
import ifsp.edu.projeto.cortaai.barbershopservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.barbershopservice.model.FixedExpense;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopRepository;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.FixedExpenseRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FixedExpenseService {

    private final FixedExpenseRepository fixedExpenseRepository;
    private final BarbershopRepository barbershopRepository;
    private final UserServiceClient userServiceClient;

    @Transactional(readOnly = true)
    public List<FixedExpenseResponseDTO> list(String principalEmail, Integer month, Integer year) {
        var owner = resolveOwner(principalEmail);
        var shop = findOwnerShop(owner);

        List<FixedExpense> expenses = (month != null && year != null)
            ? fixedExpenseRepository.findByBarbershopIdAndMonthAndYearOrderByCategoryAsc(shop.getId(), month, year)
            : fixedExpenseRepository.findByBarbershopIdAndYearOrderByMonthAscCategoryAsc(shop.getId(),
                year != null ? year : java.time.LocalDate.now().getYear());

        return expenses.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public FixedExpenseResponseDTO create(String principalEmail, FixedExpenseRequestDTO dto) {
        var owner = resolveOwner(principalEmail);
        var shop = findOwnerShop(owner);

        FixedExpense expense = new FixedExpense();
        expense.setBarbershopId(shop.getId());
        expense.setCategory(dto.category());
        expense.setCustomName(dto.customName());
        expense.setAmount(dto.amount());
        expense.setMonth(dto.month());
        expense.setYear(dto.year());

        return toDTO(fixedExpenseRepository.save(expense));
    }

    public void delete(String principalEmail, UUID expenseId) {
        var owner = resolveOwner(principalEmail);
        var shop = findOwnerShop(owner);

        FixedExpense expense = fixedExpenseRepository.findById(expenseId)
            .orElseThrow(() -> new NotFoundException("Gasto fixo não encontrado."));

        if (!expense.getBarbershopId().equals(shop.getId())) {
            throw new ForbiddenException("Você não tem permissão para excluir este gasto fixo.");
        }

        fixedExpenseRepository.delete(expense);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ifsp.edu.projeto.cortaai.barbershopservice.dto.UserInfoDTO resolveOwner(String email) {
        try {
            var user = userServiceClient.getUserByEmail(email);
            if (user == null) throw new NotFoundException("Usuário não encontrado.");
            if (!"BARBER".equals(user.getUserType())) {
                throw new ForbiddenException("Apenas barbeiros podem gerenciar gastos fixos.");
            }
            return user;
        } catch (FeignException.NotFound ex) {
            throw new NotFoundException("Usuário não encontrado.");
        } catch (FeignException ex) {
            throw new UserServiceUnavailableException("Serviço de usuários indisponível.");
        }
    }

    private ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop findOwnerShop(
            ifsp.edu.projeto.cortaai.barbershopservice.dto.UserInfoDTO owner) {
        return barbershopRepository.findByOwnerId(owner.getId())
            .orElseThrow(() -> new NotFoundException("Você não possui uma barbearia cadastrada."));
    }

    private FixedExpenseResponseDTO toDTO(FixedExpense e) {
        return new FixedExpenseResponseDTO(
            e.getId(),
            e.getCategory(),
            e.getCategory().getLabel(),
            e.getCustomName(),
            e.getAmount(),
            e.getMonth(),
            e.getYear(),
            e.getCreatedAt()
        );
    }
}
