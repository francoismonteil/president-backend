package fr.asser.presidentgame.mapper;

import fr.asser.presidentgame.dto.GameStateDTO;
import fr.asser.presidentgame.dto.PlayerDTO;
import fr.asser.presidentgame.model.Game;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GameMapper {
    public GameStateDTO toGameStateDTO(Game game, String username) {
        GameStateDTO dto = new GameStateDTO();
        dto.setGameId(game.getId());
        dto.setJoinCode(game.getJoinCode());
        dto.setState(game.getState());

        // Parcourir les joueurs pour créer la liste du DTO
        List<PlayerDTO> playerDTOs = game.getPlayers().stream().map(player -> {
            PlayerDTO playerDTO = new PlayerDTO();
            playerDTO.setId(player.getId());
            playerDTO.setName(player.getName());
            // Pour le joueur connecté, inclure la main complète, sinon ne montrer que le nombre de cartes
            if (player.getName().equals(username)) {
                playerDTO.setHand(player.getHand());
            } else {
                playerDTO.setCardsCount(player.getHand().size());
            }
            return playerDTO;
        }).collect(Collectors.toList());
        dto.setPlayers(playerDTOs);

        // Ajouter les autres informations utiles (cartes jouées, règles actives, etc.)
        dto.setPlayedCards(game.getPlayedCards());
        dto.setRuleEngine(game.getRuleEngine());
        dto.setDeck(game.getDeck().stream().toList());

        return dto;
    }
}
