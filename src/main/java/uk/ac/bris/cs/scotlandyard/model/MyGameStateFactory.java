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

	private final class MyGameState implements GameState, Board.TicketBoard {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
//		private int numOfX;

		//final means that the data that it initialises is "immutable"
		//like const except is initialised at runtime rather than compile time
		private MyGameState(
			final GameSetup setup,
			final ImmutableSet<Piece> remaining,
			final ImmutableList<LogEntry> log,
			final Player mrX,
			final List<Player> detectives){

//			this.numOfX = 0;
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty");
			if (this.detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty");
			if (!this.mrX.isMrX()) throw new IllegalArgumentException("MrX is null");

			
			/*			for (Player detective : detectives) {
				if (getLocation(detective) != ) throw new IllegalArgumentException("2 Detectives in the same place");
			}
			for (Piece p : this.remaining) {
				System.out.println(numOfX);
				if (p.isDetective()) numOfX+=1;
				if (numOfX > 1) throw new IllegalArgumentException("MrX contains more than one piece");
			}*/

		}
		@Override public GameSetup getSetup() { return setup;}
		@Override public ImmutableList<LogEntry> getMrXTravelLog() { return log; }

		@Nonnull @Override public ImmutableSet<Piece> getWinner() {

			return null;
		}

		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			return null;
		}

		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			//loop over detectives, if the piece is detective return location in Optional.of()

			for (Player d : detectives) {
				if (d.piece() == detective) return Optional.of(0);
			}
			return Optional.empty();
		}
		@Nonnull public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {
			if (piece == detectives) return Optional.empty();
			return Optional.of(this::getCount);
		}
		@Override public ImmutableSet<Piece> getPlayers() {return null;}
		@Override public GameState advance(Move move) {return null;}

		@Override
		public int getCount(@Nonnull Ticket ticket) {
			return log.size();
		}
	}
	@Nonnull @Override public GameState build(
			GameSetup setup, Player mrX, ImmutableList<Player> detectives) {

		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}
}
