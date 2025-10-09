package ifsp.edu.projeto.cortaai.mapper;

import ifsp.edu.projeto.cortaai.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.model.Barber;
import org.springframework.stereotype.Component;

@Component
public class BarberMapper {

    public BarberDTO toDTO(Barber barber) {
        if (barber == null) {
            return null;
        }
        BarberDTO barberDTO = new BarberDTO();
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

    public Barber toEntity(BarberDTO barberDTO) {
        if (barberDTO == null) {
            return null;
        }
        Barber barber = new Barber();
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
}