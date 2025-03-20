package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Scotlandyard.*;
import java.util.*;

public class BasicAI implements Ai {


	//MRX
	GameSetup gameSetup;
	Player mrX;
	List<Piece.Detective> detectiveDetectives;
	List<Player> DetectivePlayers;
	ImmutableSet<Piece.Detective> setDetectives;
	ImmutableSet<Player> setPlayerDetectives;
	Board.GameState board;

    public BasicAI() {
		gameSetup = ;
		for (Piece piece : ScotlandYard.DETECTIVES) {
			detectiveDetectives.add((Piece.Detective) piece);
			DetectivePlayers.add((Player) piece);
		}
//		ImmutableSet<Player> Detectives = (Player) ImmutableSet.copyOf(detectives);
        board = new MyGameStateFactory().build(gameSetup, mrX, setPlayerDetectives);
    }

    @Nonnull @Override
	public String name() { return "BasicAI"; }

	@Nonnull @Override
	public Move pickMove( @Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
		var moves = board.getAvailableMoves().asList();
		var size = moves.size();
		Set<Optional<Integer>> detectiveLocations /*= board.getDetectiveLocation()*/;
		for (Player player : Detectives) {
			detectiveLocations.add(board.getDetectiveLocation(player));
		}
        //		return moves.get(new Random().nextInt(moves.size()));
		
		return Dijkstra.findNode(board, detectiveLocations, board.mrX.location);
	}

}

