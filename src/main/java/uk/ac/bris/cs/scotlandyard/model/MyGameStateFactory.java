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
		private Player mrX; // cannot be final as later on we need to assign him detectives' used tickets and you cant assign a value to final variable
		final private List<Player> detectives;
		private ImmutableSet<Piece> winner; // cannot be final as values need to be assigned when game has a winner


		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {

			Set<Integer> locations = new HashSet<>();
			List<Player> allPlayers = new ArrayList<>(detectives);
//			allPlayers.addAll(detectives);
			allPlayers.add(mrX);

			this.setup = setup;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();
			this.remaining = remaining; //pieces that are yet to move

			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty");
			if (this.detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty");
			if (!this.mrX.isMrX()) throw new IllegalArgumentException("MrX is null");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty");


			for (Player p : detectives) {
				if (p.isMrX()) throw new IllegalArgumentException("Detective cannot be MrX");
				if (p.has(Ticket.SECRET)) throw new IllegalArgumentException("Detective has secret tickets");
				if (p.has(Ticket.DOUBLE)) throw new IllegalArgumentException("Detective has double tickets");
				if (!locations.add(p.location())) throw new IllegalArgumentException("Duplicate detective location");
			}
		}


		@Nonnull @Override public GameSetup getSetup() {
			return setup;
		}

		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Override public int getCount(@Nonnull Ticket ticket) {
			return log.size();
		}

		private boolean isNodeOccupiedByDetective(int nodeLocation) {
			for (Player detective : detectives)
				if (detective.location() == nodeLocation) return true;
			return false;
		}

		private Set<Move> generateSingleMoves(Player player) {
			Set<Move> moves = new HashSet<>();

			for (int destination : setup.graph.adjacentNodes(player.location())) {
				if (player.isDetective() && isNodeOccupiedByDetective(destination)) continue; // exits this iteration of loop
				for (Transport t : setup.graph.edgeValueOrDefault(player.location(), destination, ImmutableSet.of())) {
					if (player.has(t.requiredTicket())) moves.add(new SingleMove(player.piece(), player.location(), t.requiredTicket(), destination));
				}
				//Secret ticket implementation for mrX
				if (player.has(Ticket.SECRET)) moves.add(new SingleMove(mrX.piece(), mrX.location(), Ticket.SECRET, destination));
			}
			return moves;
		}

		private GameState updatedState(Player updatedPlayer, List<LogEntry> newLog) {
			List<Player> updatedDetectives = new ArrayList<>(detectives);
			Set<Piece> updatedRemaining = new HashSet<>(remaining); // starts with current remaining

			// If the player was a detective, remove them from `remaining`
			if (!updatedPlayer.isMrX()) {
				updatedRemaining.remove(updatedPlayer.piece());

				// Replace detective in the list
				updatedDetectives.removeIf(d -> d.piece().equals(updatedPlayer.piece()));
				updatedDetectives.add(updatedPlayer);
			}

			// Detectives that haven't moved yet
			boolean detectivesStillMoving = updatedDetectives.stream()
					.anyMatch(d -> updatedRemaining.contains(d.piece()) && !generateSingleMoves(d).isEmpty());

			// If all detectives moved, reset turns to MrX
			if (!detectivesStillMoving) {
				updatedRemaining.clear();
				updatedRemaining.add(mrX.piece()); // Only MrX can move now
			}

			// If it's MrX’s turn, reset turns to all detectives after move
			if (updatedPlayer.isMrX()) {
				updatedRemaining.clear();
				for (Player d : updatedDetectives) {
					updatedRemaining.add(d.piece()); // Reset all detectives' turns
				}
			}


			return new MyGameState(setup, ImmutableSet.copyOf(updatedRemaining), ImmutableList.copyOf(newLog),
					updatedPlayer.isMrX() ? updatedPlayer : mrX, updatedDetectives);
		}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------
//------------------------------------------------------------------------------------------------------------------------------------------------------------------
//------------------------------------------------------------------------------------------------------------------------------------------------------------------

		// getWinner helper functions

		// Check if MrX wins by surviving all rounds
		private boolean isMrXWinnerBySurvival() {
			return log.size() == setup.moves.size() && remaining.contains(mrX.piece());
		}

		// Check if any detective caught MrX
		private boolean isMrXCaughtByDetectives() {
			for (Player detective : detectives) {
				if (detective.location() == mrX.location()) {
					return true;
				}
			}
			return false;
		}

		// Check if MrX is completely surrounded by detectives
		private boolean isMrXCornered() {
			for (int neighbor : setup.graph.adjacentNodes(mrX.location())) {
				if (!isNodeOccupiedByDetective(neighbor)) {
					return false; // MrX has at least one escape route
				}
			}
			return true;
		}

		// Check if all detectives are stuck (no available moves)
		private boolean areAllDetectivesStuck() {
			for (Player detective : detectives) {
				if (!generateSingleMoves(detective).isEmpty()) {
					return false; // At least one detective has a move
				}
			}
			return true;
		}

		// Get all detectives as winners
		private ImmutableSet<Piece> getDetectiveWinners() {
			Set<Piece> detectiveWinners = new HashSet<>();
			for (Player detective : detectives) {
				detectiveWinners.add(detective.piece());
			}
			return ImmutableSet.copyOf(detectiveWinners);
		}

		// Check if MrX is stuck (his turn, but no moves available)
		private boolean isMrXStuck() {
			return remaining.contains(mrX.piece()) && generateSingleMoves(mrX).isEmpty();
		}

		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			if (!winner.isEmpty()) return winner;

			if (isMrXWinnerBySurvival()) winner = ImmutableSet.of(mrX.piece());
			else if (isMrXCaughtByDetectives()) winner = getDetectiveWinners();
			else if (isMrXCornered()) winner = getDetectiveWinners();
			else if (areAllDetectivesStuck()) winner = ImmutableSet.of(mrX.piece());
			else if (isMrXStuck()) winner = getDetectiveWinners();

			return winner;
		}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------
