package team153;

import java.util.*;
import battlecode.common.*;

public class MapStore {

    public NovaMapData[][] map;

    public MapStore() {
        map = new NovaMapData[60][60];
    }

    public NovaMapData get(int x, int y) {
        return map[x % 60][y % 60];
    }

    public NovaMapData getOrCreate(int x, int y) {
        NovaMapData ret = get(x, y);
        if(ret == null) {
            ret = new NovaMapData(x, y);
            map[x % 60][y % 60] = ret;
        }
        return ret;
    }

    public NovaMapData getOrCreate(MapLocation location) {
        return getOrCreate(location.getX(), location.getY());
    }

    public NovaMapData getNotNull(int x, int y) {
        NovaMapData ret = get(x, y);
        if(ret == null) {
            ret = new NovaMapData(x, y);
        }
        return ret;
    }

    public NovaMapData getNotNull(MapLocation location) {
        return getNotNull(location.getX(), location.getY());
    }

    public NovaMapData get(MapLocation location) {
        return get(location.getX(), location.getY());
    }

    public void set(int x, int y, NovaMapData data) {
        NovaMapData cur = get(x, y);
        if(cur == null) {
            map[x % 60][y % 60] = data;
        } else {
        }

    }

    public void set(NovaMapData data) {
        set(data.x, data.y, data);
    }

    public void setAll(NovaMapData[] datas) {
        for(NovaMapData data : datas) {
            set(data);
        }
    }

    public NovaMapData[] getSurroundingSquares(int x, int y) {
        NovaMapData[] squares = new NovaMapData[8];
        squares[0] = get(x - 1, y - 1);
        squares[1] = get(x, y - 1);
        squares[2] = get(x + 1, y - 1);

        squares[3] = get(x - 1, y);
        squares[4] = get(x + 1, y);

        squares[5] = get(x - 1, y + 1);
        squares[6] = get(x, y + 1);
        squares[7] = get(x + 1, y + 1);

        int count = 0;
        for(NovaMapData square : squares) {
            if(square != null) {
                count++;
            }
        }

        NovaMapData[] ret = new NovaMapData[count];
        int index = 0;
        for(int c = 0; c < 8; c++) {
            if(squares[c] != null) {
                ret[index] = squares[c];
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
                    squares = new NovaMapData[] {getNotNull(x - 1, y - 1), getNotNull(x, y - 1), getNotNull(x - 1, y)};
                } else //bottom left
                {
                    squares = new NovaMapData[] {getNotNull(x + 1, y + 1), getNotNull(x - 1, y), getNotNull(x, y + 1)};
                }
            } else if(previousYDelta == -1) //top right
            {
                squares = new NovaMapData[] {getNotNull(x + 1, y - 1), getNotNull(x, y - 1), getNotNull(x + 1, y)};
            } else //bottom right
            {
                squares = new NovaMapData[] {getNotNull(x + 1, y + 1), getNotNull(x + 1, y), getNotNull(x, y + 1)};
            }
        } else if(previousXDelta == -1) // left
        {
            squares = new NovaMapData[] {getNotNull(x - 1, y), getNotNull(x - 1, y - 1), getNotNull(x - 1, y + 1)};
        } else if(previousXDelta == 1) //right
        {
            squares = new NovaMapData[] {getNotNull(x + 1, y), getNotNull(x + 1, y - 1), getNotNull(x + 1, y + 1)};
        } else if(previousYDelta == -1) //top
        {
            squares = new NovaMapData[] {getNotNull(x, y - 1), getNotNull(x - 1, y - 1), getNotNull(x + 1, y - 1)};
        } else if(previousYDelta == 1) //bottom
        {
            squares = new NovaMapData[] {getNotNull(x, y + 1), getNotNull(x - 1, y + 1), getNotNull(x + 1, y + 1)};
        } else // first square
        {
            squares = new NovaMapData[] {getNotNull(x - 1, y - 1), getNotNull(x, y - 1), getNotNull(x + 1, y - 1), getNotNull(x - 1, y), getNotNull(x + 1, y), getNotNull(x - 1, y + 1), getNotNull(x, y + 1), getNotNull(x + 1, y + 1)};
        }

        return squares;
    }
}
