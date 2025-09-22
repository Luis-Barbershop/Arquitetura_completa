package ifsp.edu.projeto.cortaai.customer;

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
 * Validate that the documentCPF value isn't taken yet.
 */
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = CustomerDocumentCPFUnique.CustomerDocumentCPFUniqueValidator.class
)
public @interface CustomerDocumentCPFUnique {

    String message() default "{Exists.customer.documentCPF}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CustomerDocumentCPFUniqueValidator implements ConstraintValidator<CustomerDocumentCPFUnique, String> {

        private final CustomerService customerService;
        private final HttpServletRequest request;

        public CustomerDocumentCPFUniqueValidator(final CustomerService customerService,
                final HttpServletRequest request) {
            this.customerService = customerService;
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
            if (currentId != null && value.equalsIgnoreCase(customerService.get(UUID.fromString(currentId)).getDocumentCPF())) {
                // value hasn't changed
                return true;
            }
            return !customerService.documentCPFExists(value);
        }

    }

}
