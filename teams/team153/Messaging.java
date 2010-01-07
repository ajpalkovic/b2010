package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class Messaging extends Base {

    public final int KEY1 = 1234567;
    public final int KEY2 = 7654321;
    public ArrayList<Integer> messageInts = new ArrayList<Integer>();
    public ArrayList<String> messageStrings = new ArrayList<String>();
    public ArrayList<MapLocation> messageLocations = new ArrayList<MapLocation>();
    public SensationalSensing sensing;

    public Messaging(NovaPlayer player) {
        super(player);
    }

    //sendMessage (int[] ints, MapLocation[] locations, String[] strings)
    //sets a broadcast message to send at the end of turn.
    public boolean sendMessage() {
        Message message = new Message();
        message.ints = new int[messageInts.size() + 3];
        message.ints[0] = KEY1;
        message.ints[1] = KEY2;
        message.ints[2] = robot.getID();
        boolean good = true;

        for(int i = 3; i < messageInts.size() + 3; i++) {
            message.ints[i] = messageInts.get(i - 3);
        }

        message.locations = new MapLocation[messageLocations.size()];
        for(int i = 0; i < message.locations.length; i++) {
            message.locations[i] = messageLocations.get(i);
        }

        message.strings = new String[messageStrings.size()];
        for(int i = 0; i < message.strings.length; i++) {
            message.strings[i] = messageStrings.get(i);
        }

        try {
            if(controller.hasBroadcastMessage()) {
                controller.clearBroadcast();
            }

            controller.broadcast(message);

            good = true;
        } catch(Exception e) {
            e.printStackTrace();
            pr("----Caught Exception in sendMessage.  message: " + message.toString() + " Exception: " + e.toString());
            good = false;
        }
        return good;
    }

    public void parseMessages() {
        Message[] messages = controller.getAllMessages();
        for(Message message : messages) {
            processMessage(message);
        }
    }

    public void processMessage(Message message) {
        if(message != null) {

            //We got a message!
            /*String out = "";
            for(int c = 0; c < message.ints.length; c++)
            out += " ["+c+"]="+message.ints[c];
            p("Message Received: "+out);*/

            //is it ours?
            if(message.ints == null || message.ints.length < 3 || message.ints[0] != KEY1 || message.ints[1] != KEY2) {
                return;
            }

            int senderID = message.ints[2];

            //Read it!
            int locationIndex = 0;
            int stringIndex = 0;
            int messageID = -1, recipientID = -1;

            for(int i = 3; i < message.ints.length; i += getMessageLength(messageID)) {
                messageID = message.ints[i];
                //System.out.println(messageID);
                recipientID = message.ints[i + 1];
                switch(messageID) {
                    case BroadcastMessage.UNDER_ATTACK:
                        ;//someone is under attack!!!!
                        break;
                    case BroadcastMessage.ENEMY_IN_SIGHT:
                        player.enemyInSight(message.locations[locationIndex + 1], message.ints[i + 2], message.strings[stringIndex]);//theres an enemy!
                        locationIndex += 2;
                        stringIndex++;
                        //this will broadcast the unit's location (locations[0]) and the enemy's location(locations[1])
                        //
                        break;
                    case BroadcastMessage.MAP_INFO:
                        ;//update of mapinfo can be very many locations[]
                        break;
                    case BroadcastMessage.NEW_UNIT:
                        player.newUnit(senderID, message.locations[0], message.strings[0]);//theres a new unit! I should send my map info!
                        break;
                    case BroadcastMessage.LOW_ALLIED_UNITS:
                        int count = message.ints[i + 2];
                        int index = i + 3;
                        if(player.isArchon) {
                            player.lowAlliedUnitMessageCallback();
                            for(int c = 0; c < count; c++) {
                                MapLocation location = message.locations[locationIndex];
                                int level = message.ints[index];
                                int reserve = message.ints[index + 1];
                                int max = message.ints[index + 2];

                                player.lowAlliedUnitMessageCallback(location, level, reserve, max);

                                index += 3;
                                locationIndex++;
                            }
                            i = index + 1;
                        } else {
                            i = i + 3 + (count * 3) + 1;
                        }
                        break;
                    case BroadcastMessage.LOW_ENERGON:
                        //make sure the message is intended for me
                        player.lowEnergonMessageCallback(message.locations[locationIndex], message.locations[locationIndex + 1], message.ints[i + 3], message.ints[i + 2]);
                        break;
                    case BroadcastMessage.MOVE:
                        locationIndex++;

                        player.moveMessageCallback(message.locations[locationIndex - 1]);

                        break;
                    case BroadcastMessage.PONG:
                        for(int j = 2; i < message.ints.length; i++) {
                            switch(message.ints[i]) {
                                case BroadcastMessage.ENEMY_IN_SIGHT:
                                    //this unit has seen an enemy in the past 100 turns
                                    break;
                                case BroadcastMessage.UNDER_ATTACK:
                                    //this unit has been recently attacked
                                    break;
                            }
                        }
                        ;//locations[0] is the location of the unit
                        //locations[i] corresponds to the location for the status
                        break;
                    case BroadcastMessage.FOLLOW_REQUEST:
                        player.followRequestMessageCallback(message.locations[locationIndex], message.ints[i + 2], senderID, recipientID);

                        if(player.isArchon && recipientID == robot.getID() && player.currentGoal != Goal.followingArchon) {
                            locationIndex++;
                        }

                        break;
                    case BroadcastMessage.SUPPORT:

                        break;
                }
            }
        }
    }

    public boolean broadcastMap(MapLocation[] locations) {
        int[] ints = new int[2];
        ints[1] = BroadcastMessage.MAP_INFO;
        ints[0] = robot.getID();
        return addMessage(ints, null, locations);
    }

    /**********************************************
     *  BROADCASTING A MESSAGE CODE
     *  ints[0] and ints[1] are the KEYS in the message
     *  ints[2] is ints[0] in the messages.
     *  ints[2] is senderID
     *  ints[3] is messageCode
     *  ints[4] is recepientID (-1 if its for everyone)
     *  
     **********************************************/
    public int getMessageLength(int messageID) {
        switch(messageID) {
            case BroadcastMessage.ENEMY_IN_SIGHT:
                return 3;
            case BroadcastMessage.LOW_ENERGON:
                return 4;
            case BroadcastMessage.NEW_UNIT:
                return 2;
            case BroadcastMessage.FOLLOW_REQUEST:
                return 3;
            case BroadcastMessage.FIND_BLOCKS:
                return 3;
            case BroadcastMessage.MOVE:
                return 2;
            case BroadcastMessage.LOW_ALLIED_UNITS:
                return 0;
        }
        return -1;
    }

    /**
     * This method sends enemyInSight message broadcast for all enemies in sight
     */
    public void sendMessageForEnemyRobots() {
        ArrayList<RobotInfo> enemies = sensing.senseEnemyRobotInfoInSensorRange();
        for(RobotInfo robot : enemies) {
            int[] data = {(int) robot.energonLevel, -1};
            String robotType = robot.type.toString();

            sendEnemyInSight(robot.location, data, robotType);
        }
        if(enemies.size() > 0) {
            sendMessage();
            player.enemyInSight(enemies);
        }
    }

    public boolean addMessage(int[] ints, String[] strings, MapLocation[] locations) {
        if(!controller.hasBroadcastMessage()) {
            clearMessages();
        }

        int i = 0;
        if(ints != null) {
            for(i = 0; i < ints.length; i++) {
                messageInts.add(ints[i]);
            }
        }
        if(strings != null) {
            for(i = 0; strings != null && i < strings.length; i++) {
                messageStrings.add(strings[i]);
            }
        }
        if(locations != null) {
            for(i = 0; i < locations.length; i++) {
                messageLocations.add(locations[i]);
            }
        }

        sendMessage();
        return true;
    }

    /*
     * send message methods
     */
    public void clearMessages() {
        messageInts.clear();
        messageStrings.clear();
        messageLocations.clear();
    }

    public boolean sendMove(MapLocation location) {
        int[] ints = new int[2];
        ints[0] = BroadcastMessage.MOVE;
        ints[1] = -1;
        MapLocation[] locations = new MapLocation[1];
        locations[0] = location;
        String[] strings = null;
        pr("Sent Move");
        return addMessage(ints, strings, locations);
    }

    public boolean sendLowEnergon(MapLocation archonLocation, int amount) {
        int[] ints = new int[4];
        ints[0] = BroadcastMessage.LOW_ENERGON;
        ints[1] = -1;
        ints[2] = amount;
        ints[3] = player.isAirRobot ? 1 : 0;

        MapLocation[] locations = new MapLocation[2];
        locations[0] = controller.getLocation();
        locations[1] = archonLocation;

        String[] strings = null;

        return addMessage(ints, strings, locations);
    }

    public boolean sendEnemyInSight(MapLocation location, int[] data, String robotType) {
        int[] ints = new int[3];
        ints[0] = BroadcastMessage.ENEMY_IN_SIGHT;
        ints[1] = -1;
        //the type of enemy
        ints[2] = data[0];
        MapLocation[] locations = new MapLocation[2];
        locations[0] = controller.getLocation();
        locations[1] = location;
        String[] strings = {robotType};
        return addMessage(ints, strings, locations);
    }

    public boolean sendNewUnit() {
        int[] ints = new int[2];
        ints[0] = BroadcastMessage.NEW_UNIT;
        ints[1] = -1;
        MapLocation[] locations = new MapLocation[1];
        locations[0] = controller.getLocation();
        String[] strings = {controller.getRobotType().toString()};
        return addMessage(ints, strings, locations);
    }
    //follow request archon only

    public boolean sendFollowRequest(MapLocation archonLocation, int archonNumber, int supportUnit) {
        int[] ints = new int[3];
        ints[0] = BroadcastMessage.FOLLOW_REQUEST;
        ints[1] = supportUnit;
        ints[2] = archonNumber;
        MapLocation[] locations = new MapLocation[1];
        locations[0] = archonLocation;
        String[] strings = null;
        return addMessage(ints, strings, locations);
    }
}
