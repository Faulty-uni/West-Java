package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Scotlandyard.*;
import ValueGraph;

import java.util.*;

public class Dijkstra {
    
    private int closestValidNode;
    public static Map<Move, Integer> weightedMoves = new HashMap<>();

    public int getLength(Board board, List<Optional<Integer>> detectiveLocations, int mrXLocation){
        return 0;
    }

    public static Move extract_Max(Map<Move, Integer> weightedMoves){
        return null;
    }

    public static Move findNode (Board board, List<Optional<Integer>> detectiveLocations, int mrXLocation, List<Move> moves){
        for (Move move : moves){
            if(){}
        }

        return extract_Max(weightedMoves);
    }
}

