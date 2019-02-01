package mappedin.com.wayfindingsample;

import com.mappedin.sdk.Coordinate;
import com.mappedin.sdk.Instruction;

import java.util.List;

/**
 * Created by christinemaiolo on 2018-03-15.
 */

public class DirectionInstruction {
    Instruction instruction;
    Coordinate[] coords;

    public DirectionInstruction(Instruction instruction, List<Coordinate> coords){
        this.instruction = instruction;
        this.coords = coords.toArray(new Coordinate[coords.size()]);
    }
}