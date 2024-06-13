package fr.asser.presidentgame.repository;

import fr.asser.presidentgame.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findAllByIsSaved(boolean isSaved);

    @Query("SELECT g FROM Game g LEFT JOIN FETCH g.players LEFT JOIN FETCH g.deck LEFT JOIN FETCH g.playedCards WHERE g.id = :id")
    Optional<Game> findByIdWithAssociations(@Param("id") Long id);
}
