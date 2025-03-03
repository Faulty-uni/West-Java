package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;

import com.google.common.graph.ImmutableValueGraph;
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

		final private GameSetup setup;
		final private ImmutableSet<Piece> remaining;
		final private ImmutableList<LogEntry> log;
		final private Player mrX;
		final private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		private int numOfX;
		private Map<Detective, Integer> DetectiveLocation;
		private List<Player> allPlayers = new ArrayList<>();
		private Set<Integer> detectiveLocations = new HashSet<>();


		//final means that the data that it initialises is "immutable"
		//like const except is initialised at runtime rather than compile time
		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {

			this.DetectiveLocation = new HashMap<>();
			this.allPlayers.addAll(detectives);
			this.allPlayers.add(mrX);
			this.numOfX = 0;
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();

			for (Player d : detectives) {
				if (d.isDetective()) {
					DetectiveLocation.put((Detective) d.piece(), d.location());
				}
			}

			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty");
			if (this.detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty");
			if (!this.mrX.isMrX()) throw new IllegalArgumentException("MrX is null");

			//if (setup.moves.size() != this.detectives.size())
			//	throw new IllegalArgumentException("Detectives size mismatch");
			//if (!setup.graph.isDirected()) throw new IllegalArgumentException("Graph is not directed");

			for (Player p : detectives) {
				if (p.isMrX()) numOfX += 1;
				if (numOfX > 0) throw new IllegalArgumentException("MrX contains more than one piece");
			}
			for (Player p : detectives) {
				if (p.has(Ticket.SECRET)) throw new IllegalArgumentException("Detectives contains secret tickets");
			}
			for (Player p : detectives) {
				if (p.has(Ticket.DOUBLE)) throw new IllegalArgumentException("Detectives contains double tickets");
			}

			for (Player p : detectives) {
				if (!detectiveLocations.add(p.location())) {
					// If add() returns false, it means location is already in the set
					throw new IllegalArgumentException("Detectives contain the same location: " + p.location());
				}
			}
			// maybe only passes test if getWinner is actually implemented?
			if (winner != null && !winner.isEmpty()) throw new IllegalArgumentException("Winner is not empty");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty");


		}


		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			return winner;
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return null;
		}

		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			//System.out.println("DetectiveLocation Map: " + DetectiveLocation);
			//System.out.println("Checking detective: " + detective);
			for (Player p : allPlayers) {
				if (p.isMrX()) {
					return Optional.empty();
				}
				if (p.isDetective() && DetectiveLocation.containsKey(detective)) {
					return Optional.of(DetectiveLocation.get(detective));
				}
			}
			return Optional.empty();

		}

		@Override
		@Nonnull
		public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {
			for (Player p : detectives) {
				if (p.piece().equals(piece)) {
					return Optional.of(ticket -> p.tickets().getOrDefault(ticket, 0));
				}
			}
			if (mrX.piece().equals(piece)) {
				return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
			}

			return Optional.empty(); //  Player not found
		}

		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> allPlayers = new HashSet<>();
			allPlayers.add(mrX.piece());
			for (Player detective : detectives) {
				allPlayers.add(detective.piece());
			}
			return ImmutableSet.copyOf(allPlayers);
		}

		@Override
		public GameState advance(Move move) {
			return null;
		}

		@Override
		public int getCount(@Nonnull Ticket ticket) {
			return log.size();
		}
	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(mrX.piece()),ImmutableList.of(),mrX,detectives);
	}
}