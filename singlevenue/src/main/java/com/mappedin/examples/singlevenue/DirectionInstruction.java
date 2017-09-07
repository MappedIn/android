package com.mappedin.examples.singlevenue;

import com.mappedin.sdk.Coordinate;
import com.mappedin.sdk.Instruction;

import java.util.List;

public class DirectionInstruction {
    Instruction instruction;
    Coordinate[] coords;

    public DirectionInstruction(Instruction instruction, List<Coordinate> coords){
        this.instruction = instruction;
        this.coords = coords.toArray(new Coordinate[coords.size()]);
    }
}
