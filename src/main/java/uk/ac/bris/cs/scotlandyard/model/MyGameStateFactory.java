package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.stream.Collectors;
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
		private Player mrX;
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

		private GameState updateGameState(Player updatedPlayer, List<LogEntry> newLog) {
			List<Player> updatedDetectives = new ArrayList<>(detectives);
			Set<Piece> updatedRemaining = new HashSet<>();

			// Replace the detective in the list if they moved
			if (!updatedPlayer.isMrX()) {
				updatedDetectives.removeIf(d -> d.piece().equals(updatedPlayer.piece()));
				updatedDetectives.add(updatedPlayer);
			}

			// If a detective moved, ensure other detectives still get their turns
			boolean detectivesStillMoving = false;
			if (!updatedPlayer.isMrX()) {
				for (Player detective : updatedDetectives) {
					if (!detective.piece().equals(updatedPlayer.piece()) && !generateSingleMoves(detective).isEmpty()) {
						updatedRemaining.add(detective.piece());
						detectivesStillMoving = true;
					}
				}
			}

			// If all detectives are stuck, immediately switch to MrX**
			if (!detectivesStillMoving) {
				System.out.println("All detectives stuck switching to MrX");
				updatedRemaining.clear();
				updatedRemaining.add(mrX.piece());
			}

			// If MrX moved, reset turns to all detectives
			if (updatedPlayer.isMrX()) {
				updatedRemaining.clear();
				for (Player d : updatedDetectives) {
					updatedRemaining.add(d.piece());
				}
			}

			// Ensure MrX gets his turn when he should
			if (updatedRemaining.isEmpty()) {
				updatedRemaining.add(mrX.piece());
			}

			// Deb,Print the new remaining players after the move
			System.out.println("New remaining players after update: " + updatedRemaining);

			return new MyGameState(setup, ImmutableSet.copyOf(updatedRemaining), ImmutableList.copyOf(newLog),
					updatedPlayer.isMrX() ? updatedPlayer : mrX, updatedDetectives);
		}



		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			Set<SingleMove> possibleSingleMoves = new HashSet<>();
			Set<Integer> detectiveLocations = new HashSet<>();
			for (Player detective : detectives) {
				detectiveLocations.add(detective.location());
			}
			for (int destination : setup.graph.adjacentNodes(source)) {
				if (detectiveLocations.contains(destination)) continue;
				for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if (player.has(t.requiredTicket()))
						possibleSingleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
				}
				if (player.has(Ticket.SECRET))
					possibleSingleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
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
			if (!winner.isEmpty()) {
				System.out.println("Game over! Winner: " + winner);
				return winner;
			}

			// MrX wins if all moves are used
			if (log.size() >= setup.moves.size()) {
				System.out.println("MrX wins: All moves used up.");
				winner = ImmutableSet.of(mrX.piece());
				return winner;
			}

			// Detectives win if they catch MrX
			List<Piece> detectivePieces = new ArrayList<>();
			for (Player detective : detectives) {
				if (detective.location() == mrX.location()) {
					System.out.println("X caught at location " + mrX.location() + "Detectives win.");
					for (Player d : detectives) {
						detectivePieces.add(d.piece());
					}
					winner = ImmutableSet.copyOf(detectivePieces);
					return winner;
				}
			}

			// Detectives win if MrX is truly cornered (ALL surrounding nodes occupied by detectives)
			boolean mrXCornered = true;
			for (int neighbor : setup.graph.adjacentNodes(mrX.location())) {
				if (!isOccupiedByDetective(neighbor)) {
					mrXCornered = false;
					break;
				}
			}

			if (mrXCornered) {
				System.out.println("MrX is cornered!");
				for (Player d : detectives) {
					detectivePieces.add(d.piece());
				}
				winner = ImmutableSet.copyOf(detectivePieces);
				return winner;
			}

			// MrX wins if ALL detectives are stuck (not just one detective)
			boolean allDetectivesStuck = true;
			for (Player detective : detectives) {
				if (!generateSingleMoves(detective).isEmpty()) {
					allDetectivesStuck = false; // At least one detective can move
					break;
				}
			}

			if (allDetectivesStuck) {
				System.out.println("All detectives are stuck. MrX wins.");
				winner = ImmutableSet.of(mrX.piece());
				return winner;
			}

			// Detectives win if MrX is stuck during his turn
			if (remaining.contains(mrX.piece())) {
				Set<Move> mrXMoves = getAvailableMoves();
				if (mrXMoves.isEmpty()) {
					System.out.println("MrX is stuck on his turn! Detectives win.");
					for (Player d : detectives) {
						detectivePieces.add(d.piece());
					}
					winner = ImmutableSet.copyOf(detectivePieces);
					return winner;
				} else {
					System.out.println(" MrX still has moves: " + mrXMoves);
				}
			} else {
				System.out.println("Not MrX’s turn. Game continues.");
			}

			return winner;
		}


		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves() {
			if (!winner.isEmpty()) {
				System.out.println("Game over! Winner: " + winner);
				return ImmutableSet.of();
			}

			ImmutableSet.Builder<Move> moves = ImmutableSet.builder();

			// Generate moves for MrX
			if (remaining.contains(mrX.piece())) {
				System.out.println("Checking available moves for MrX at " + mrX.location());
				Set<Move.SingleMove> singleMoves = generateSingleMoves(mrX).stream()
						.filter(move -> move instanceof Move.SingleMove)
						.map(move -> (Move.SingleMove) move)
						.collect(Collectors.toSet());

				// Always add SingleMoves
				moves.addAll(singleMoves);

				// Debug single moves
				System.out.println("MrX Single Moves: " + singleMoves);

				// Add DoubleMoves if MrX has a DOUBLE ticket
				if (mrX.has(Ticket.DOUBLE)) {
					System.out.println("MrX has DOUBLE ticket, checking DoubleMoves...");
					for (Move.SingleMove firstMove : singleMoves) {
						Player tempMrX = mrX.at(firstMove.destination);

						Set<Move.SingleMove> secondMoves = generateSingleMoves(tempMrX).stream()
								.filter(move -> move instanceof Move.SingleMove)
								.map(move -> (Move.SingleMove) move)
								.collect(Collectors.toSet());

						for (Move.SingleMove secondMove : secondMoves) {
							if (mrX.has(firstMove.ticket) && mrX.has(secondMove.ticket)) {
								Move.DoubleMove doubleMove = new Move.DoubleMove(mrX.piece(),
										mrX.location(),
										firstMove.ticket, firstMove.destination,
										secondMove.ticket, secondMove.destination);
								moves.add(doubleMove);
								System.out.println("MrX Double Move added: " + doubleMove);
							}
						}
					}
				}
			}

			// Generate moves for detectives
			for (Player detective : detectives) {
				if (remaining.contains(detective.piece())) {
					moves.addAll(generateSingleMoves(detective));
				}
			}

			ImmutableSet<Move> availableMoves = moves.build();
			System.out.println("Final available moves: " + availableMoves);
			return availableMoves;
		}





		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player p : detectives) {
				if (p.piece().equals(detective)) return Optional.of(p.location());
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {
			for (Player p : detectives) {
				if (p.piece().equals(piece)) return Optional.of(ticket -> p.tickets().getOrDefault(ticket, 0));
			}
			if (mrX.piece().equals(piece)) return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
			return Optional.empty();
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> detectivePieces = new HashSet<>();
			for (Player detective : detectives) {
				detectivePieces.add(detective.piece());
			}
			return ImmutableSet.<Piece>builder().add(mrX.piece()).addAll(detectivePieces).build(); //builder - adds elements to set
			//bridge between immutable set and actually building it
		}

		@Override
		public GameState advance(Move move) {
			// Ensure move is valid
			Set<Move> availableMoves = getAvailableMoves();
			if (!availableMoves.contains(move)) {
				System.out.println("Invalid move attempted: " + move);
				System.out.println("Available moves: " + availableMoves);
				throw new IllegalArgumentException("Invalid move");
			}

			// Identify the player making the move
			Player player;
			if (move.commencedBy().isMrX()) {
				player = mrX;
			} else {
				player = getDetective(move.commencedBy());
			}

			if (player == null) {
				System.out.println("ERROR: Player not found for move: " + move);
				throw new IllegalArgumentException("Player not found");
			}

			// Debug before processing
			System.out.println("Processing move: " + move);
			System.out.println("Player before move: " + player);
			System.out.println("Tickets before move: " + player.tickets());

			// Process the move using the visitor pattern
			return move.accept(new Move.Visitor<GameState>() {

				@Override
				public GameState visit(Move.SingleMove singleMove) {
					System.out.println("Single move detected: " + singleMove);

					return processSingleMove(player, singleMove);
				}

				@Override
				public GameState visit(Move.DoubleMove doubleMove) {
					System.out.println("Double move detected: " + doubleMove);

					return processDoubleMove(player, doubleMove);
				}
			});
		}

		private GameState processSingleMove(Player player, Move.SingleMove singleMove) {
			List<LogEntry> newLog = new ArrayList<>(log);
			Map<Ticket, Integer> newTickets = new HashMap<>(player.tickets());
			newTickets.put(singleMove.ticket, newTickets.get(singleMove.ticket) - 1);

			System.out.println("Ticket " + singleMove.ticket + " used by " + player.piece());

			Player updatedPlayer = new Player(player.piece(), ImmutableMap.copyOf(newTickets), singleMove.destination);

			// Debug MrX and detectives' ticket update
			if (player.isMrX()) {
				System.out.println("MrX moved to: " + singleMove.destination);
				if (setup.moves.get(newLog.size())) {
					newLog.add(LogEntry.reveal(singleMove.ticket, singleMove.destination));
				} else {
					newLog.add(LogEntry.hidden(singleMove.ticket));
				}
			} else {
				// If a detective moves, MrX gains their ticket
				Map<Ticket, Integer> mrXTickets = new HashMap<>(mrX.tickets());
				mrXTickets.put(singleMove.ticket, mrXTickets.getOrDefault(singleMove.ticket, 0) + 1);
				mrX = new Player(mrX.piece(), ImmutableMap.copyOf(mrXTickets), mrX.location());

				System.out.println("Updated MrX tickets: " + mrX.tickets());
			}

			GameState newState = updateGameState(updatedPlayer, newLog);

			// Debugging remaining moves
			System.out.println("Remaining pieces to move: " + newState.getPlayers());

			return newState;
		}

		private GameState processDoubleMove(Player player, Move.DoubleMove doubleMove) {
			if (!player.isMrX()) {
				System.out.println("ERROR: A detective tried to make a double move!");
				throw new IllegalArgumentException("Only MrX can perform a double move!");
			}

			List<LogEntry> newLog = new ArrayList<>(log);
			Map<Ticket, Integer> newTickets = new HashMap<>(player.tickets());
			newTickets.put(doubleMove.ticket1, newTickets.get(doubleMove.ticket1) - 1);
			newTickets.put(doubleMove.ticket2, newTickets.get(doubleMove.ticket2) - 1);
			newTickets.put(Ticket.DOUBLE, newTickets.get(Ticket.DOUBLE) - 1);

			Player updatedMrX = new Player(player.piece(), ImmutableMap.copyOf(newTickets), doubleMove.destination2);

			if (setup.moves.get(newLog.size())) {
				newLog.add(LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1));
			} else {
				newLog.add(LogEntry.hidden(doubleMove.ticket1));
			}

			if (setup.moves.get(newLog.size() + 1)) {
				newLog.add(LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2));
			} else {
				newLog.add(LogEntry.hidden(doubleMove.ticket2));
			}

			GameState newState = updateGameState(updatedMrX, newLog);

			// Debugging remaining moves
			System.out.println("Remaining pieces to move: " + newState.getPlayers());

			return newState;
		}



		@Override public int getCount(@Nonnull Ticket ticket) {return log.size();}}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives)
	{return new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, detectives);}
}