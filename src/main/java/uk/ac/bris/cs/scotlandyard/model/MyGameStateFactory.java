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
			List<Player> allPlayers = new ArrayList<>();
			allPlayers.addAll(detectives);
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


		@Nonnull @Override public GameSetup getSetup() { //returns current gamesetup
			return setup;
		}

		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() { // returns mrX's travel log
			return log;
		}

		@Override public int getCount(@Nonnull Ticket t) { //returns the amount of tickets
			return log.size();
		}

		private Player getDetective(Piece p) { // returns the detective that is its turn
			for (Player detective : detectives) if (detective.piece().equals(p)) return detective;
			return null;
		}

		@Override public Optional<Integer> getDetectiveLocation(Detective detective) { //returns detective location, empty if detective is not is not part of the game
			for (Player p : detectives) {
				if (p.piece().equals(detective)) return Optional.of(p.location());
			}
			return Optional.empty();
		}

		@Nonnull @Override public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) { //gets the ticket of the current player
			for (Player d: detectives) {
				if (d.piece().equals(piece)) return Optional.of(ticket -> d.tickets().getOrDefault(ticket, 0));
			} //get or default used in order to return 0 instead of null if no tickets are left
			if (mrX.piece().equals(piece)) return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
			return Optional.empty();
		}

		@Nonnull @Override public ImmutableSet<Piece> getPlayers() { //returns a set of all players in game
			Set<Piece> players = new HashSet<>();
			players.add(mrX.piece()); // Add MrX
			for (Player d : detectives) {
				players.add(d.piece()); // Add detectives
			}
			return ImmutableSet.copyOf(players);
		}

		private boolean isNodeOccupiedByDetective(int nodeLocation) { //checks if a node is occupied by a detective
			for (Player d : detectives)
				if (d.location() == nodeLocation) return true;
			return false;
		}

		private Set<Move> generateSingleMoves(Player player) { //generates all available singlemoves for a player from their current position

			Set<Move> moves = new HashSet<>();

			for (int destination : setup.graph.adjacentNodes(player.location())) {
				if (isNodeOccupiedByDetective(destination)) continue; // exits this iteration of loop if destination node is occupied by detective

				for (Transport t : setup.graph.edgeValueOrDefault(player.location(), destination, ImmutableSet.of())) { // if there is no connection between player loc and
																														// destination it returns an empty set
					if (player.has(t.requiredTicket())) moves.add(new SingleMove(player.piece(), player.location(), t.requiredTicket(), destination)); // if player has required ticket a new move is added to set
				}
				if (player.has(Ticket.SECRET)) moves.add(new SingleMove(mrX.piece(), mrX.location(), Ticket.SECRET, destination));//Secret ticket implementation only for mrX

			}
			return moves;
		}

		// getWinner & its helper functions

		private boolean isMrXWinnerByRoundsFinished() {	// Check if MrX wins by all rounds finishing
			return log.size() == setup.moves.size() && remaining.contains(mrX.piece()); // true if only mrx remains and log size is the size of total moves
		}

		private boolean isMrXCaughtByDetectives() {	// Checks if any detectives caught MrX
			for (Player detective : detectives) {
				if (detective.location() == mrX.location()) {
					return true;
				}
			}
			return false;
		}

		private boolean isMrXSurrounded() {	// Check if MrX is surrounded by the detectives
			for (int neighbor : setup.graph.adjacentNodes(mrX.location())) {
				if (!isNodeOccupiedByDetective(neighbor)) {
					return false; // MrX has at least one escape route
				}
			}
			return true;
		}

		private boolean areAllDetectivesStuck() {	// Check if all detectives are stuck
			for (Player detective : detectives) {
				if (!generateSingleMoves(detective).isEmpty()) {
					return false; // At least one detective has a move
				}
			}
			return true;
		}

		private ImmutableSet<Piece> getDetectiveWinners() {	// adds akk detectives to Hashset if they're winners
			Set<Piece> detectiveWinners = new HashSet<>();
			for (Player detective : detectives) {
				detectiveWinners.add(detective.piece());
			}
			return ImmutableSet.copyOf(detectiveWinners);
		}

		private boolean isMrXStuck() {	// Check if MrX is stuck (his turn, but no moves available)
			return remaining.contains(mrX.piece()) && generateSingleMoves(mrX).isEmpty();
		}

		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			if (!winner.isEmpty()) return winner;

			if (isMrXWinnerByRoundsFinished()) winner = ImmutableSet.of(mrX.piece());
			else if (isMrXCaughtByDetectives()) winner = getDetectiveWinners();
			else if (isMrXSurrounded()) winner = getDetectiveWinners();
			else if (areAllDetectivesStuck()) winner = ImmutableSet.of(mrX.piece());
			else if (isMrXStuck()) winner = getDetectiveWinners();

			return winner;
		}

		//getAvailableMoves and it's helper functions

		private Set<Move.SingleMove> getMrXSingleMoves() {	//returns a set of singlemoves for mrx
			Set<Move.SingleMove> singleMoves = new HashSet<>();

			for (Move move : generateSingleMoves(mrX)) {
				if (move instanceof Move.SingleMove) { // check if move is as of type Singlemove
					Move.SingleMove singleMove = (Move.SingleMove) move; // downcast move from parent class to access singleMove
					if (!isNodeOccupiedByDetective(singleMove.destination)) { //if Node isn't occupied by a detective add that singleMove to the set
						singleMoves.add(singleMove);
					}
				}
			}

			return singleMoves;
		}

		private Set<Move.SingleMove> getMrXSingleMovesAfter(Player mrX) {// Generate single moves for MrX AFTER taking the first move
			Set<Move.SingleMove> secondMoves = new HashSet<>();

			for (Move move : generateSingleMoves(mrX)) { //just like getMrXSingleMoves
				if (move instanceof Move.SingleMove) {
					Move.SingleMove secondMove = (Move.SingleMove) move;
					if (!isNodeOccupiedByDetective(secondMove.destination)) {
						secondMoves.add(secondMove);
					}
				}
			}

			return secondMoves;
		}

		private boolean canMrXUseDoubleMove() {	// Check if MrX can use a DOUBLE move
			return mrX.has(Ticket.DOUBLE) && setup.moves.size() > log.size() + 1; //checks if mrX has doubleticket and if total available rounds have 2 space
		}

		private Set<Move> addMrXDoubleMoves(Set<Move.SingleMove> singleMoves) {
			Set<Move> moves = new HashSet<>();

			for (Move.SingleMove firstMove : singleMoves) {
				if (!mrX.has(firstMove.ticket)) continue; //if mrX doesnt have ticket for first move exit iteration fo this loop

				Player tempMrX = mrX.at(firstMove.destination); //make a temporary mrX located at the destination of the first move
				Set<Move.SingleMove> secondMoves = getMrXSingleMovesAfter(tempMrX); //get the singlemoves for mrX after his potential first move

				for (Move.SingleMove secondMove : secondMoves) {
					if (mrX.has(firstMove.ticket) && mrX.has(secondMove.ticket) && // if mrX has tickets for both first and second move
							(firstMove.ticket != secondMove.ticket || mrX.tickets().get(firstMove.ticket) > 1)) { // and the ticket for both moves aren't the same or he has more than one of those tickets

						Move.DoubleMove doubleMove = new Move.DoubleMove(// generate a new double move
								mrX.piece(), mrX.location(),
								firstMove.ticket, firstMove.destination,
								secondMove.ticket, secondMove.destination);

						moves.add(doubleMove);//add double move to set
					}
				}
			}

			return moves; // Returns a mutable set, caller must convert to immutableSet if needed
		}





		private Set<Move> addMrXMoves() {// Add all  moves for MrX and return set
			Set<Move> moves = new HashSet<>();

			Set<Move.SingleMove> singleMoves = getMrXSingleMoves(); //set of singlemoves available
			moves.addAll(singleMoves); //add the singlemoves to set of moves

			if (canMrXUseDoubleMove()) {
				moves.addAll(addMrXDoubleMoves(singleMoves)); // Return double moves and add them
			}

			return moves;
		}


		private Set<Move> addDetectiveMoves() { //add all moves for detectives and return set
			Set<Move> moves = new HashSet<>();

			for (Player detective : detectives) {
				if (remaining.contains(detective.piece())) { //if detective is still in game(remaining) add the generated single moves to the move set
					moves.addAll(generateSingleMoves(detective));
				}
			}

			return moves;
		}


		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves() { // returns a copy of the set of moves
			if (!winner.isEmpty()) {
				return ImmutableSet.of(); // if there is a winner return an empty set
			}

			Set<Move> moves = new HashSet<>();

			if (remaining.contains(mrX.piece())) {
				moves.addAll(addMrXMoves()); // if mrX is remaining add mrXmoves to set of moves
			} else {
				moves.addAll(addDetectiveMoves()); // else add detective moves
			}

			return ImmutableSet.copyOf(moves);
		}


		private GameState updatedState(Player updatedPlayer, List<LogEntry> newLog) { //provides an updated gamestate after single or double move processed
			List<Player> updatedDetectives = new ArrayList<>(detectives); // starts with crrent detectives
			Set<Piece> updatedRemaining = new HashSet<>(remaining); // starts with current remaining

			if (!updatedPlayer.isMrX()) {// If the player moved is a detective, remove them from remaining
				updatedRemaining.remove(updatedPlayer.piece());

				updatedDetectives.removeIf(d -> d.piece().equals(updatedPlayer.piece()));// Replaces detective in the list
				updatedDetectives.add(updatedPlayer); // adds the updated version (updated player) to the updated detectives
			}

			boolean detectivesStillMoving = false;
			for (Player d : updatedDetectives) { //checks if there is any detectives left to move
				if (updatedRemaining.contains(d.piece()) && !generateSingleMoves(d).isEmpty()) { //if piece of player d is still in game and has available moves then detectives still moving
					detectivesStillMoving = true;
					break;//loop breaks once we find one detective who can still move
				}
			}


			if (!detectivesStillMoving) {// If all detectives moved, reset turns to MrX
				updatedRemaining.clear();
				updatedRemaining.add(mrX.piece()); // Only MrX can move now
			}

			if (updatedPlayer.isMrX()) {// If it's MrX’s turn, reset turns to all detectives after move
				updatedRemaining.clear();
				for (Player d : updatedDetectives) {
					updatedRemaining.add(d.piece()); // All detectives being added back to updated remaining
				}
			}

		// returns updated gamestate
			return new MyGameState(setup, ImmutableSet.copyOf(updatedRemaining), ImmutableList.copyOf(newLog), updatedPlayer.isMrX() ? updatedPlayer : mrX, updatedDetectives);
		}


		private GameState processSingleMove(Player player, Move.SingleMove singleMove) { //process a singlemove
			List<LogEntry> newLog = new ArrayList<>(log);	// Create a copy of the current move log

			Map<Ticket, Integer> newTickets = new HashMap<>(player.tickets());// Create a copy of the player's ticket map
			newTickets.put(singleMove.ticket, newTickets.get(singleMove.ticket) - 1);//reduce the used ticket amount

			// Create an updated player
			Player updatedPlayer = new Player(player.piece(), ImmutableMap.copyOf(newTickets), singleMove.destination);

			if (player.isMrX()) {
				int moveIndex = newLog.size();// determine the correct move index based on the log size
				boolean shouldReveal = setup.moves.get(moveIndex); // determine where move should be revealed or hidden

				// Log MrX's move based on the reveal schedule
				newLog.add(shouldReveal ? LogEntry.reveal(singleMove.ticket, singleMove.destination) : LogEntry.hidden(singleMove.ticket));
			} else {
				// If a detective moves, give used ticket ot mrX
				Map<Ticket, Integer> mrXTickets = new HashMap<>(mrX.tickets());
				mrXTickets.put(singleMove.ticket, mrXTickets.getOrDefault(singleMove.ticket, 0) + 1);

				mrX = new Player(mrX.piece(), ImmutableMap.copyOf(mrXTickets), mrX.location());// Update MrX with the new ticket count
			}

			// Return the updated GameState with the new log and updated player
			return updatedState(updatedPlayer, newLog);
		}

		private GameState processDoubleMove(Player player, Move.DoubleMove doubleMove) {// Processes a DoubleMove

			if (!player.isMrX()) { //only mrX can make a double move
				throw new IllegalArgumentException("Only MrX can perform a double move!");
			}

			List<LogEntry> newLog = new ArrayList<>(log);// Create a copy of the move log for this update

			// Create a copy of the player's ticket map and deduct the used tickets, similar to singlemove
			Map<Ticket, Integer> newTickets = new HashMap<>(player.tickets());
			newTickets.put(doubleMove.ticket1, newTickets.get(doubleMove.ticket1) - 1);
			newTickets.put(doubleMove.ticket2, newTickets.get(doubleMove.ticket2) - 1);
			newTickets.put(Ticket.DOUBLE, newTickets.get(Ticket.DOUBLE) - 1); // Double move ticket used

			// Create an updated version of MrX
			Player updatedMrX = new Player(player.piece(), ImmutableMap.copyOf(newTickets), doubleMove.destination2);

			// Determine if the first move should be revealed
			int moveIndex1 = newLog.size();
			newLog.add(setup.moves.get(moveIndex1) ? LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1)
					: LogEntry.hidden(doubleMove.ticket1));

			// Determine if the second move should be revealed
			int moveIndex2 = newLog.size();
			newLog.add(setup.moves.get(moveIndex2) ? LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2)
						: LogEntry.hidden(doubleMove.ticket2));


			// Return the updated GameState
			return updatedState(updatedMrX, newLog);
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
			return move.accept(new Move.Visitor<>() {
                @Override public GameState visit(Move.SingleMove singleMove) {
                    return processSingleMove(player, singleMove);
                }

                @Override public GameState visit(Move.DoubleMove doubleMove) {
                    return processDoubleMove(player, doubleMove);
                }
            });
		}
	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives)
	{return new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, detectives);} //return gamestate
}