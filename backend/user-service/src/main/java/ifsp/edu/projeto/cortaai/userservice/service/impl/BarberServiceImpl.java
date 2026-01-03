package ifsp.edu.projeto.cortaai.userservice.service.impl;

import ifsp.edu.projeto.cortaai.userservice.dto.*;
import ifsp.edu.projeto.cortaai.userservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.userservice.mapper.BarberMapper;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.service.BarberService;
import ifsp.edu.projeto.cortaai.userservice.service.JwtTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
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
        // Validação básica de senha (pode melhorar depois)
        if (!createBarberDTO.password().equals(createBarberDTO.confirmPassword())) {
            throw new IllegalArgumentException("Senhas não conferem");
        }

        Barber barber = new Barber();
        barber.setName(createBarberDTO.name());
        barber.setEmail(createBarberDTO.email());
        barber.setCpf(createBarberDTO.cpf());
        barber.setPhoneNumber(createBarberDTO.phoneNumber());
        barber.setPassword(passwordEncoder.encode(createBarberDTO.password()));

        barber.setRole("ROLE_BARBER");

        Barber savedBarber = barberRepository.save(barber);
        return barberMapper.toDTO(savedBarber);
    }

    @Override
    public LoginResponseDTO login(LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.email(), loginDTO.password());

        Authentication authenticate = this.authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        var barber = (Barber) authenticate.getPrincipal();

        String token = jwtTokenService.generateToken(barber);

        return new LoginResponseDTO(token, barber.getName(), barber.getRole(), barber.getId());
    }

    @Override
    public BarberDTO update(Long id, UpdateBarberDTO dto) {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado."));

        barber.setName(dto.getName());
        barber.setPhoneNumber(dto.getPhoneNumber());

        return barberMapper.toDTO(barberRepository.save(barber));
    }

    @Override
    public BarberDTO findById(Long id) {
        Barber barber = barberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado."));
        return barberMapper.toDTO(barber);
    }

    @Override
    public List<BarberDTO> findAll() {
        return barberRepository.findAll().stream()
                .map(barberMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<BarberDTO> findByBarbershopId(Long barbershopId) {
        return barberRepository.findByBarbershopId(barbershopId).stream()
                .map(barberMapper::toDTO)
                .collect(Collectors.toList());
    }
}