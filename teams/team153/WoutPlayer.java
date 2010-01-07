package team153;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.util.*;

public class WoutPlayer extends NovaPlayer {

    public WoutPlayer(RobotController controller) {
        super(controller);
    }
    
    public void step() {
        if(energon.isEnergonLow()) {
            while(!energon.isEnergonFull()) {
                energon.requestEnergonTransfer();
                controller.yield();
            }
        }

        NovaMapData square = findSquareWithFlux();

        pr("begin sense");
        sensing.senseAllTiles();
        pr("end sense");

        for(NovaMapData s : sensedSurroundingSquares) {
            pr(s.toStringFull());
        }
    }

    public boolean tileSensedCallback(NovaMapData data) {
        pr(data.toStringFull());
        return true;
    }

    public NovaMapData findSquareWithFlux() {
        p(""+sensedSurroundingSquares[8].flux);
        return null;
    }
}
