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

public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState, Board.TicketBoard {

		final private GameSetup setup;
		final private ImmutableSet<Piece> remaining;
		final private ImmutableList<LogEntry> log;
		final private Player mrX;
		final private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		private Map<Detective, Integer> DetectiveLocation;
		private List<Player> allPlayers = new ArrayList<>();
		private Set<Integer> detectiveLocations = new HashSet<>();

		private Player getDetective(Piece piece) {
			for (Player detective : detectives) {
				if (detective.piece().equals(piece)) return detective;
			}
			return null;
		}

		private boolean isOccupiedByDetective(int location) {
			for (Player detective : detectives) {
				if (detective.location() == location) return true;
			}
			return false;
		}

		private Set<Move> generateSingleMoves(Player player) {
			Set<Move> moves = new HashSet<>();
			for (int destination : setup.graph.adjacentNodes(player.location())) {
				if (player.isDetective() && isOccupiedByDetective(destination))
					continue;
				for (Transport transport : setup.graph.edgeValueOrDefault(player.location(), destination, ImmutableSet.of())) {
					if (player.has(transport.requiredTicket())) {
						moves.add(new SingleMove(player.piece(), player.location(), transport.requiredTicket(), destination));
					}
				}
				if (player.has(Ticket.SECRET)) {
					moves.add(new SingleMove(player.piece(), player.location(), Ticket.SECRET, destination));
				}
			}
			return moves;
		}

		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			Set<SingleMove> possibleSingleMoves = new HashSet<>();
			Set<Integer> detectiveLocations = new HashSet<>();
			for (Player detective : detectives) {detectiveLocations.add(detective.location());}
			for (int destination : setup.graph.adjacentNodes(source)) {
				if (detectiveLocations.contains(destination)) continue;
				for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if (player.has(t.requiredTicket())) possibleSingleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
				}
				if (player.has(Ticket.SECRET)) possibleSingleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
			}
			return possibleSingleMoves;
		}

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {

			this.DetectiveLocation = new HashMap<>();
			this.allPlayers.addAll(detectives);
			this.allPlayers.add(mrX);
			this.setup = setup;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();
			this.moves = ImmutableSet.of();
			this.remaining = remaining; //pieces that are yet to move

			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty");
			if (this.detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty");
			if (!this.mrX.isMrX()) throw new IllegalArgumentException("MrX is null");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty");

			Set<Integer> locations = new HashSet<>();
			for (Player p : detectives) {
				if (p.isMrX()) throw new IllegalArgumentException("Detective cannot be MrX");
				if (p.has(Ticket.SECRET)) throw new IllegalArgumentException("Detective has secret tickets");
				if (p.has(Ticket.DOUBLE)) throw new IllegalArgumentException("Detective has double tickets");
				if (!locations.add(p.location())) throw new IllegalArgumentException("Duplicate detective location");
			}
		}

		@Nonnull @Override public GameSetup getSetup() {return setup;}

		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {return log;}

		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			if (!winner.isEmpty()) return winner;

			// MrX wins if all moves are used
			if (log.size() >= setup.moves.size()) {winner = ImmutableSet.of(mrX.piece());
				return winner;
			}

			// Detectives win if they catch MrX
			List<Piece> detectivePieces = new ArrayList<>();
			for (Player detective : detectives) {
				if (detective.location() == mrX.location()) {
					for (Player d : detectives) { detectivePieces.add(d.piece()); }
					winner = ImmutableSet.copyOf(detectivePieces);
					return winner;
				}
			}

			// Detectives win if MrX is cornered
			boolean mrXCornered = true;
			for (int neighbor : setup.graph.adjacentNodes(mrX.location())) {
				if (!isOccupiedByDetective(neighbor)) {
					mrXCornered = false;
					break;
				}
			}

			if (mrXCornered) {
				for (Player d : detectives) { detectivePieces.add(d.piece()); }
				winner = ImmutableSet.copyOf(detectivePieces);
				return winner;
			}

			// MrX wins if all detectives are stuck
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

			// Detectives win if MrX is stuck during his turn
			if (remaining.contains(mrX.piece())) {
				Set<Move> mrXMoves = generateSingleMoves(mrX);
				if (mrXMoves.isEmpty()) {
					for (Player d : detectives) { detectivePieces.add(d.piece()); }
					winner = ImmutableSet.copyOf(detectivePieces);
					return winner;
				}
			}
			return winner;
		}

		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			if (!winner.isEmpty()) return ImmutableSet.of();
			Set<Move> availableMoves = new HashSet<>();
			for (Piece piece : remaining) {
				Player player = null;

				if (piece.isMrX()) {
					player = mrX;
				}
				else {
					player = getDetective(piece);
				}

				if (player != null) {Set<SingleMove> playerMoves = makeSingleMoves(setup, detectives, player, player.location());
					availableMoves.addAll(playerMoves);
				}
			}

			return ImmutableSet.copyOf(availableMoves);
		}


		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player p : detectives) {if (p.piece().equals(detective)) return Optional.of(p.location());}
			return Optional.empty();}

		@Nonnull @Override
		public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {
			for (Player p : detectives) {if (p.piece().equals(piece)) return Optional.of(ticket -> p.tickets().getOrDefault(ticket, 0));}
			if (mrX.piece().equals(piece)) return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
			return Optional.empty();
		}

		@Nonnull @Override public ImmutableSet<Piece> getPlayers() {
			Set<Piece> detectivePieces = new HashSet<>();
			for (Player detective : detectives) {detectivePieces.add(detective.piece());}
			return ImmutableSet.<Piece>builder().add(mrX.piece()).addAll(detectivePieces).build(); //builder - adds elements to set
			//bridge between immutable set and actually building it
		}

		@Override public GameState advance(Move move) {
			// Ensure move is valid
			Set<Move> availableMoves = getAvailableMoves();
			if (!availableMoves.contains(move)) {throw new IllegalArgumentException("Invalid move");}

			// Identify the player making the move
			Player player = null;
			if (move.commencedBy().isMrX()) {player = mrX;}
			else {player = getDetective(move.commencedBy());}

			if (player == null) {throw new IllegalArgumentException("Player not found");}

			// Handle MrX's move
			if (player.isMrX()) {
				SingleMove singleMove = (SingleMove) move;
				List<LogEntry> newLog = new ArrayList<>(log);

				if (setup.moves.get(newLog.size())) { // Check if move should be revealed
					newLog.add(LogEntry.reveal(singleMove.ticket, singleMove.destination));
				}
				else {newLog.add(LogEntry.hidden(singleMove.ticket));}

				Map<Ticket, Integer> newTickets = new HashMap<>(player.tickets());
				newTickets.put(singleMove.ticket, newTickets.get(singleMove.ticket) - 1);

				Player newMrX = new Player(player.piece(), ImmutableMap.copyOf(newTickets), singleMove.destination);

				// Create updated GameState
				List<Player> updatedDetectives = new ArrayList<>(detectives);
				Set<Piece> updatedRemaining = new HashSet<>();
				for (Player d : updatedDetectives) {updatedRemaining.add(d.piece());}

				return new MyGameState(setup, ImmutableSet.copyOf(updatedRemaining), ImmutableList.copyOf(newLog), newMrX, updatedDetectives);
			}

			// Setup Detective's Move
			SingleMove singleMove = (SingleMove) move;
			Ticket usedTicket = singleMove.ticket;

			// Update Detective's tickets
			Map<Ticket, Integer> detectiveTickets = new HashMap<>(player.tickets());
			detectiveTickets.put(usedTicket, detectiveTickets.get(usedTicket) - 1);
			Player updatedDetective = new Player(player.piece(), ImmutableMap.copyOf(detectiveTickets), singleMove.destination);

			// Update MrX's tickets
			Map<Ticket, Integer> mrXTickets = new HashMap<>(mrX.tickets());
			mrXTickets.put(usedTicket, mrXTickets.getOrDefault(usedTicket, 0) + 1);
			Player newMrX = new Player(mrX.piece(), ImmutableMap.copyOf(mrXTickets), mrX.location());

			// Update detectives list
			List<Player> newDetectives = new ArrayList<>();
			for (Player d : detectives) {
				if (d.piece().equals(updatedDetective.piece())) {
					newDetectives.add(updatedDetective);}
				else {newDetectives.add(d);}}

			// Update remaining pieces
			Set<Piece> newRemaining = new HashSet<>();
			for (Piece p : remaining) {
				if (!p.equals(player.piece())) {newRemaining.add(p);}}

			// Switch to MrX's turn if all detectives have moved
			if (newRemaining.isEmpty()) {newRemaining.add(newMrX.piece());}

			return new MyGameState(setup, ImmutableSet.copyOf(newRemaining), log, newMrX, newDetectives);}

		@Override public int getCount(@Nonnull Ticket ticket) {return log.size();}}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives)
	{return new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, detectives);}
}