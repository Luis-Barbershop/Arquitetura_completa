package ifsp.edu.projeto.cortaai.controller;

import ifsp.edu.projeto.cortaai.dto.AppointmentsDTO;
import ifsp.edu.projeto.cortaai.service.impl.AppointmentsServiceImpl;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/appointmentss", produces = MediaType.APPLICATION_JSON_VALUE)
public class AppointmentsController {

    private final AppointmentsServiceImpl appointmentsServiceImpl;

    public AppointmentsController(final AppointmentsServiceImpl appointmentsServiceImpl) {
        this.appointmentsServiceImpl = appointmentsServiceImpl;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentsDTO>> getAllAppointmentss() {
        return ResponseEntity.ok(appointmentsServiceImpl.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentsDTO> getAppointments(
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(appointmentsServiceImpl.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createAppointments(
            @RequestBody @Valid final AppointmentsDTO appointmentsDTO) {
        final Long createdId = appointmentsServiceImpl.create(appointmentsDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateAppointments(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final AppointmentsDTO appointmentsDTO) {
        appointmentsServiceImpl.update(id, appointmentsDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteAppointments(@PathVariable(name = "id") final Long id) {
        appointmentsServiceImpl.delete(id);
        return ResponseEntity.noContent().build();
    }

}
