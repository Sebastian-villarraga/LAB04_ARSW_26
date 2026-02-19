package edu.eci.arsw.blueprints.persistence.entity;

import jakarta.persistence.*;



@Entity
@Table(name = "points")
public class PointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int x;

    private int y;

    @ManyToOne
    @JoinColumn(name = "blueprint_id")
    private BluePrintEntity blueprint;

    public PointEntity() {
    }

    public PointEntity(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public PointEntity(int x, int y, BluePrintEntity blueprint) {
        this.x = x;
        this.y = y;
        this.blueprint = blueprint;
    }

    // ===== Getters and Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public BluePrintEntity getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BluePrintEntity blueprint) {
        this.blueprint = blueprint;
    }
}
