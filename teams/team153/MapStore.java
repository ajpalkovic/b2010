package team153;

import java.util.*;
import battlecode.common.*;

public class MapStore {

    public NovaMapData[][] map;

    public static final int size = 180;
    public MapStore() {
        map = new NovaMapData[size][size];
    }

    public NovaMapData _get(int x, int y) {
        return map[x % size][y % size];
    }

    public NovaMapData get(int x, int y) {
        NovaMapData ret = _get(x, y);
        if(ret == null) {
            ret = new NovaMapData(x, y);
            map[x % size][y % size] = ret;
        }
        return ret;
    }

    public NovaMapData get(MapLocation location) {
        return get(location.getX(), location.getY());
    }

    public NovaMapData[] getSurroundingSquares(int x, int y) {
        NovaMapData[] squares = new NovaMapData[8];
        squares[0] = _get(x - 1, y - 1);
        squares[1] = _get(x, y - 1);
        squares[2] = _get(x + 1, y - 1);

        squares[3] = _get(x - 1, y);
        squares[4] = _get(x + 1, y);

        squares[5] = _get(x - 1, y + 1);
        squares[6] = _get(x, y + 1);
        squares[7] = _get(x + 1, y + 1);

        int count = 0;
        for(NovaMapData square : squares) {
            if(square != null && square.onMap()) {
                count++;
            }
        }

        NovaMapData[] ret = new NovaMapData[count];
        int index = 0;
        NovaMapData square;
        for(int c = 0; c < 8; c++) {
            square = squares[c];
            if(square != null && square.onMap()) {
                ret[index] = square;
                index++;
            }
        }

        return ret;
    }

    public NovaMapData[] getForwardSquares(int x, int y, int previousXDelta, int previousYDelta, boolean diagonal) {
        NovaMapData[] squares;
        if(diagonal) {
            if(previousXDelta == -1) {
                if(previousYDelta == -1) //top left
                {
                    squares = new NovaMapData[] {get(x - 1, y - 1), get(x, y - 1), get(x - 1, y)};
                } else //bottom left
                {
                    squares = new NovaMapData[] {get(x + 1, y + 1), get(x - 1, y), get(x, y + 1)};
                }
            } else if(previousYDelta == -1) //top right
            {
                squares = new NovaMapData[] {get(x + 1, y - 1), get(x, y - 1), get(x + 1, y)};
            } else //bottom right
            {
                squares = new NovaMapData[] {get(x + 1, y + 1), get(x + 1, y), get(x, y + 1)};
            }
        } else if(previousXDelta == -1) // left
        {
            squares = new NovaMapData[] {get(x - 1, y), get(x - 1, y - 1), get(x - 1, y + 1)};
        } else if(previousXDelta == 1) //right
        {
            squares = new NovaMapData[] {get(x + 1, y), get(x + 1, y - 1), get(x + 1, y + 1)};
        } else if(previousYDelta == -1) //top
        {
            squares = new NovaMapData[] {get(x, y - 1), get(x - 1, y - 1), get(x + 1, y - 1)};
        } else if(previousYDelta == 1) //bottom
        {
            squares = new NovaMapData[] {get(x, y + 1), get(x - 1, y + 1), get(x + 1, y + 1)};
        } else // first square
        {
            squares = new NovaMapData[] {get(x - 1, y - 1), get(x, y - 1), get(x + 1, y - 1), get(x - 1, y), get(x + 1, y), get(x - 1, y + 1), get(x, y + 1), get(x + 1, y + 1)};
        }

        return squares;
    }
}
