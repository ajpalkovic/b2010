package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class TowerPlayer extends NovaPlayer {

    public MapLocation[] idealLocations;
    
    public TowerPlayer(RobotController controller) {
        super(controller);
    }

    public void towerBuildLocationRequestCallback() {
        if(controller.getFlux() <= 1) return;
        if(idealLocations != null) {
            messaging.sendTowerBuildLocationResponse(idealLocations);
        }
    }

    public void updateTowerBuildLocations() {
        ArrayList<MapLocation> locations = sensing.senseAlliedTowerLocations();
        boolean top = true, bottom = true, left = true, right = true;
        MapLocation us = controller.getLocation();

        MapLocation topLoc = (new MapLocation(us.getX(), us.getY()-5));
        MapLocation bottomLoc = (new MapLocation(us.getX(), us.getY()+5));
        MapLocation leftLoc = (new MapLocation(us.getX()-5, us.getY()));
        MapLocation rightLoc = (new MapLocation(us.getX()+5, us.getY()));
        
        for(MapLocation location : locations) {
            top = top && location.equals(topLoc);
            bottom = bottom && location.equals(bottomLoc);
            left = left && location.equals(leftLoc);
            right = right && location.equals(rightLoc);
        }

        int count = 0;
        if(top) count++;
        if(bottom) count++;
        if(left) count++;
        if(right) count++;

        idealLocations = new MapLocation[count];
        int index = -1;
        if(top) idealLocations[++index] = topLoc;
        if(bottom) idealLocations[++index] = bottomLoc;
        if(left) idealLocations[++index] = leftLoc;
        if(right) idealLocations[++index] = rightLoc;
    }

    public void moveMessageCallback(MapLocation location) {
        
    }
}
