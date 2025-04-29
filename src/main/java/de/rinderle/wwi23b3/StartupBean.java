package de.rinderle.wwi23b3;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.openapitools.client.api.DefaultApi;
import org.openapitools.client.model.*;

import java.math.BigDecimal;

@Component
public class StartupBean {

    private static final int SIZE = 5; // Spielfeldgröße 5x5
    private DefaultApi api;
    private BigDecimal gameId;
    private int x;
    private int y;

    @PostConstruct
    public void init() {
        try {
            api = new DefaultApi();

            GameInputDto gameInput = new GameInputDto();
            gameInput.setGroupName("KA-WWI23-B3");

            GameDto game = api.gamePost(gameInput);
            gameId = game.getGameId();
            x = game.getPosition().getPositionX().intValue();
            y = game.getPosition().getPositionY().intValue();

            System.out.println("Neues Spiel gestartet! Game-ID: " + gameId);
            System.out.println("Start-Position: (" + x + ", " + y + ")");

            play();

        } catch (Exception e) {
            System.err.println("Fehler im Maze-Client:");
            e.printStackTrace();
        }
    }

    private void play() throws Exception {
        while (true) {
            boolean moved = false;

            // PHASE 1: Hoch, Hoch, Hoch so lange es geht
            while (tryMove(DirectionDto.UP)) {
                moved = true;
            }

            // PHASE 2: Wenn Hoch blockiert, einmal nach Rechts
            if (tryMove(DirectionDto.RIGHT)) {
                moved = true;

                // Nach Rechts wieder PHASE 1: Hoch, Hoch, Hoch
                while (tryMove(DirectionDto.UP)) {
                    moved = true;
                }
            }

            // Wenn weder Hoch noch Rechts geht
            if (!moved) {
                System.out.println("Keine möglichen Züge mehr. Spiel beendet.");
                return;
            }

            // Prüfen ob Spielziel erreicht wurde
            GameDto currentGameState = api.gameGameIdGet(gameId);
            if ("success".equals(currentGameState.getStatus().toString())) {
                System.out.println("Ziel erreicht!");
                return;
            }
        }
    }

    private boolean tryMove(DirectionDto direction) {
        int newX = x;
        int newY = y;

        switch (direction) {
            case UP -> newY++;
            case RIGHT -> newX++;
            case LEFT -> newX--;
            case DOWN -> newY--;
        }

        // Spielfeldgrenzen prüfen
        if (newX < 0 || newX >= SIZE || newY < 0 || newY >= SIZE) {
            return false;
        }

        MoveInputDto moveInput = new MoveInputDto().direction(direction);
        MoveDto moveResult;

        try {
            moveResult = api.gameGameIdMovePost(gameId, moveInput);
        } catch (Exception e) {
            System.out.println("API Fehler bei Bewegung: " + direction);
            return false;
        }

        System.out.println("Bewegung: " + direction);
        System.out.println("Neues Feld: (" + moveResult.getPositionAfterMove().getPositionX() + ", " + moveResult.getPositionAfterMove().getPositionY() + ")");
        System.out.println("Move-Status: " + moveResult.getMoveStatus());

        if ("moved".equals(moveResult.getMoveStatus())) {
            x = moveResult.getPositionAfterMove().getPositionX().intValue();
            y = moveResult.getPositionAfterMove().getPositionY().intValue();
            return true;
        }

        return false;
    }
}