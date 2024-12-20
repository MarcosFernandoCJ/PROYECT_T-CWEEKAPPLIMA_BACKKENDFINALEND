package com.tecup.backend.controllers;

import com.tecup.backend.models.Department;
import com.tecup.backend.models.Event;
import com.tecup.backend.models.GroupEvent;
import com.tecup.backend.models.User;
import com.tecup.backend.payload.repository.DepartmentRepository;
import com.tecup.backend.payload.repository.EventRepository;
import com.tecup.backend.payload.repository.GroupEventRepository;
import com.tecup.backend.payload.repository.UserRepository;
import com.tecup.backend.payload.request.EventRequest;
import com.tecup.backend.payload.response.EventResponse;
import com.tecup.backend.payload.response.MessageResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/events")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private GroupEventRepository groupEventRepository;

    // Obtener todos los eventos
    @GetMapping("/all")
    @PreAuthorize("hasRole('USER') or hasRole('ORGANIZADOR') or hasRole('ADMIN') or hasRole('JURADO')")
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream().map(event ->
                new EventResponse(
                        event.getId(),
                        event.getName(),
                        event.getDescription(),
                        event.getPlace(),
                        event.getImg_event(),
                        event.getOrganizador_id() != null ? event.getOrganizador_id().getUsername() : "No Organizador"
                )
        ).collect(Collectors.toList());
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<?> addEvent(@Valid @RequestBody EventRequest eventRequest) {
        logger.info("Creando un nuevo evento: {}", eventRequest.getName());

        // Obtener usuario autenticado
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Usuario autenticado no encontrado."));
        }

        // Validar fechas
        if (eventRequest.getStartDate() == null || eventRequest.getEndDate() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Las fechas de inicio y fin son obligatorias."));
        }
        if (eventRequest.getStartDate().after(eventRequest.getEndDate())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: La fecha de inicio no puede ser posterior a la fecha de fin."));
        }

        // Crear evento
        Event event = new Event();
        event.setName(eventRequest.getName());
        event.setDescription(eventRequest.getDescription());
        event.setPlace(eventRequest.getPlace());
        event.setStart_date(eventRequest.getStartDate());
        event.setEnd_date(eventRequest.getEndDate());
        event.setMax_participants_group(eventRequest.getMax_participants_group()); // Campo requerido
        event.setStatusEvent(true); // Se establece como true por defecto
        event.setImg_event(eventRequest.getImgEvent());
        event.setOrganizador_id(user.get());

        // Guardar el evento
        Event savedEvent = eventRepository.save(event);

        // Crear grupos por cada departamento
        List<Department> departments = departmentRepository.findAll();
        List<GroupEvent> groupEvents = departments.stream()
                .map(department -> new GroupEvent(department.getName(), savedEvent, department))
                .collect(Collectors.toList());

        groupEventRepository.saveAll(groupEvents); // Guardar los grupos

        return ResponseEntity.ok(new MessageResponse("Evento creado exitosamente con " + groupEvents.size() + " grupos."));
    }

    @PutMapping("/update/{eventId}")
    @PreAuthorize("hasRole('ORGANIZADOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updateEvent(@PathVariable Long eventId, @Valid @RequestBody EventRequest eventRequest) {
        // Buscar el evento por ID
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Evento no encontrado."));
        }

        Event event = eventOptional.get();

        // Validar fechas
        if (eventRequest.getStartDate() == null || eventRequest.getEndDate() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Las fechas de inicio y fin son obligatorias."));
        }
        if (eventRequest.getStartDate().after(eventRequest.getEndDate())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: La fecha de inicio no puede ser posterior a la fecha de fin."));
        }

        // Actualizar campos del evento
        event.setName(eventRequest.getName());
        event.setDescription(eventRequest.getDescription());
        event.setPlace(eventRequest.getPlace());
        event.setStart_date(eventRequest.getStartDate());
        event.setEnd_date(eventRequest.getEndDate());
        event.setMax_participants_group(eventRequest.getMax_participants_group());
        event.setImg_event(eventRequest.getImgEvent());

        // Guardar cambios en el repositorio
        eventRepository.save(event);

        return ResponseEntity.ok(new MessageResponse("Evento actualizado exitosamente."));
    }

    @DeleteMapping("/delete/{eventId}")
    @PreAuthorize("hasRole('ORGANIZADOR')")
    public ResponseEntity<?> deleteEvent(@PathVariable Long eventId) {
        logger.info("Intentando eliminar el evento con ID: {}", eventId);

        // Buscar el evento por su ID
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Evento no encontrado."));
        }

        Event event = eventOptional.get();

        // Eliminar grupos asociados al evento
        List<GroupEvent> groupsToDelete = groupEventRepository.findByEvent(event);
        if (!groupsToDelete.isEmpty()) {
            groupEventRepository.deleteAll(groupsToDelete);
            logger.info("Grupos asociados al evento eliminados.");
        }

        // Eliminar el evento
        eventRepository.delete(event);
        logger.info("Evento eliminado exitosamente.");

        return ResponseEntity.ok(new MessageResponse("Evento y sus grupos asociados eliminados exitosamente."));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ORGANIZADOR') or hasRole('ADMIN') or hasRole('JURADO')")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        logger.info("Buscando detalles del evento con ID: {}", id);

        // Buscar el evento por su ID
        Optional<Event> eventOptional = eventRepository.findById(id);

        if (eventOptional.isEmpty()) {
            logger.error("Evento con ID {} no encontrado.", id);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Evento no encontrado."));
        }

        Event event = eventOptional.get();

        // Crear la respuesta
        EventResponse eventResponse = new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getPlace(),
                event.getImg_event(),
                event.getOrganizador_id() != null ? event.getOrganizador_id().getUsername() : "No Organizador"
        );

        return ResponseEntity.ok(eventResponse);
    }
}
