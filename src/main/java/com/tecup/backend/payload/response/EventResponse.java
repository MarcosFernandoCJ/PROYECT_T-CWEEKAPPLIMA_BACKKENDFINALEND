package com.tecup.backend.payload.response;

public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private String place;
    private String imgEvent;
    private String organizador;

    public EventResponse(Long id, String name, String description, String place, String imgEvent,String organizador) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.place = place;
        this.imgEvent = imgEvent;
        this.organizador = organizador;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getImgEvent() {
        return imgEvent;
    }

    public void setImgEvent(String imgEvent) {
        this.imgEvent = imgEvent;
    }

    public String getOrganizador() {
        return organizador;
    }

    public void setOrganizador(String organizador) {
        this.organizador = organizador;
    }
}
