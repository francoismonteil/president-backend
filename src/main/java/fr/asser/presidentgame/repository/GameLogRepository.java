package fr.asser.presidentgame.repository;

import fr.asser.presidentgame.model.GameLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameLogRepository extends JpaRepository<GameLog, Long> {
}
