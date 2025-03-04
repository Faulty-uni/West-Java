package uk.ac.bris.cs.scotlandyard.model;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.awt.*;
import java.util.*;
import java.util.List;
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

		private Player getDetective(Piece piece) {
			for (Player detective : detectives) {
				if (detective.piece().equals(piece)) {return detective;}}
			return null;}

		private boolean isOccupiedByDetective(int location) {
			for (Player detective : detectives) {
				if (detective.location() == location) {return true;}}
			return false;}

		private Set<Move> generateSingleMoves(Player player) {
			Set<Move> moves = new HashSet<>();

			//System.out.println("Generating moves for: " + player.piece() + " at " + player.location());

			for (int destination : setup.graph.adjacentNodes(player.location())) {
				//System.out.println("Checking destination: " + destination);

				if (player.isDetective() && isOccupiedByDetective(destination)) {
					//System.out.println("Destination " + destination + " is occupied by another detective.");
					continue;}

				for (Transport transport : setup.graph.edgeValueOrDefault(player.location(), destination, ImmutableSet.of())) {
					//System.out.println("Found transport: " + transport + " to " + destination);

					if (player.has(transport.requiredTicket())) {
						//System.out.println("Player has ticket: " + transport.requiredTicket());
						moves.add(new SingleMove(player.piece(), player.location(), transport.requiredTicket(), destination));}}}
			return moves;}


		private static Set<SingleMove> makeSingleMoves
				(GameSetup setup, List<Player> detectives, Player player, int source) {
			Set<SingleMove> possibleSingleMoves = new HashSet<>();

			Set<Integer> detectiveLocations = new HashSet<>();
			for (Player detective : detectives) {detectiveLocations.add(detective.location());}

			for (int destination : setup.graph.adjacentNodes(source)) {
				if (detectiveLocations.contains(destination)) continue;

				for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if (player.has(t.requiredTicket())) {possibleSingleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));}}

				if (player.has(Ticket.SECRET)) {possibleSingleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));}}
				return possibleSingleMoves;}

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
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();
			this.moves = ImmutableSet.of();
			this.remaining = ImmutableSet.<Piece>builder()
					.add(mrX.piece())
					.addAll(detectives.stream().map(Player::piece).toList())
					.build();

			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty");
			if (this.detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty");
			if (!this.mrX.isMrX()) throw new IllegalArgumentException("MrX is null");
			if (winner != null && !winner.isEmpty()) throw new IllegalArgumentException("Winner is not empty");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty");

			for (Player p : detectives) {
				if (p.isMrX()) numOfX += 1;
				if (numOfX > 0) throw new IllegalArgumentException("MrX contains more than one piece");}

			for (Player p : detectives) {
				if (p.has(Ticket.SECRET)) throw new IllegalArgumentException("Detectives contains secret tickets");}

			for (Player p : detectives) {
				if (p.has(Ticket.DOUBLE)) throw new IllegalArgumentException("Detectives contains double tickets");}

			for (Player p : detectives) {
				if (!detectiveLocations.add(p.location())) {
					// If add() returns false, it means location is already in the set
					throw new IllegalArgumentException("Detectives contain the same location: " + p.location());}}

			for (Player d : detectives) {
				if (d.isDetective()) {
					DetectiveLocation.put((Detective) d.piece(), d.location());}}
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

		@Override
		@Nonnull
		public ImmutableSet<Piece> getWinner() {
			if (!winner.isEmpty()) return winner;

			// MrX captured, detectives win
			for (Player detective : detectives) {
				if (detective.location() == mrX.location()) {
					winner = ImmutableSet.copyOf(detectives.stream().map(Player::piece).toList());
					return winner;
				}
			}

			// MrX completely cornered
			boolean mrXCornered = true;
			for (int neighbor : setup.graph.adjacentNodes(mrX.location())) {
				if (!isOccupiedByDetective(neighbor)) {
					mrXCornered = false;
					break;
				}
			}
			if (mrXCornered) {
				winner = ImmutableSet.copyOf(detectives.stream().map(Player::piece).toList());
				return winner;
			}

			// Detectives have no moves left
			boolean allDetectivesStuck = true;
			for (Player detective : detectives) {
				if (!generateSingleMoves(detective).isEmpty()) {
					allDetectivesStuck = false;
					break;
				}
			}
			if (allDetectivesStuck) {
				winner = ImmutableSet.of(mrX.piece());
				return winner;
			}

			// all moves are used up, MrX wins
			if (log.size() >= setup.moves.size()) {
				winner = ImmutableSet.of(mrX.piece());
				return winner;
			}

			//  Declare MrX stuck even if it's NOT his turn
			if (generateSingleMoves(mrX).isEmpty()) {
				winner = ImmutableSet.copyOf(detectives.stream().map(Player::piece).toList());
				return winner; // MrX loses  if he has no moves
			}

			return winner;
		}


		@Override @Nonnull
		public ImmutableSet<Move> getAvailableMoves() {
			if (!winner.isEmpty()) {
				return ImmutableSet.of();
			}

			Set<Move> availableMoves = new HashSet<>();

			//System.out.println("Remaining Pieces: " + remaining);

			for (Piece piece : remaining) {
				Player currentPlayer = piece.isMrX() ? mrX : getDetective(piece);
				if (currentPlayer != null) {
					Set<Move> playerMoves = generateSingleMoves(currentPlayer);
					//System.out.println("Generated moves for " + piece + ": " + playerMoves);
					availableMoves.addAll(playerMoves);
				}
			}

			//System.out.println("Final Available Moves (after adding all players): " + availableMoves);
			return ImmutableSet.copyOf(availableMoves);
		}





		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			//System.out.println("DetectiveLocation Map: " + DetectiveLocation);
			//System.out.println("Checking detective: " + detective);
			for (Player p : allPlayers) {
				if (p.isMrX()) {return Optional.empty();}
				if (p.isDetective() && DetectiveLocation.containsKey(detective)) {return Optional.of(DetectiveLocation.get(detective));}}
			return Optional.empty();}

		@Override @Nonnull public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {
			//used lambdas to make our lives easier
			//used getOrDefault instead of mapping becasue mapping can cause NullPointer error
			for (Player p : detectives) {
				if (p.piece().equals(piece)) {return Optional.of(ticket -> p.tickets().getOrDefault(ticket, 0));}}
			if (mrX.piece().equals(piece)) {return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));}
			return Optional.empty(); //  Player not found
		}

		@Override public ImmutableSet<Piece> getPlayers() {
			Set<Piece> allPlayers = new HashSet<>();
			allPlayers.add(mrX.piece());
			for (Player detective : detectives) {allPlayers.add(detective.piece());}
			return ImmutableSet.copyOf(allPlayers);}

		@Override
		public GameState advance(Move move) {
			if (moves == null || moves.isEmpty()) {
				moves = getAvailableMoves(); // Ensure moves are always available}

				//System.out.println("Moves before checking: " + moves);
				//System.out.println("Checking move: " + move);

				if (!moves.contains(move)) {
					throw new IllegalArgumentException("Illegal move: " + move);
				}

				Player movedPlayer = move.commencedBy().isMrX() ? mrX : getDetective(move.commencedBy());

				if (movedPlayer != null) {
					// Deduct the ticket after use
					Map<Ticket, Integer> updatedTickets = new HashMap<>(movedPlayer.tickets());
					updatedTickets.put(((SingleMove) move).ticket, updatedTickets.getOrDefault(((SingleMove) move).ticket, 0) - 1);

					// Create updated Player object with new location
					Player updatedPlayer = new Player(movedPlayer.piece(), ImmutableMap.copyOf(updatedTickets), ((SingleMove) move).destination);

					if (movedPlayer.isMrX()) {
						// MrX  moved, update `remaining` to only include detectives
						ImmutableSet<Piece> updatedRemaining = ImmutableSet.<Piece>builder()
								.addAll(detectives.stream().map(Player::piece).toList()) // Add all detectives- still unsure about stream
								.build();

						GameState newState = new MyGameState(setup, updatedRemaining, log, updatedPlayer, detectives);
						return newState;}
					else {
						//  Update detectives
						List<Player> updatedDetectives = new ArrayList<>(detectives);
						updatedDetectives.remove(movedPlayer);
						updatedDetectives.add(updatedPlayer);

						//  Detectives have moved, so MrX's turn
						ImmutableSet<Piece> updatedRemaining = ImmutableSet.<Piece>builder()
								.add(mrX.piece()) // Add MrX back
								.addAll(updatedDetectives.stream().map(Player::piece).toList())
								.build();

						GameState newState = new MyGameState(setup, updatedRemaining, log, mrX, updatedDetectives);
						ImmutableSet<Move> newMoves = newState.getAvailableMoves();
						//System.out.println("Moves after advancing detective: " + newMoves);
						return newState;}
				}}

				return this;


		}



		@Override public int getCount(@Nonnull Ticket ticket) {return log.size();}}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(mrX.piece()),ImmutableList.of(),mrX,detectives);
	}
}