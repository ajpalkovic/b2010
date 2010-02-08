package team299;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class TowerPlayer extends NovaPlayer {

    public MapLocation[] idealLocations;
    
    public TowerPlayer(RobotController controller) {
        super(controller);
        messaging.sendNewUnit();
    }

    public void towerBuildLocationRequestCallback(int recepientID) {
        if(controller.getFlux() <= 1) return;
        if(idealLocations != null) {
            messaging.sendTowerBuildLocationResponse(idealLocations, recepientID);
        }
    }
    public void towerBuildLocationResponseCallback(MapLocation[] locations){
    	int index = Clock.getRoundNum();
    	for (MapLocation l : locations) {
    		sensing.knownAlliedTowerLocations.put(index, l);
    		sensing.knownAlliedTowerIDs.put(l.getX()+ "," + l.getY(), index);
    		index++;
    	}
    }
    public void newUnit(int senderID, MapLocation location, String robotType) {
    	if (RobotType.valueOf(robotType).isBuilding()){
    		sensing.senseAlliedTowerLocations();
    		if (!sensing.knownAlliedTowerIDs.containsKey(location.getX() +","+location.getY())){
    			sensing.knownAlliedTowerLocations.put(new Integer(senderID), location);
    			sensing.knownAlliedTowerIDs.put(location.getX() + "," + location.getY(), senderID);
    			if (controller.getFlux() > 10){
    				messaging.sendNewUnit();
    				messaging.sendTowerPing(senderID, location);
    				MapLocation[] loc = new MapLocation[sensing.knownAlliedTowerLocations.size()];
    				sensing.knownAlliedTowerLocations.values().toArray(loc);
    				messaging.sendTowerBuildLocationResponse(loc, senderID);
    			}
    		}
    	}
    }
    public void towerPingLocationCallback(MapLocation location, int robotID) {
    	sensing.senseAlliedTowerLocations();
		if (!sensing.knownAlliedTowerIDs.containsKey(location.getX() +","+location.getY())){
			sensing.knownAlliedTowerLocations.put(new Integer(robotID), location);
			sensing.knownAlliedTowerIDs.put(location.getX() + "," + location.getY(), robotID);
			if (controller.getFlux() > 10)
				messaging.sendTowerPing(robotID, location);			
		}
    }
    
    public void updateTowerBuildLocations() {
        ArrayList<MapLocation> locations = sensing.senseKnownAlliedTowerLocations();
        int[] dirValues = {0, 0, 0, 0}; //north south east west
        int distance, nmax=Integer.MIN_VALUE, smax=Integer.MIN_VALUE, emax=Integer.MIN_VALUE, wmax=Integer.MIN_VALUE;
    	Direction tmpDir = Direction.NORTH;
    	int northsoutheastwest = 2;
        MapLocation us = controller.getLocation(), north= us, south=us, east=us, west=us, location=null;        
    
    if (locations.size() != 0){ 		
        for (MapLocation loc : locations) {
    		tmpDir = us.directionTo(loc);
    		if (tmpDir.isDiagonal()) {
        		tmpDir = tmpDir.rotateRight();
        	}
			distance = us.distanceSquaredTo(loc);
        	switch (tmpDir) {
        		case NORTH:
        			dirValues[0]++;
        			if (distance > nmax){        				
        				nmax = distance;
        				north = loc;
        			}
        		break;
        		case SOUTH:
        			dirValues[1]++;
        			if (distance > smax){        				
        				smax = distance;
        				south = loc;
        			}
        		break;
        		case EAST:
        			dirValues[2]++;
        			if (distance > emax){        				
        				emax = distance;
        				east = loc;
        			}
        		break;
        		case WEST:
        			dirValues[3]++;
        			if (distance > wmax){        				
        				wmax = distance;
        				west = loc;
        			}
        		break;
        	}        	
        }
        int max= Clock.getRoundNum()%locations.size(),maxi = 0,sums=0; 
        northsoutheastwest = (dirValues[0] + dirValues[1] > dirValues [2] + dirValues [3])? 4 : 2;
        for (int i = 0; i < 4; i++)
        	if ((sums += (locations.size() - dirValues[i]))>max && (i < northsoutheastwest && i >= northsoutheastwest-2)){
        		maxi = i;
        		max = dirValues[i];
        		break;
        	}
        		switch (maxi){
        		case 0://north
        			location = north;
        			tmpDir = Direction.NORTH;        			
        		break;        		
        		
        		case 1://south
        			location = south;
        			tmpDir = Direction.SOUTH;
        		break;
        		
        		case 2://east
        			location = east;
        			tmpDir = Direction.EAST;
        		break;
        		
        		case 3://west
        			location = west;        		
        			tmpDir = Direction.WEST;
        		break;
        		}
    	}
        boolean top = northsoutheastwest == 2 , bottom = northsoutheastwest == 2, left = northsoutheastwest == 4, right = northsoutheastwest ==4;
        //pa(dirValues[0] + ", " + dirValues[1] + ", " + dirValues[2] + ", " + dirValues[3]);
        
        MapLocation topLoc = (new MapLocation(north.getX(), north.getY()-5));
        MapLocation bottomLoc = (new MapLocation(south.getX(), south.getY()+5));
        MapLocation leftLoc = (new MapLocation(west.getX()-5, west.getY()));
        MapLocation rightLoc = (new MapLocation(east.getX()+5, east.getY()));
        
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
