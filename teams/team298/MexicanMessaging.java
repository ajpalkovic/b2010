package team298;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class MexicanMessaging extends Base {

    public int KEY1 = 439874345;
    public int KEY2 = 198465730;
    public ArrayList<Integer> messageInts = new ArrayList<Integer>();
    public ArrayList<String> messageStrings = new ArrayList<String>();
    public ArrayList<MapLocation> messageLocations = new ArrayList<MapLocation>();
    public SensationalSensing sensing;
    public Message previousMessage = null;
    
    public MexicanMessaging(NovaPlayer player) {
        super(player);

        //swap the keys so if we play ourself we dont pick up each other's messages
        if(player.team == Team.A) {
            int tmp = KEY1;
            KEY1 = KEY2;
            KEY2 = tmp;
        }
    }
    public boolean sendTowerPing(int robotID, MapLocation location) {
        //p("Send Tower Ping");
    	return addMessage(new int[]{KEY1, KEY2, robot.getID(), BroadcastMessage.towerPing, BroadcastMessage.everyone, robotID}, null, new MapLocation[]{location});
    }
    /**
     * Each of these methods represent the public interface for sending messages.
     * All they have to do is call addMessage and pass it some data.
     * The first two parameters are the message type (from BroadcastMessage.*) and the recipient id (BroadcastMessage.everyone for everyone)
     * Add message accepts an array of ints, strings, and MapLocations which will be sent along with the message.
     */
    public boolean sendTowerBuildLocationRequest(int recipientRobotID) {
        //p("Send Tower Build Location Request");
        return addMessage(new int[] {KEY1, KEY2, robot.getID(), BroadcastMessage.towerBuildLocationRequest, recipientRobotID}, null, null);
    }

    public boolean sendTowerBuildLocationResponse(MapLocation[] locations,int recepientID) {
        //p("Send Tower Build Location Response");
        return addMessage(new int[] {KEY1, KEY2, robot.getID(), BroadcastMessage.towerBuildLocationResponse, recepientID, locations.length}, null, locations);
    }

    public boolean sendMove(MapLocation location) {
        //p("Send Move");
        return addMessage(new int[] {KEY1, KEY2, robot.getID(), BroadcastMessage.move, BroadcastMessage.everyone}, null, new MapLocation[] {location});
    }
    public boolean sendLowEnergon(int amount) {
        //p("Send Low Energon"+amount);
        int round = Clock.getRoundNum() + controller.getRoundsUntilMovementIdle();
        int[] ints = new int[] {KEY1, KEY2, robot.getID(), BroadcastMessage.lowEnergon, BroadcastMessage.everyone, amount, player.isAirRobot ? 1 : 0, round};
        MapLocation[] locations = new MapLocation[] {controller.getLocation()};
        return addMessage(ints, null, locations);
    }

    public boolean sendNewUnit() {
        //p("Send New Unit");
        MapLocation[] locations = new MapLocation[] {controller.getLocation()};
        String[] strings = {controller.getRobotType().toString()};
        return addMessage(new int[] {KEY1, KEY2, robot.getID(), BroadcastMessage.newUnit, BroadcastMessage.everyone}, strings, locations);
    }

    public boolean sendFollowRequest(MapLocation archonLocation, int recipientRobotId) {
        //p("Send Follow Request");
        MapLocation[] locations = new MapLocation[] {archonLocation};
        return addMessage(new int[] {KEY1, KEY2, robot.getID(), BroadcastMessage.followRequest, recipientRobotId}, null, locations);
    }

    public boolean sendClosestEnemyInSight() {
        RobotInfo enemy = player.navigation.findClosest(sensing.senseEnemyRobotInfoInSensorRange());
        if(enemy == null) return true;
        return addMessage(new int[] {KEY1, KEY2, robot.getID(), BroadcastMessage.closestEnemyInSight, BroadcastMessage.everyone, (int)enemy.energonLevel}, new String[] {enemy.type.toString()}, new MapLocation[] {enemy.location});
    }


    /**
     * This method sends enemyInSight message broadcast for all enemies in sight
     */
    public void sendMessageForEnemyRobots() {
        //p("Send Message For Enemy Robots");
        try {
            if(controller.hasBroadcastMessage()) {
                controller.yield();
            }

            ArrayList<RobotInfo> enemies = sensing.senseEnemyRobotInfoInSensorRange();
        
            if(enemies.size() > 0) {
                MapLocation[] locations = new MapLocation[enemies.size()];
                int[] ints = new int[enemies.size()+6];
                String[] strings = new String[enemies.size()];

                RobotInfo robot = null;

                ints[0] = KEY1;
                ints[1] = KEY2;
                ints[2] = player.robot.getID();
                ints[3] = BroadcastMessage.enemyInSight;
                ints[4] = BroadcastMessage.everyone;
                ints[5] = locations.length;
                for(int c = 0; c < locations.length; c++) {
                    robot = enemies.get(c);
                    locations[c] = robot.location;
                    ints[c+6] = (int)robot.energonLevel;
                    strings[c] = robot.type.toString();
                }

                Message message = new Message();
                message.ints = ints;
                message.locations = locations;
                message.strings = strings;
                previousMessage = message;
                controller.broadcast(message);
                player.enemyInSight(enemies);
            }
        } catch (Exception e) {
            pa("----Caught exception in sendMessageForEnemyRobots "+e.toString());
        }
    }


    /**
     * This method processes all of the messages that the robot has.
     */
    public void parseMessages() {
        Message[] messages = controller.getAllMessages();
        for(Message message : messages) {
            processMessage(message);
        }
    }


    /**
     * This is the core method for processing a message.
     * Every message must update this method with two switch cases.
     *
     * A message consists of a series of ints, strings, and locations.
     * The first three ints of a message will always be our two keys and the sender's id.
     * Those are the same from all messages sent from a robot.
     *
     * Inside of a Message object, we can actually send multiple "messages", by just adding a second message
     * to the end of the ints/strings/locations.
     * So, the role of this method is to process each message within the Message object.
     *
     * Any message can have two intended recipients: everyone, or one specific robot.
     *
     * Regardless of for whom the message is intended, this method must update the index in the array of the ints, strings, and locations.
     * Even if the message is not intended for us, the data will still be there, so the indexes need to be updated.
     * Updating the indexes ensures that for the next message, the indexes will point to the start of its data.
     * Essentially, the amount each index needs to be updated is really just the same as the size of each of the arrays in the send method for that message.
     *
     * Finally, if the message is intended for us, it should call a callback function in NovaPlayer to process the message.
     * Putting it there allows us to override it later.
     * All this method should do is copy the data from the arrays into the callback function parameters.
     *
     **********************************************/
    private void processMessage(Message message) {
        if(message != null) {
            //is it ours?
            if(message.ints == null || message.ints.length < 3 || message.ints[0] != KEY1 || message.ints[1] != KEY2) {
            	return;
            }

            int senderID = message.ints[2];
            int locationIndex = 0;
            int stringIndex = 0;
            int messageId = -1;
            int recipientId = -1;
            int myId = controller.getRobot().getID();

            for(int intIndex = 3; intIndex < message.ints.length; ) {
                messageId = message.ints[intIndex];
                recipientId = message.ints[intIndex + 1];

                intIndex += 2;

                //If this was not my message, we still need to update the location and string arrays because they will contain the data from a message that is not mine.
                if(recipientId != BroadcastMessage.everyone && recipientId != myId) {
                    switch(messageId) {
                        case BroadcastMessage.enemyInSight:
                            locationIndex += message.ints[intIndex];
                            stringIndex += message.ints[intIndex];
                            intIndex += message.ints[intIndex]+1;
                            break;
                        case BroadcastMessage.closestEnemyInSight:
                            intIndex++;
                            stringIndex++;
                            locationIndex++;
                            break;
                        case BroadcastMessage.newUnit:
                            break;
                        case BroadcastMessage.lowEnergon:
                            intIndex += 3;
                            locationIndex += 1;
                            break;
                        case BroadcastMessage.move:
                            locationIndex++;
                            break;
                        case BroadcastMessage.followRequest:
                            locationIndex++;
                            break;
                        case BroadcastMessage.support:
                            break;
                        case BroadcastMessage.towerBuildLocationRequest:
                            break;
                        case BroadcastMessage.towerBuildLocationResponse:
                            locationIndex += message.ints[intIndex];
                            intIndex++;
                            break;
                    }
                } else {
                    // this message is ours
                    int count;
                    switch(messageId) {
                        case BroadcastMessage.enemyInSight:
                            count = message.ints[intIndex];
                            player.enemyInSight(message.locations, message.ints, message.strings, locationIndex, intIndex+1, stringIndex, count);
                            intIndex += count+1;
                            locationIndex += count;
                            stringIndex += count;
                            break;
                        case BroadcastMessage.closestEnemyInSight:
                            player.enemyInSight(message.locations[locationIndex], message.ints[intIndex], message.strings[stringIndex]);
                            intIndex++;
                            stringIndex++;
                            locationIndex++;
                            break;
                        case BroadcastMessage.newUnit:
                            player.newUnit(senderID, message.locations[locationIndex], message.strings[stringIndex]);
                            break;
                        case BroadcastMessage.lowEnergon:
                            player.lowEnergonMessageCallback(message.locations[locationIndex], message.ints[intIndex], message.ints[intIndex + 1], message.ints[intIndex+2]);
                            intIndex += 3;
                            locationIndex += 1;
                            break;
                        case BroadcastMessage.move:
                            player.moveMessageCallback(message.locations[locationIndex]);
                            locationIndex++;
                            break;
                        case BroadcastMessage.followRequest:
                            player.followRequestMessageCallback(message.locations[locationIndex], senderID, recipientId);
                            locationIndex++;
                            break;
                        case BroadcastMessage.support:
                            break;
                        case BroadcastMessage.towerBuildLocationRequest:
                            player.towerBuildLocationRequestCallback(senderID);
                            break;
                        case BroadcastMessage.towerBuildLocationResponse:
                            count = message.ints[intIndex];
                            MapLocation[] locations = new MapLocation[count];
                            for(int c = 0; c < count; c++)
                                locations[c] = message.locations[c];
                            player.towerBuildLocationResponseCallback(locations);
                            locationIndex += count;
                            intIndex++;
                            break;
                        case BroadcastMessage.towerPing:
                        	player.towerPingLocationCallback(message.locations[locationIndex], message.ints[intIndex]);
                        	intIndex++;
                        	locationIndex++;
                        	break;
                    }
                }
            }
        }
    }

    /**
     * Copies the data into the arraylists.
     * If no message exists, then any previous message must have been sent, so all the data id old and can be removed.
     */
    private boolean addMessage(int[] ints, String[] strings, MapLocation[] locations) {
        if(!controller.hasBroadcastMessage() || previousMessage == null) {
            previousMessage = new Message();
            previousMessage.ints = ints;
            previousMessage.strings = strings;
            previousMessage.locations = locations;
            try {
                controller.broadcast(previousMessage);
            } catch (Exception e) {
                pa("----Caught exception while sending message"+e);
            }
            return true;
        }

        controller.clearBroadcast();

        Message newMessage = new Message();
        int[] newInts;
        if (previousMessage.ints == null)
        	previousMessage.ints = new int[0];
        
        newInts = new int[ints.length-3+previousMessage.ints.length];
        
        newInts[0] = KEY1;
        newInts[1] = KEY2;
        newInts[2] = robot.getID();
        for(int c = 3; c < previousMessage.ints.length; c++) {
            newInts[c] = previousMessage.ints[c];
        }
        int len = previousMessage.ints.length-3;
        for(int c = 3; c < ints.length; c++) {
            newInts[c+len] = ints[c];
        }
        newMessage.ints = newInts;
        
        if(strings == null) {
            newMessage.strings = previousMessage.strings;
        } else if(previousMessage.strings == null) {
            newMessage.strings = strings;
        } else {
            String[] newStrings = new String[previousMessage.strings.length + strings.length];
            for(int c = 0; c < previousMessage.strings.length; c++) {
                newStrings[c] = previousMessage.strings[c];
            }
            for(int c = 0; c < strings.length; c++) {
                newStrings[c+previousMessage.strings.length] = strings[c];
            }
            newMessage.strings = newStrings;
        }

        if(locations == null) {
            newMessage.locations = previousMessage.locations;
        } else if(previousMessage.locations == null) {
            newMessage.locations = locations;
        } else {
            MapLocation[] newLocations = new MapLocation[previousMessage.locations.length+locations.length];
            for(int c = 0; c < previousMessage.locations.length; c++) {
                newLocations[c] = previousMessage.locations[c];
            }
            for(int c = 0; c < locations.length; c++) {
                newLocations[c+previousMessage.locations.length]  = locations[c];
            }
            newMessage.locations = newLocations;
        }

        previousMessage = newMessage;
        try {
            controller.broadcast(newMessage);
        } catch (Exception e) {
            pa("----Caught exception while sending message"+e);
        }

        return true;
    }
}
