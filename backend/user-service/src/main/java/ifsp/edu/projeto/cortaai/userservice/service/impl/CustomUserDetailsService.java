package ifsp.edu.projeto.cortaai.userservice.service.impl;

import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Serviço de autenticação que busca usuários (Customer ou Barber) por email.
 * Usado pelo Spring Security DaoAuthenticationProvider.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;
    private final BarberRepository barberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Tenta encontrar como Barber primeiro (implementa UserDetails)
        Optional<Barber> barber = barberRepository.findByEmail(email);
        if (barber.isPresent()) {
            return barber.get(); // Barber já implementa UserDetails
        }

        // Tenta encontrar como Customer
        Optional<Customer> customer = customerRepository.findByEmail(email);
        if (customer.isPresent()) {
            Customer c = customer.get();
            return new User(
                    c.getEmail(),
                    c.getPassword(),
                    List.of(new SimpleGrantedAuthority(c.getRole()))
            );
        }

        throw new UsernameNotFoundException("Usuário não encontrado com email: " + email);
    }
}