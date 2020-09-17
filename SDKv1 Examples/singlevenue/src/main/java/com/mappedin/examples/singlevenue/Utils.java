package com.mappedin.examples.singlevenue;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.mappedin.sdk.Coordinate;
import com.mappedin.sdk.Instruction;
import com.mappedin.sdk.MappedInException;

import java.util.List;

public class Utils {
    /**
     * Determine the instruction to show the user, based on their current position
     * Each Instruction has a list of coordinates that belong to it (all the coordinates after the
     * last instruction, to the one the instruction actually happens at). So, we just find the
     * Instruction who's portion of the path we are closest to, and use that.
     * @param currentPosition {@link Coordinate} current position's coordinate
     * @return {@link Instruction} the next instruction
     */
    public static Instruction getNextInstruction(List<DirectionInstruction> directionInstructions, Coordinate currentPosition) {
        float minDistance = Float.MAX_VALUE;
        DirectionInstruction nearest = directionInstructions.get(0);
        int nearestIndex = 0;
        for (int i = 0; i < directionInstructions.size(); i++){
            DirectionInstruction instruction = directionInstructions.get(i);
            float distance;
            try {
                distance = currentPosition.metersFrom(instruction.coords);
            } catch (MappedInException e){
                distance = Float.MAX_VALUE;
            }
            if (distance < minDistance) {
                minDistance = distance;
                nearest = instruction;
                nearestIndex = i;
            }
        }
        if (currentPosition == nearest.instruction.coordinate && nearestIndex+1 < directionInstructions.size()){
            return directionInstructions.get(nearestIndex+1).instruction;
        }
        return nearest.instruction;
    }

    public static Drawable setDirectionImage(Context context, Instruction instruction) {
        if (instruction.action.getClass() == Instruction.Turn.class) {
            Instruction.Turn turn = ((Instruction.Turn) instruction.action);
            switch (turn.relativeBearing) {
                case Left:
                    return context.getResources().getDrawable(R.drawable.turn_left);
                case SlightLeft:
                    return context.getResources().getDrawable(R.drawable.slight_left);
                case Right:
                    return context.getResources().getDrawable(R.drawable.turn_right);
                case SlightRight:
                    return context.getResources().getDrawable(R.drawable.slight_right);
                case Straight:
                    return context.getResources().getDrawable(R.drawable.go_straight);
                default:
                    return null;
            }
        } else if (instruction.action.getClass() == Instruction.Arrival.class
                || instruction.action.getClass() == Instruction.Departure.class) {
            return context.getResources().getDrawable(R.drawable.location);
        } else if (instruction.action.getClass() == Instruction.TakeVortex.class) {
            Instruction.TakeVortex action = (Instruction.TakeVortex)instruction.action;
            if (action.fromMap.getFloor() > action.toMap.getFloor()){
                return context.getResources().getDrawable(R.drawable.elevator_down);
            } else {
                return context.getResources().getDrawable(R.drawable.elevator_up);
            }
        } else {
            return context.getResources().getDrawable(R.drawable.location);
        }
    }
}
