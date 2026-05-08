package ifsp.edu.projeto.cortaai.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(
        @NotBlank
        @Size(max = 80)
        String name
) {}
