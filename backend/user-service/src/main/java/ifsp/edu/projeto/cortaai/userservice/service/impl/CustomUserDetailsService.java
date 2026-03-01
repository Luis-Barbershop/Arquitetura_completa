package ifsp.edu.projeto.cortaai.userservice.service.impl;

import ifsp.edu.projeto.cortaai.userservice.dto.*;
import ifsp.edu.projeto.cortaai.userservice.mapper.BarberMapper;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.service.BarberService;
import ifsp.edu.projeto.cortaai.userservice.service.JwtTokenService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BarberServiceImpl implements BarberService {

    private final BarberRepository barberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;
    private final BarberMapper barberMapper;

    @Override
    public BarberDTO createBarber(CreateBarberDTO createBarberDTO) {
        if (barberRepository.existsByEmail(createBarberDTO.email())) {
            throw new IllegalArgumentException("Email já cadastrado.");
        }
        if (barberRepository.existsByDocumentCPF(createBarberDTO.cpf())) {
            throw new IllegalArgumentException("CPF já cadastrado.");
        }

        Barber barber = new Barber();
        barber.setName(createBarberDTO.name());
        barber.setEmail(createBarberDTO.email());
        barber.setDocumentCPF(createBarberDTO.cpf());
        barber.setTell(createBarberDTO.phoneNumber());
        barber.setPassword(passwordEncoder.encode(createBarberDTO.password()));

        // Padrão
        barber.setRole("ROLE_BARBER");
        barber.setOwner(false);

        Barber savedBarber = barberRepository.save(barber);
        return barberMapper.toDTO(savedBarber);
    }

    @Override
    public LoginResponseDTO login(LoginDTO loginDTO) {
        // Isso invoca o CustomUserDetailsService.loadUserByUsername
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.email(), loginDTO.password())
        );

        // Se a autenticação passou, buscamos o usuário para gerar o token
        Barber barber = barberRepository.findByEmail(loginDTO.email())
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        String token = jwtTokenService.generateToken(barber); // Ajuste seu TokenService para aceitar UserDetails

        return new LoginResponseDTO(token, barber.getName(), barber.getRole(), barber.getId());
    }

    @Override
    public BarberDTO update(UUID id, UpdateBarberDTO dto) {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        if(dto.getName() != null) barber.setName(dto.getName());
        if(dto.getPhoneNumber() != null) barber.setTell(dto.getPhoneNumber());

        return barberMapper.toDTO(barberRepository.save(barber));
    }

    @Override
    public BarberDTO findById(UUID id) {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));
        return barberMapper.toDTO(barber);
    }

    @Override
    public List<BarberDTO> findAll() {
        return barberRepository.findAll().stream()
                .map(barberMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<BarberDTO> findByBarbershopId(UUID barbershopId) {
        return barberRepository.findByBarbershopId(barbershopId).stream()
                .map(barberMapper::toDTO)
                .collect(Collectors.toList());
    }
}