//------------------------------------------------------------------------------------------------------------------------------------------------------------------
//------------------------------------------------------------------------------------------------------------------------------------------------------------------

		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves() {
			if (!winner.isEmpty()) {
				return ImmutableSet.of();
			}

			ImmutableSet.Builder<Move> moves = ImmutableSet.builder();

			if (remaining.contains(mrX.piece())) {
				addMrXMoves(moves);
			} else {
				addDetectiveMoves(moves);
			}

			return moves.build();
		}

		// Add all valid moves for MrX
		private void addMrXMoves(ImmutableSet.Builder<Move> moves) {
			Set<Move.SingleMove> singleMoves = getMrXSingleMoves();
			moves.addAll(singleMoves);

			if (canMrXUseDoubleMove()) {
				addMrXDoubleMoves(moves, singleMoves);
			}
		}

		// Add all valid moves for detectives
		private void addDetectiveMoves(ImmutableSet.Builder<Move> moves) {
			for (int i = 0; i < detectives.size(); i++) {
				Player detective = detectives.get(i);
				if (remaining.contains(detective.piece())) {
					moves.addAll(generateSingleMoves(detective));
				}
			}
		}

		// Generate valid single moves for MrX (avoiding occupied destinations)
		private Set<Move.SingleMove> getMrXSingleMoves() {
			Set<Move.SingleMove> singleMoves = new HashSet<>();

			for (Move move : generateSingleMoves(mrX)) {
				if (move instanceof Move.SingleMove) {
					Move.SingleMove singleMove = (Move.SingleMove) move;
					if (!isNodeOccupiedByDetective(singleMove.destination)) {
						singleMoves.add(singleMove);
					}
				}
			}

			return singleMoves;
		}

		// Check if MrX can use a DOUBLE move
		private boolean canMrXUseDoubleMove() {
			return mrX.has(Ticket.DOUBLE) && setup.moves.size() > log.size() + 1;
		}

		// Generate valid double moves for MrX
		private void addMrXDoubleMoves(ImmutableSet.Builder<Move> moves, Set<Move.SingleMove> singleMoves) {
			for (Move.SingleMove firstMove : singleMoves) {
				if (!mrX.has(firstMove.ticket)) continue;

				Player tempMrX = mrX.at(firstMove.destination);
				Set<Move.SingleMove> secondMoves = getMrXSingleMovesAfter(tempMrX);

				for (Move.SingleMove secondMove : secondMoves) {
					if (mrX.has(firstMove.ticket) && mrX.has(secondMove.ticket) &&
							(firstMove.ticket != secondMove.ticket || mrX.tickets().get(firstMove.ticket) > 1)) {
						moves.add(new Move.DoubleMove(
								mrX.piece(), mrX.location(),
								firstMove.ticket, firstMove.destination,
								secondMove.ticket, secondMove.destination));
					}
				}
			}
		}

		// Generate valid single moves for MrX AFTER taking the first move
		private Set<Move.SingleMove> getMrXSingleMovesAfter(Player tempMrX) {
			Set<Move.SingleMove> secondMoves = new HashSet<>();

			for (Move move : generateSingleMoves(tempMrX)) {
				if (move instanceof Move.SingleMove) {
					Move.SingleMove secondMove = (Move.SingleMove) move;
					if (!isNodeOccupiedByDetective(secondMove.destination)) {
						secondMoves.add(secondMove);
					}
				}
			}
			return secondMoves;
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
			Set<Piece> players = new HashSet<>();
			players.add(mrX.piece()); // Add MrX
			for (Player detective : detectives) {
				players.add(detective.piece()); // Add detectives
			}
			return ImmutableSet.copyOf(players);
		}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------
