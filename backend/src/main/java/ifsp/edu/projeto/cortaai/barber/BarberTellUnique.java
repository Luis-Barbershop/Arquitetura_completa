package ifsp.edu.projeto.cortaai.barber;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.servlet.HandlerMapping;


/**
 * Validate that the tell value isn't taken yet.
 */
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = BarberTellUnique.BarberTellUniqueValidator.class
)
public @interface BarberTellUnique {

    String message() default "{Exists.barber.tell}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class BarberTellUniqueValidator implements ConstraintValidator<BarberTellUnique, String> {

        private final BarberService barberService;
        private final HttpServletRequest request;

        public BarberTellUniqueValidator(final BarberService barberService,
                final HttpServletRequest request) {
            this.barberService = barberService;
            this.request = request;
        }

        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext cvContext) {
            if (value == null) {
                // no value present
                return true;
            }
            @SuppressWarnings("unchecked") final Map<String, String> pathVariables =
                    ((Map<String, String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
            final String currentId = pathVariables.get("id");
            if (currentId != null && value.equalsIgnoreCase(barberService.get(UUID.fromString(currentId)).getTell())) {
                // value hasn't changed
                return true;
            }
            return !barberService.tellExists(value);
        }

    }

}
