package ifsp.edu.projeto.cortaai.validator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import ifsp.edu.projeto.cortaai.service.impl.CustomerServiceImpl;
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
 * Validate that the email value isn't taken yet.
 */
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = CustomerEmailUnique.CustomerEmailUniqueValidator.class
)
public @interface CustomerEmailUnique {

    String message() default "{Exists.customer.email}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CustomerEmailUniqueValidator implements ConstraintValidator<CustomerEmailUnique, String> {

        private final CustomerServiceImpl customerServiceImpl;
        private final HttpServletRequest request;

        public CustomerEmailUniqueValidator(final CustomerServiceImpl customerServiceImpl,
                final HttpServletRequest request) {
            this.customerServiceImpl = customerServiceImpl;
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
            if (currentId != null && value.equalsIgnoreCase(customerServiceImpl.get(UUID.fromString(currentId)).getEmail())) {
                // value hasn't changed
                return true;
            }
            return !customerServiceImpl.emailExists(value);
        }

    }

}
