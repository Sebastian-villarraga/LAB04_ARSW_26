package edu.eci.arsw.blueprints.persistence.impl;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistence;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.persistence.entity.BluePrintEntity;
import edu.eci.arsw.blueprints.persistence.entity.PointEntity;
import edu.eci.arsw.blueprints.persistence.repository.BluePrintRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Primary
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final BluePrintRepository repository;

    public PostgresBlueprintPersistence(BluePrintRepository repository) {
        this.repository = repository;
    }

    // ================================
    // SAVE
    // ================================
    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {

        if (repository.findByAuthorAndName(bp.getAuthor(), bp.getName()).isPresent()) {
            throw new BlueprintPersistenceException("Blueprint already exists");
        }

        BluePrintEntity entity = new BluePrintEntity(bp.getAuthor(), bp.getName());

        if (bp.getPoints() != null) {
            bp.getPoints().forEach(p ->
                    entity.addPoint(new PointEntity(p.x(), p.y()))
            );
        }

        repository.save(entity);
    }

    // ================================
    // GET ONE
    // ================================
    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {

        BluePrintEntity entity = repository.findByAuthorAndName(author, name)
                .orElseThrow(() ->
                        new BlueprintNotFoundException("Blueprint not found"));

        return mapToModel(entity);
    }

    // ================================
    // GET BY AUTHOR
    // ================================
    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {

        var list = repository.findByAuthor(author);

        if (list.isEmpty()) {
            throw new BlueprintNotFoundException("No blueprints found for author: " + author);
        }

        return list.stream()
                .map(this::mapToModel)
                .collect(Collectors.toSet());
    }

    // ================================
    // GET ALL
    // ================================
    @Override
    public Set<Blueprint> getAllBlueprints() {

        return repository.findAll()
                .stream()
                .map(this::mapToModel)
                .collect(Collectors.toSet());
    }

    // ================================
    // ADD POINT
    // ================================
    @Override
    public void addPoint(String author, String name, int x, int y)
            throws BlueprintNotFoundException {

        BluePrintEntity entity = repository.findByAuthorAndName(author, name)
                .orElseThrow(() ->
                        new BlueprintNotFoundException("Blueprint not found"));

        entity.addPoint(new PointEntity(x, y));

        repository.save(entity);
    }

    // ================================
    // MAPPER ENTITY -> MODEL
    // ================================
    private Blueprint mapToModel(BluePrintEntity entity) {

        return new Blueprint(
                entity.getAuthor(),
                entity.getName(),
                entity.getPoints()
                        .stream()
                        .map(p -> new Point(p.getX(), p.getY()))
                        .toList()
        );
    }
}
