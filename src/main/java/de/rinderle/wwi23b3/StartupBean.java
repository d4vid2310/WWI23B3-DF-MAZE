package de.rinderle.wwi23b3;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.openapitools.client.api.DefaultApi;
import org.openapitools.client.model.*;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@Component
public class StartupBean {

    private static final int SIZE = 5;
    private static final Pos GOAL = new Pos(5, 5);

    private final DefaultApi api = new DefaultApi();
    private BigDecimal gameId;
    private int x, y;

    private static record Pos(int x, int y) {
    }

    private final Set<Pos> visited = new HashSet<>();
    private final Set<Pos> walls = new HashSet<>();
    private final Deque<Pos> stack = new ArrayDeque<>();

    private final DirectionDto[] priority = {
            DirectionDto.UP,
            DirectionDto.RIGHT,
            DirectionDto.DOWN,
            DirectionDto.LEFT
    };

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        GameDto game = api.gamePost(new GameInputDto().groupName("KA-WWI23-B3"));
        gameId = game.getGameId();
        x = game.getPosition().getPositionX().intValue();
        y = game.getPosition().getPositionY().intValue();
        Pos start = new Pos(x, y);
        System.out.println("Neues Spiel: ID=" + gameId + ", Start=" + start);
        solve(start);
    }

    private void solve(Pos start) {
        visited.add(start);
        stack.push(start);

        while (!stack.isEmpty()) {
            Pos curr = stack.peek();
            if (curr.equals(GOAL)) {
                System.out.println("Ziel erreicht bei " + curr);
                return;
            }

            boolean moved = false;
            for (DirectionDto dir : priority) {
                Pos next = movePos(curr, dir);

                String status;
                Pos posAfter = curr;

                if (!isValid(next)) {
                    status = "INVALID";
                } else if (visited.contains(next) || walls.contains(next)) {
                    status = "SKIPPED";
                } else {
                    Pos movedTo = tryMove(dir);
                    if (movedTo != null) {
                        visited.add(movedTo);
                        stack.push(movedTo);
                        status = "OK";
                        posAfter = movedTo;
                        moved = true;
                        System.out.printf("%-6s (%d, %d) nach (%d, %d): %s -> (%d, %d)%n",
                                dir, curr.x, curr.y, next.x, next.y, status, posAfter.x, posAfter.y);
                        break;
                    } else {
                        walls.add(next);
                        status = "BLOCKED";
                    }
                }
                System.out.printf("%-6s (%d, %d) nach (%d, %d): %s -> (%d, %d)%n",
                        dir, curr.x, curr.y, next.x, next.y, status, posAfter.x, posAfter.y);
            }

            if (!moved) {
                if (stack.size() <= 1) {
                    System.out.println("Kein Weg mehr zum Ziel. Spiel beendet.");
                    return;
                }
                stack.pop();
                Pos back = stack.peek();
                x = back.x;
                y = back.y;
                System.out.printf("BACKTRACK -> (%d, %d)%n", back.x, back.y);
            }
        }

        System.out.println("Kein Weg zum Ziel");
    }

    private Pos tryMove(DirectionDto dir) {
        try {
            MoveDto res = api.gameGameIdMovePost(
                    gameId,
                    new MoveInputDto().direction(dir));
            if (MoveStatusDto.MOVED.equals(res.getMoveStatus())) {
                x = res.getPositionAfterMove().getPositionX().intValue();
                y = res.getPositionAfterMove().getPositionY().intValue();
                Pos p = new Pos(x, y);
                return p;
            } else {
                return null;
            }
        } catch (HttpClientErrorException bre) {
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private Pos movePos(Pos p, DirectionDto d) {
        return switch (d) {
            case UP -> new Pos(p.x, p.y + 1);
            case DOWN -> new Pos(p.x, p.y - 1);
            case LEFT -> new Pos(p.x - 1, p.y);
            case RIGHT -> new Pos(p.x + 1, p.y);
        };
    }

    private boolean isValid(Pos p) {
        return p.x >= 1 && p.x <= SIZE && p.y >= 1 && p.y <= SIZE;
    }
}