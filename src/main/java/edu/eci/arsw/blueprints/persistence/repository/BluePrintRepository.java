package edu.eci.arsw.blueprints.persistence.repository;

import edu.eci.arsw.blueprints.persistence.entity.BluePrintEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BluePrintRepository extends JpaRepository<BluePrintEntity, Long> {

    Optional<BluePrintEntity> findByAuthorAndName(String author, String name);

    List<BluePrintEntity> findByAuthor(String author);
}
