package com.gaming.luckengine.controller;

import com.gaming.luckengine.domain.entity.Game;
import com.gaming.luckengine.domain.enums.GameStatus;
import com.gaming.luckengine.dto.GameCreateRequest;
import com.gaming.luckengine.service.GameService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Game management operations.
 */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<Game> createGame(@Valid @RequestBody GameCreateRequest request) {
        Game game = gameService.createGame(
                request.getStartTime(),
                request.getDurationMinutes(),
                request.getBrandContributions(),
                request.getWinProbability()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(game);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Game> getGameById(@PathVariable Long id) {
        Game game = gameService.getGameById(id);
        return ResponseEntity.ok(game);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Game>> getActiveGames() {
        List<Game> games = gameService.getActiveGames();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/latest-active")
    public ResponseEntity<Game> getLatestActiveGame() {
        Game game = gameService.getLatestActiveGame();
        return ResponseEntity.ok(game);
    }

    @GetMapping
    public ResponseEntity<List<Game>> getAllGames(
            @RequestParam(required = false) GameStatus status) {
        List<Game> games = status != null
                ? gameService.getGamesByStatus(status)
                : gameService.getAllGames();
        return ResponseEntity.ok(games);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Game> startGame(@PathVariable Long id) {
        Game game = gameService.startGame(id);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Game> completeGame(@PathVariable Long id) {
        Game game = gameService.completeGame(id);
        return ResponseEntity.ok(game);
    }
}

