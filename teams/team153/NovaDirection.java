package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class NovaDirection {
    // should add final keyword here later
    public static NovaDirection
            north = new NovaDirection(Direction.NORTH, 0, -1),
            northWest = new NovaDirection(Direction.NORTH_WEST, -1, -1),
            northEast = new NovaDirection(Direction.NORTH_WEST, 1, -1),
            east = new NovaDirection(Direction.EAST, 1, 0),
            west = new NovaDirection(Direction.WEST, -1, 0),
            southWest = new NovaDirection(Direction.SOUTH_WEST, -1, 1),
            south = new NovaDirection(Direction.SOUTH, 0, 1),
            southEast = new NovaDirection(Direction.SOUTH_EAST, 1, 1),
            none = new NovaDirection(Direction.NONE, 0, 0);

    public Direction direction;
    public int xDelta, yDelta;

    public NovaDirection(Direction direction, int xDelta, int yDelta) {
        this.direction = direction;
        this.xDelta = xDelta;
        this.yDelta = yDelta;
    }

    public NovaDirection getNovaDirection(Direction direction) {
        return null;
    }
}
