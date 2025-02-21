package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		//final means that the data that it initialises is "immutable"
		//like const except is initialised at runtime rather than compile time
		private MyGameState(
			final GameSetup setup,
			final ImmutableSet<Piece> remaining,
			final ImmutableList<LogEntry> log,
			final Player mrX,
			final List<Player> detectives){

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty");
		}
		@Override public GameSetup getSetup() { return setup;}
		@Override public ImmutableList<LogEntry> getMrXTravelLog() { return log; }
		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			//loop over detectives, if the piece is detective return location in Optional.of()

			for (Player d : detectives) {
				if (d.piece() == detective) return Optional.of(0);
			}
			return Optional.empty();
		}
		@Nonnull Optional<Board.TicketBoard> getPlayerTicket(Piece piece) {
			if (piece == detectives) return Optional.of(1)
			return Optional.empty();
		}
		@Override public ImmutableSet<Piece> getPlayers() {return null;}
		@Override public GameState advance(Move move) {return null;}
	}
	@Nonnull @Override public GameState build(
			GameSetup setup, Player mrX, ImmutableList<Player> detectives) {

		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}
}