//------------------------------------------------------------------------------------------------------------------------------------------------------------------
//------------------------------------------------------------------------------------------------------------------------------------------------------------------

		private Player getDetective(Piece p) {
			for (Player detective : detectives) if (detective.piece().equals(p)) return detective;
			return null;
		}

		@Override
		public GameState advance(Move move) {
			// Ensure move is valid
			Set<Move> availableMoves = getAvailableMoves();
			if (!availableMoves.contains(move)) {
				throw new IllegalArgumentException("Invalid move");
			}

			// Identify the player making the move
			Player player = move.commencedBy().isMrX() ? mrX : getDetective(move.commencedBy());

			if (player == null) {
				throw new IllegalArgumentException("Player not found");
			}

			// Process the move using the visitor pattern
			return move.accept(new Move.Visitor<GameState>() {
				@Override
				public GameState visit(Move.SingleMove singleMove) {
					return processSingleMove(player, singleMove);
				}

				@Override
				public GameState visit(Move.DoubleMove doubleMove) {
					return processDoubleMove(player, doubleMove);
				}
			});
		}

		private GameState processSingleMove(Player player, Move.SingleMove singleMove) {
			List<LogEntry> newLog = new ArrayList<>(log);
			Map<Ticket, Integer> newTickets = new HashMap<>(player.tickets());
			newTickets.put(singleMove.ticket, newTickets.get(singleMove.ticket) - 1);

			Player updatedPlayer = new Player(player.piece(), ImmutableMap.copyOf(newTickets), singleMove.destination);

			// Debug MrX and detectives' ticket update
			if (player.isMrX()) {

				// Get the correct move index from the log size
				int moveIndex = newLog.size();
				boolean shouldReveal = setup.moves.get(moveIndex);

				// Log according to the reveal schedule
				newLog.add(shouldReveal ? LogEntry.reveal(singleMove.ticket, singleMove.destination)
						: LogEntry.hidden(singleMove.ticket));
			} else {
				// If a detective moves, MrX gains their ticket
				Map<Ticket, Integer> mrXTickets = new HashMap<>(mrX.tickets());
				mrXTickets.put(singleMove.ticket, mrXTickets.getOrDefault(singleMove.ticket, 0) + 1);
				mrX = new Player(mrX.piece(), ImmutableMap.copyOf(mrXTickets), mrX.location());

			}

			GameState newState = updatedState(updatedPlayer, newLog);

			return newState;
		}

		private GameState processDoubleMove(Player player, Move.DoubleMove doubleMove) {
			if (!player.isMrX()) {
				throw new IllegalArgumentException("Only MrX can perform a double move!");
			}

			List<LogEntry> newLog = new ArrayList<>(log);
			Map<Ticket, Integer> newTickets = new HashMap<>(player.tickets());
			newTickets.put(doubleMove.ticket1, newTickets.get(doubleMove.ticket1) - 1);
			newTickets.put(doubleMove.ticket2, newTickets.get(doubleMove.ticket2) - 1);
			newTickets.put(Ticket.DOUBLE, newTickets.get(Ticket.DOUBLE) - 1);

			Player updatedMrX = new Player(player.piece(), ImmutableMap.copyOf(newTickets), doubleMove.destination2);

			// Reveal/hide each move based on the reveal schedule
			int moveIndex1 = newLog.size();
			newLog.add(setup.moves.get(moveIndex1) ? LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1)
					: LogEntry.hidden(doubleMove.ticket1));

			int moveIndex2 = newLog.size();
			if (moveIndex2 < setup.moves.size()) {
				newLog.add(setup.moves.get(moveIndex2) ? LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2)
						: LogEntry.hidden(doubleMove.ticket2));
			} else {
				System.out.println("Warning: Double move second step out of reveal schedule bounds!");
			}

			GameState newState = updatedState(updatedMrX, newLog);

			return newState;
		}

	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives)
	{return new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, detectives);}
}