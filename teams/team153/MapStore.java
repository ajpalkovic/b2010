package team298;

import java.util.*;
import battlecode.common.*;

public class MapStore {

    public MapData[][] map;
    private int averageHeight, count;

    public MapStore() {
        map = new MapData[60][60];
    }

    public MapData get(int x, int y) {
        return map[x % 60][y % 60];
    }

    public MapData getOrCreate(int x, int y) {
        MapData ret = get(x, y);
        if(ret == null) {
            ret = new MapData(x, y);
            map[x % 60][y % 60] = ret;
        }
        return ret;
    }

    public MapData getOrCreate(MapLocation location) {
        return getOrCreate(location.getX(), location.getY());
    }

    public MapData getNotNull(int x, int y) {
        MapData ret = get(x, y);
        if(ret == null) {
            ret = new MapData(x, y);
        }
        return ret;
    }

    public MapData getNotNull(MapLocation location) {
        return getNotNull(location.getX(), location.getY());
    }

    public MapData get(MapLocation location) {
        return get(location.getX(), location.getY());
    }

    public void set(int x, int y, MapData data) {
        MapData cur = get(x, y);
        if(cur == null) {
            map[x % 60][y % 60] = data;
        } else {
            cur.isFluxDeposit = data.isFluxDeposit || cur.isFluxDeposit;
            if(data.height > 0) {
                cur.height = data.height;
            }
            if(data.blockHeight >= 0) {
                cur.blockHeight = data.blockHeight;
            }
            if(data.terrainHeight >= 0) {
                cur.terrainHeight = data.terrainHeight;
            }
        }
        if(data.height > 0) {
            averageHeight += data.height;
            count++;
        }

    }

    public void set(MapData data) {
        set(data.x, data.y, data);
    }

    public void setAll(MapData[] datas) {
        for(MapData data : datas) {
            set(data);
        }
    }

    public int getAverageHeight() {
        if(count > 0) {
            return averageHeight / count;
        }
        return 1;
    }

    public MapData[] getSurroundingSquares(int x, int y) {
        MapData[] squares = new MapData[8];
        squares[0] = get(x - 1, y - 1);
        squares[1] = get(x, y - 1);
        squares[2] = get(x + 1, y - 1);

        squares[3] = get(x - 1, y);
        squares[4] = get(x + 1, y);

        squares[5] = get(x - 1, y + 1);
        squares[6] = get(x, y + 1);
        squares[7] = get(x + 1, y + 1);

        int count = 0;
        for(MapData square : squares) {
            if(square != null) {
                count++;
            }
        }

        MapData[] ret = new MapData[count];
        int index = 0;
        for(int c = 0; c < 8; c++) {
            if(squares[c] != null) {
                ret[index] = squares[c];
                index++;
            }
        }

        return ret;
    }

    public MapData[] getForwardSquares(int x, int y, int previousXDelta, int previousYDelta, boolean diagonal) {
        MapData[] squares;
        if(diagonal) {
            if(previousXDelta == -1) {
                if(previousYDelta == -1) //top left
                {
                    squares = new MapData[] {getNotNull(x - 1, y - 1), getNotNull(x, y - 1), getNotNull(x - 1, y)};
                } else //bottom left
                {
                    squares = new MapData[] {getNotNull(x + 1, y + 1), getNotNull(x - 1, y), getNotNull(x, y + 1)};
                }
            } else if(previousYDelta == -1) //top right
            {
                squares = new MapData[] {getNotNull(x + 1, y - 1), getNotNull(x, y - 1), getNotNull(x + 1, y)};
            } else //bottom right
            {
                squares = new MapData[] {getNotNull(x + 1, y + 1), getNotNull(x + 1, y), getNotNull(x, y + 1)};
            }
        } else if(previousXDelta == -1) // left
        {
            squares = new MapData[] {getNotNull(x - 1, y), getNotNull(x - 1, y - 1), getNotNull(x - 1, y + 1)};
        } else if(previousXDelta == 1) //right
        {
            squares = new MapData[] {getNotNull(x + 1, y), getNotNull(x + 1, y - 1), getNotNull(x + 1, y + 1)};
        } else if(previousYDelta == -1) //top
        {
            squares = new MapData[] {getNotNull(x, y - 1), getNotNull(x - 1, y - 1), getNotNull(x + 1, y - 1)};
        } else if(previousYDelta == 1) //bottom
        {
            squares = new MapData[] {getNotNull(x, y + 1), getNotNull(x - 1, y + 1), getNotNull(x + 1, y + 1)};
        } else // first square
        {
            squares = new MapData[] {getNotNull(x - 1, y - 1), getNotNull(x, y - 1), getNotNull(x + 1, y - 1), getNotNull(x - 1, y), getNotNull(x + 1, y), getNotNull(x - 1, y + 1), getNotNull(x, y + 1), getNotNull(x + 1, y + 1)};
        }

        return squares;
    }

    public int getHeight(MapData data) {
        if(data.height > 0) {
            return data.height;
        }

        /*MapData[] squares = getSurroundingSquares(data.x, data.y);

        if(squares.length == 0)*/
        return getAverageHeight() + 1;
        /*
        int sum = 0;
        for(int c = 0; c < squares.length; c++)
        sum += squares[c].height;

        return (sum / count) + 1;*/
    }

    public int getHeight(int x, int y) {
        return getHeight(get(x, y));
    }
}
