package com.tecup.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String place;
    private Date start_date;
    private Date end_date;
    private int max_participants_group;
    private boolean statusEvent = true;
    private String img_event;

    @ManyToOne
    @JoinColumn(name = "organizador_id")
    private User organizador_id;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true) // Relación con Inscription
    private Set<Inscription> inscriptions = new HashSet<>();

    public Event() {}

    public Event(Long id, String name, String description, String place, Date start_date, Date end_date,int max_participants_group, boolean statusEvent, String img_event, User organizador_id) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.place = place;
        this.start_date = start_date;
        this.end_date = end_date;
        this.max_participants_group = max_participants_group;
        this.statusEvent = statusEvent;
        this.img_event = img_event;
        this.organizador_id = organizador_id;
    }
}
