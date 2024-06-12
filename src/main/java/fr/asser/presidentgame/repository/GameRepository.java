package fr.asser.presidentgame.repository;

import fr.asser.presidentgame.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
