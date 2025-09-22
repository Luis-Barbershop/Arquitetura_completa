package ifsp.edu.projeto.cortaai.service;

import ifsp.edu.projeto.cortaai.model.Barber;
import ifsp.edu.projeto.cortaai.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.events.BeforeDeleteBarber;
import ifsp.edu.projeto.cortaai.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class BarberService {

    private final BarberRepository barberRepository;
    private final ApplicationEventPublisher publisher;

    public BarberService(final BarberRepository barberRepository,
            final ApplicationEventPublisher publisher) {
        this.barberRepository = barberRepository;
        this.publisher = publisher;
    }

    public List<BarberDTO> findAll() {
        final List<Barber> barbers = barberRepository.findAll(Sort.by("id"));
        return barbers.stream()
                .map(barber -> mapToDTO(barber, new BarberDTO()))
                .toList();
    }

    public BarberDTO get(final UUID id) {
        return barberRepository.findById(id)
                .map(barber -> mapToDTO(barber, new BarberDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public UUID create(final BarberDTO barberDTO) {
        final Barber barber = new Barber();
        mapToEntity(barberDTO, barber);
        return barberRepository.save(barber).getId();
    }

    public void update(final UUID id, final BarberDTO barberDTO) {
        final Barber barber = barberRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(barberDTO, barber);
        barberRepository.save(barber);
    }

    public void delete(final UUID id) {
        final Barber barber = barberRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteBarber(id));
        barberRepository.delete(barber);
    }

    private BarberDTO mapToDTO(final Barber barber, final BarberDTO barberDTO) {
        barberDTO.setId(barber.getId());
        barberDTO.setName(barber.getName());
        barberDTO.setTell(barber.getTell());
        barberDTO.setEmail(barber.getEmail());
        barberDTO.setDocumentCPF(barber.getDocumentCPF());
        barberDTO.setMainSkill(barber.getMainSkill());
        barberDTO.setSecondSkill(barber.getSecondSkill());
        barberDTO.setThirdSkill(barber.getThirdSkill());
        barberDTO.setBarberShop(barber.getBarberShop());
        return barberDTO;
    }

    private Barber mapToEntity(final BarberDTO barberDTO, final Barber barber) {
        barber.setName(barberDTO.getName());
        barber.setTell(barberDTO.getTell());
        barber.setEmail(barberDTO.getEmail());
        barber.setDocumentCPF(barberDTO.getDocumentCPF());
        barber.setMainSkill(barberDTO.getMainSkill());
        barber.setSecondSkill(barberDTO.getSecondSkill());
        barber.setThirdSkill(barberDTO.getThirdSkill());
        barber.setBarberShop(barberDTO.getBarberShop());
        return barber;
    }

    public boolean tellExists(final String tell) {
        return barberRepository.existsByTellIgnoreCase(tell);
    }

    public boolean emailExists(final String email) {
        return barberRepository.existsByEmailIgnoreCase(email);
    }

    public boolean documentCPFExists(final String documentCPF) {
        return barberRepository.existsByDocumentCPFIgnoreCase(documentCPF);
    }

}
