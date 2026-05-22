package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.AssignActivitiesDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UploadResultDTO;
import ifsp.edu.projeto.cortaai.userservice.exception.ExternalServiceUnavailableException;
import ifsp.edu.projeto.cortaai.userservice.mapper.BarberMapper;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.service.impl.BarberServiceImpl;
import ifsp.edu.projeto.cortaai.userservice.service.storage.StorageService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarberServiceImplTest {

    @Mock
    private BarberRepository barberRepository;
    @Mock
    private FirebaseAuthService firebaseAuthService;
    @Mock
    private StorageService storageService;

    private BarberServiceImpl service;

    @BeforeEach
    void setUp() {
        BarberMapper mapper = Mappers.getMapper(BarberMapper.class);
        service = new BarberServiceImpl(barberRepository, mapper, firebaseAuthService, storageService);
    }

    @Test
    void shouldUpdateOwnerProfileAndPropagateFirebaseClaims() {
        UUID id = UUID.randomUUID();
        Barber barber = barber(id, true);
        barber.setFirebaseUid("barber-uid");
        UpdateBarberDTO dto = new UpdateBarberDTO();
        dto.setName("Novo Nome");
        dto.setTell("11988887777");
        dto.setEmail(" Novo@Email.COM ");
        dto.setBirthDate(LocalDate.of(1990, 4, 5));
        dto.setWorkStartTime(LocalTime.of(8, 30));
        dto.setWorkEndTime(LocalTime.of(17, 30));
        dto.setActAsBarber(false);
        when(barberRepository.findById(id)).thenReturn(Optional.of(barber));
        when(barberRepository.save(barber)).thenReturn(barber);

        BarberDTO result = service.update(id, dto);

        assertThat(result.name()).isEqualTo("Novo Nome");
        assertThat(result.email()).isEqualTo("novo@email.com");
        assertThat(result.actAsBarber()).isFalse();
        assertThat(result.workStartTime()).isEqualTo(LocalTime.of(8, 30));
        verify(firebaseAuthService).setCustomUserClaims("barber-uid", "BARBER", false);
    }

    @Test
    void shouldForceNonOwnerToActAsBarberWhenUpdating() {
        UUID id = UUID.randomUUID();
        Barber barber = barber(id, false);
        barber.setActAsBarber(false);
        UpdateBarberDTO dto = new UpdateBarberDTO();
        dto.setActAsBarber(false);
        when(barberRepository.findById(id)).thenReturn(Optional.of(barber));
        when(barberRepository.save(barber)).thenReturn(barber);

        BarberDTO result = service.update(id, dto);

        assertThat(result.actAsBarber()).isTrue();
    }

    @Test
    void shouldThrowWhenUpdatingMissingBarber() {
        UUID id = UUID.randomUUID();
        when(barberRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UpdateBarberDTO()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Barbeiro não encontrado.");
    }

    @Test
    void shouldFindByIdAndListAllBarbers() {
        UUID id = UUID.randomUUID();
        Barber barber = barber(id, false);
        when(barberRepository.findById(id)).thenReturn(Optional.of(barber));
        when(barberRepository.findAll()).thenReturn(List.of(barber));

        assertThat(service.findById(id).id()).isEqualTo(id);
        assertThat(service.findAll()).extracting(BarberDTO::id).containsExactly(id);
    }

    @Test
    void shouldFindActiveBarbersByBarbershopId() {
        UUID shopId = UUID.randomUUID();
        Barber barber = barber(UUID.randomUUID(), false);
        barber.setBarbershopId(shopId);
        when(barberRepository.findActiveByBarbershopId(shopId)).thenReturn(List.of(barber));

        List<BarberDTO> result = service.findByBarbershopId(shopId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).barbershopId()).isEqualTo(shopId);
    }

    @Test
    void shouldUpdateProfilePhotoAndWrapStorageFailure() throws IOException {
        Barber barber = barber(UUID.randomUUID(), false);
        barber.setImageUrlPublicId("old-photo");
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(barberRepository.findByFirebaseUid("barber-uid")).thenReturn(Optional.of(barber));
        when(storageService.uploadFile(file, "barber-profiles"))
                .thenReturn(new UploadResultDTO("new-photo", "https://cdn/barber.png"));

        String result = service.updateProfilePhotoByFirebaseUid("barber-uid", file);

        assertThat(result).isEqualTo("https://cdn/barber.png");
        assertThat(barber.getImageUrlPublicId()).isEqualTo("new-photo");
        verify(storageService).deleteFile("old-photo");
        verify(barberRepository).save(barber);

        MultipartFile failingFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(storageService.uploadFile(failingFile, "barber-profiles")).thenThrow(new IOException("cdn off"));

        assertThatThrownBy(() -> service.updateProfilePhotoByFirebaseUid("barber-uid", failingFile))
                .isInstanceOf(ExternalServiceUnavailableException.class)
                .hasMessageContaining("Falha ao fazer upload da foto");
    }

    @Test
    void shouldReturnAssignedActivitiesByFirebaseUidAndId() {
        UUID activityId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        Barber barber = barber(barberId, false);
        barber.setAssignedActivityIds(new HashSet<>(Set.of(activityId)));
        when(barberRepository.findByFirebaseUid("barber-uid")).thenReturn(Optional.of(barber));
        when(barberRepository.findById(barberId)).thenReturn(Optional.of(barber));

        assertThat(service.getAssignedActivityIds("barber-uid")).containsExactly(activityId);
        assertThat(service.getAssignedActivityIdsById(barberId)).containsExactly(activityId);
    }

    @Test
    void shouldAssignActivitiesFilteringNullsAndAvoidingUnnecessarySave() {
        UUID activityId = UUID.randomUUID();
        Barber barber = barber(UUID.randomUUID(), false);
        barber.setAssignedActivityIds(new HashSet<>(Set.of(activityId)));
        when(barberRepository.findByFirebaseUidForUpdate("barber-uid")).thenReturn(Optional.of(barber));

        Set<UUID> unchanged = service.assignActivities("barber-uid", new AssignActivitiesDTO(List.of(activityId)));

        assertThat(unchanged).containsExactly(activityId);
        verify(barberRepository, never()).save(any());

        UUID newActivity = UUID.randomUUID();
        Set<UUID> changed = service.assignActivities("barber-uid", new AssignActivitiesDTO(java.util.Arrays.asList(newActivity, null)));

        assertThat(changed).containsExactly(newActivity);
        verify(barberRepository).save(barber);
    }

    @Test
    void shouldDelegateUniquenessChecksWithNormalizedInputs() {
        when(barberRepository.existsByEmailIgnoreCase("barber@example.com")).thenReturn(true);
        when(barberRepository.existsByDocumentCPFIgnoreCase("12345678901")).thenReturn(true);
        when(barberRepository.existsByTellIgnoreCase("11999999999")).thenReturn(true);

        assertThat(service.emailExists(" Barber@Example.COM ")).isTrue();
        assertThat(service.documentCPFExists("123.456.789-01")).isTrue();
        assertThat(service.tellExists("11999999999")).isTrue();
    }

    private Barber barber(UUID id, boolean owner) {
        Barber barber = new Barber();
        barber.setId(id);
        barber.setName("Barbeiro");
        barber.setEmail("barber@example.com");
        barber.setTell("11999999999");
        barber.setDocumentCPF("12345678901");
        barber.setOwner(owner);
        barber.setActAsBarber(true);
        barber.setBirthDate(LocalDate.of(1990, 1, 1));
        barber.setWorkStartTime(LocalTime.of(9, 0));
        barber.setWorkEndTime(LocalTime.of(18, 0));
        barber.setAssignedActivityIds(new HashSet<>());
        return barber;
    }
}
