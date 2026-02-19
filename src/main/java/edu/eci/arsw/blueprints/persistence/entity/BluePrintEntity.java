package edu.eci.arsw.blueprints.persistence.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blueprints")
public class BluePrintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String author;

    private String name;

    @OneToMany(
            mappedBy = "blueprint",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PointEntity> points = new ArrayList<>();

    public BluePrintEntity() {
    }

    public BluePrintEntity(String author, String name) {
        this.author = author;
        this.name = name;
    }

    public void addPoint(PointEntity point) {
        point.setBlueprint(this);
        this.points.add(point);
    }

    // ===== Getters and Setters =====

    public Long getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PointEntity> getPoints() {
        return points;
    }

    public void setPoints(List<PointEntity> points) {
        this.points = points;
    }
}
