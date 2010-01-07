package team153;

import battlecode.common.*;
import battlecode.common.TerrainTile.TerrainType;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class ArchonPlayer extends NovaPlayer {

    public int[] verticalDeltas = new int[] {-6, 0, -5, 3, -4, 4, -3, 5, -2, 5, -1, 5, 0, 6, 1, 5, 2, 5, 3, 5, 4, 4, 5, 3, 6, 0};
    public int[] horizontalDeltas = new int[] {0, -6, 3, -5, 4, -4, 5, -3, 5, -2, 5, -1, 6, 0, 5, 1, 5, 2, 5, 3, 4, 4, 3, 5, 0, 6};
    public int[] diagonalDeltas = new int[] {5, -3, 5, -2, 5, 0, 6, 0, 5, 1, 5, 2, 5, 3, 4, 3, 4, 4, 3, 4, 3, 5, 2, 5, 1, 5, 0, 5, 0, 6};
    public int archonNumber = 0;
    public int archonGroup = -1;
    public Party scoutParty = new Party(3);
    public Direction exploreDirection;
    public SporadicSpawning spawning;

    public ArchonPlayer(RobotController controller) {
        super(controller);
        spawning = new SporadicSpawning(this);
    }

    public void step() {
        // reevaluate goal here?
        sensing.senseAllTiles();
        switch(currentGoal) {
            case Goal.collectingFlux:
                if(spawning.canSupportUnit(RobotType.WOUT)) {
                    spawning.spawnRobot(RobotType.WOUT);
                }
                break;
            case Goal.followingArchon:
                messaging.sendMessageForEnemyRobots();
                for(Robot r : controller.senseNearbyAirRobots()) {
                    if(r.getID() == followingArchonNumber) {
                        try {
                            NovaMapData loc = map.getNotNull(controller.senseRobotInfo(r).location);
                            navigation.goByBugging(loc);


                        } catch(Exception e) {
                            pr("----------------cannot sense robot info in following archon");
                        }
                    }
                }

                break;
            case Goal.idle:
                // reevaluate the goal
                break;
            case Goal.scouting:
                //
                messaging.sendMessageForEnemyRobots();
                trackingCount++;
                trackingCount %= 10;
                scoutParty.partyUp();
                if(trackingCount == 0) {
                    //explore();
                }
                scout();

                break;
            case Goal.fight:
                spawnParty();
                break;
        }
    }

    public void scout() {
        navigation.moveOnce(exploreDirection);
    }

    public void boot() {
        team = controller.getTeam();
        senseArchonNumber();
        if(archonNumber < 5) {
            setGoal(Goal.collectingFlux);
            archonGroup = 1;
        }
        if(archonNumber % 2 == 0) {
            //message to other archon
        }

    }

    public void enemyInSight() {
        //spawnParty();
    }

    public void spawnParty() {
        int turnsToWait = new Random().nextInt(8) + Clock.getRoundNum() + 1;
        while(Clock.getRoundNum() < turnsToWait) {
            messaging.parseMessages();
            energon.processEnergonTransferRequests();
            controller.yield();
        }

        if(spawning.canSupportUnit(RobotType.TURRET) && scoutParty.partySize < 2) {
            scoutParty.waitForNewUnit(RobotType.TURRET);
            spawning.spawnRobot(RobotType.TURRET);
        } else if(controller.getEnergonLevel() / controller.getMaxEnergonLevel() > .5) {
            ArrayList<MapLocation> enemies = sensing.senseEnemyRobotLocations();
            if(enemies.size() <= 3) {
                goDirection(controller.getLocation().directionTo(enemies.get(0)));
            }
            /*if(enemies.size() == 0) {
                setGoal(Goal.scouting);
                explore();
            }*/
        } else {
            ArrayList<MapLocation> enemies = sensing.senseEnemyRobotLocations();
            if(enemies.size() > 0) {
                goDirection(controller.getLocation().directionTo(enemies.get(0)).opposite());
            }

        }
    }

    public void newUnit(int robotID, MapLocation location, String robotType) {

        if(scoutParty.isWaitingForNewRobot() && scoutParty.expectedRobotType.toString().equals(robotType)) {
            ArrayList<Robot> nearbyRobots = new ArrayList<Robot>();
            nearbyRobots.addAll(Arrays.asList(controller.senseNearbyAirRobots()));
            nearbyRobots.addAll(Arrays.asList(controller.senseNearbyGroundRobots()));
            for(Robot r : nearbyRobots) {
                if(r.getID() == robotID) {
                    //this guy is near us, and he is probably the one we were waiting for in our party
                    try {
                        scoutParty.addPartyMember(r, controller.senseRobotInfo(r));
                    } catch(Exception e) {
                        pr("------------Cannot Sense Robot Info in newUnit in ArchonPlayer");
                    }
                }
            }
        }

    }

    /**
     * so archon teams will stay together
     */
    public void followRequest(int archonNumber, int id) {
        if(archonNumber % 2 == 1 && this.archonNumber == archonNumber + 1) {
            setGoal(Goal.followingArchon);
            followingArchonNumber = id;
        }
    }

    public void goDirection(Direction d) {
        int[] delta = navigation.getDirectionDelta(d);
        int x = controller.getLocation().getX() + delta[0] * 10;
        int y = controller.getLocation().getY() + delta[1] * 10;
        MapLocation destination = new MapLocation(x, y);
        navigation.go(new NovaMapData(destination));
    }

    /**
     * Calculates the order in which the archons were spawned.
     */
    public void senseArchonNumber() {
        Message[] messages = controller.getAllMessages();
        int min = 1;
        for(Message m : messages) {
            if(m.ints[0] >= min) {
                min = m.ints[0] + 1;
            }
        }

        archonNumber = min;

        Message m = new Message();
        m.ints = new int[] {min};
        try {
            controller.broadcast(m);
        } catch(Exception e) {
            System.out.println("----Caught Exception in senseArchonNumber.  Exception: " + e.toString());
        }
        System.out.println("Number: " + min);
    }

    public void senseNewTiles() {
        sensing.senseDeltas(verticalDeltas, horizontalDeltas, diagonalDeltas);
    }

    private class Party {

        ArrayList<Robot> partyRobots;
        ArrayList<RobotInfo> partyRobotInfo;
        int partyGoal = -1;
        int partyMaxSize = 3;
        int partySize;
        boolean waitingForNewRobot;
        RobotType expectedRobotType;

        public Party(int size) {
            partyMaxSize = size;
            init();
        }

        public Party() {
            init();
        }

        public void init() {
            partySize = 0;
            partyRobots = new ArrayList<Robot>();
            partyRobotInfo = new ArrayList<RobotInfo>();
            waitingForNewRobot = false;
        }

        public int getPartySize() {
            return partySize;
        }

        public void partyUp() {
            MapLocation ret = null;
            int turn = 0;
            ArrayList<Robot> missingParty = (ArrayList<Robot>) partyRobots.clone();
            ArrayList<Robot> nearbyRobots = new ArrayList<Robot>();
            nearbyRobots.addAll(Arrays.asList(controller.senseNearbyAirRobots()));
            nearbyRobots.addAll(Arrays.asList(controller.senseNearbyGroundRobots()));
            while(turn < 15 && missingParty.size() > 0) {
                for(Robot r : partyRobots) {
                    for(Robot ro : nearbyRobots) {
                        if(r.getID() == ro.getID()) {
                            missingParty.remove(r);
                        }
                    }
                }
                turn++;
                controller.yield();
            }
        }

        public void setPartyGoal(int partyGoal) {
            if(this.partyGoal == partyGoal) {
                return;
            }
            this.partyGoal = partyGoal;
            switch(partyGoal) {
                case Goal.followingArchon:
                    System.out.println("Group is Following Archon");
                    for(Robot robot : partyRobots) {
                        messaging.sendFollowRequest(controller.getLocation(), archonNumber, robot.getID());
                    }
                    break;
                default:
                    //TODO: this used to be Goal.supportingFluxDeposit

                    messaging.sendFollowRequest(controller.getLocation(), -1, -1);
                    break;
            }
        }

        public void addPartyMember(Robot robot, RobotInfo robotInfo) {
            if(partySize < partyMaxSize) {
                partyRobots.add(robot);
                partyRobotInfo.add(robotInfo);
                partySize++;
            }
            if(waitingForNewRobot && expectedRobotType == robotInfo.type) {
                waitingForNewRobot = false;
            }
        }

        public boolean isWaitingForNewRobot() {
            return waitingForNewRobot;
        }

        public void waitForNewUnit(RobotType robotType) {
            expectedRobotType = robotType;
            waitingForNewRobot = true;
        }
    }
}
