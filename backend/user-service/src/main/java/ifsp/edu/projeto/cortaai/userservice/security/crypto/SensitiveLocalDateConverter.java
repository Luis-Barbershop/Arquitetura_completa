package ifsp.edu.projeto.cortaai.userservice.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;

@Converter
public class SensitiveLocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute == null ? null : DataCrypto.encrypt(attribute.toString());
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        String decrypted = DataCrypto.decrypt(dbData);
        return decrypted == null || decrypted.isBlank() ? null : LocalDate.parse(decrypted);
    }
}
