package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBarbershopReviewDTO {

    @NotNull(message = "A nota e obrigatoria.")
    @Min(value = 1, message = "A nota minima e 1.")
    @Max(value = 5, message = "A nota maxima e 5.")
    private Integer rating;

    @Size(max = 500, message = "O comentario deve ter no maximo 500 caracteres.")
    private String comment;
}

