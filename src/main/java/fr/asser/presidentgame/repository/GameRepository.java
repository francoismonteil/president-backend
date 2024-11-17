package fr.asser.presidentgame.repository;

import fr.asser.presidentgame.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface GameRepository extends JpaRepository<Game, Long> {
    Set<Game> findAllByIsSaved(boolean isSaved);

    @Query("SELECT g FROM Game g LEFT JOIN FETCH g.ranks WHERE g.id = :id")
    Optional<Game> findByIdWithRanks(@Param("id") Long id);
}