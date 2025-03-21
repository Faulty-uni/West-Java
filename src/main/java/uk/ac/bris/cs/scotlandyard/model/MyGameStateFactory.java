package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState, Board.TicketBoard {

		final private GameSetup setup;
		final private ImmutableSet<Piece> remaining; //pieces that still need to move
		final private ImmutableList<LogEntry> log; //MRX move log
		private Player mrX; // cannot be final as later on we need to assign him detectives' used tickets and you cant assign a value to final variable
		final private List<Player> detectives;
		private ImmutableSet<Piece> winner; // cannot be final as values need to be assigned when game has a winner


		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {


			this.setup = setup;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();
			this.remaining = remaining;

			if (setup.moves.isEmpty()) throw new IllegalArgumentException("No available moves!");
			if (this.detectives.isEmpty()) throw new IllegalArgumentException("No detectives found!");
			if (!this.mrX.isMrX()) throw new IllegalArgumentException("MrX is null!");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");

			Set<Integer> locations = new HashSet<>();
			for (Player detective : detectives) {
				if (detective.isMrX()) throw new IllegalArgumentException("Detective cannot be MrX!");
				if (detective.has(Ticket.SECRET)) throw new IllegalArgumentException("Detective cannot have secret tickets!");
				if (detective.has(Ticket.DOUBLE)) throw new IllegalArgumentException("Detective cannot have double tickets!");
				if (!locations.add(detective.location())) throw new IllegalArgumentException("Duplicate detective location found!");
			}
		}


		@Nonnull @Override public GameSetup getSetup() { //returns current game setup
			return setup;
		}

		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() { // returns mrX's travel log
			return log;
		}

		@Override public int getCount(@Nonnull Ticket t) { //returns the amount of tickets
			return log.size();
		}

		private Player getDetective(Piece p) { // returns the detective that is its turn
			for (Player detective : detectives) {
				if (detective.piece().equals(p)) return detective;
			}
			return null;
		}

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective detective) { //returns detective location, empty if detective is not part of the game
			for (Player p : detectives) {
				if (p.piece().equals(detective)) return Optional.of(p.location());
			}
			return Optional.empty();
		}

		@Nonnull @Override public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) { //gets the ticket of the current player
			for (Player detective: detectives) {
				if (detective.piece().equals(piece)) return Optional.of(ticket -> detective.tickets().getOrDefault(ticket, 0));
			} //get or default used in order to return 0 instead of null if no tickets are left
			if (mrX.piece().equals(piece)) return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
			return Optional.empty();
		}

		@Nonnull @Override public ImmutableSet<Piece> getPlayers() { //returns a set of all players in game
			Set<Piece> allPlayers = new HashSet<>();
			allPlayers.add(mrX.piece()); // add MrX
			for (Player detective : detectives) {
				allPlayers.add(detective.piece()); // add detectives
			}
			return ImmutableSet.copyOf(allPlayers);
		}

		private boolean isNodeOccupiedByDetective(int nodeLocation) { //checks if a node is occupied by a detective
			for (Player detective : detectives)
				if (detective.location() == nodeLocation) return true;
			return false;
		}

		private Set<Move.SingleMove> generateSingleMoves(Player player) { //generates all available single moves for a player from their current position

			Set<Move.SingleMove> moves = new HashSet<>();

			for (int destination : setup.graph.adjacentNodes(player.location())) {
				if (isNodeOccupiedByDetective(destination)) continue; // exits this iteration of loop if destination node is occupied by detective

				for (Transport t : setup.graph.edgeValueOrDefault(player.location(), destination, ImmutableSet.of())) { // if there is no connection between player location and
																														// destination it returns an empty set
					if (player.has(t.requiredTicket())) moves.add(new Move.SingleMove(player.piece(), player.location(), t.requiredTicket(), destination)); // if player has required ticket a new move is added to set
				}
				if (player.has(Ticket.SECRET)) moves.add(new Move.SingleMove(mrX.piece(), mrX.location(), Ticket.SECRET, destination));//secret ticket implementation only for mrX

			}
			return moves;
		}

		// getWinner & its helper functions

		private boolean isMrXWinnerByRoundsFinished() {	// check if MrX wins by all rounds finishing
			return log.size() == setup.moves.size() && remaining.contains(mrX.piece()); // true if only mrx remains and log size is the size of total moves
		}

		private boolean isMrXCaughtByDetectives() {	// checks if any detectives caught MrX
			for (Player detective : detectives) {
				if (detective.location() == mrX.location()) {
					return true;
				}
			}
			return false;
		}

		private boolean isMrXSurrounded() {	// check if MrX is surrounded by the detectives
			for (int neighbor : setup.graph.adjacentNodes(mrX.location())) {
				if (!isNodeOccupiedByDetective(neighbor)) {
					return false; // mrX has at least one escape route
				}
			}
			return true;
		}

		private boolean areAllDetectivesStuck() {	// check if all detectives are stuck
			for (Player detective : detectives) {
				if (!generateSingleMoves(detective).isEmpty()) {
					return false; // at least one detective has a move
				}
			}
			return true;
		}

		private ImmutableSet<Piece> getDetectiveWinners() {	// adds detectives to Hashset if they're winners
			Set<Piece> winningDetectives = new HashSet<>();
			for (Player detective : detectives) {
				winningDetectives.add(detective.piece());
			}
			return ImmutableSet.copyOf(winningDetectives);
		}

		private boolean isMrXStuck() {	// check if MrX is stuck (his turn, but no moves available)
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

		private Set<Move.SingleMove> getMrXSingleMoves() {// returns a set of valid SingleMoves for MrX
			Set<Move.SingleMove> singleMoves = generateSingleMoves(mrX);
			singleMoves.removeIf(move -> isNodeOccupiedByDetective(move.destination)); //remove any move where destination is occupied by a detective
			return singleMoves;
		}

		private Set<Move.SingleMove> getMrXSingleMovesAfter(Player mrX) {
			// generate single moves for MrX AFTER the first move
			// used as a bridge to get double moves
			Set<Move.SingleMove> secondMoves = generateSingleMoves(mrX);
			secondMoves.removeIf(move -> isNodeOccupiedByDetective(move.destination));
			return secondMoves;
		}

		private boolean canMrXUseDoubleMove() {	// check if MrX can use a double move
			return mrX.has(Ticket.DOUBLE) && setup.moves.size() > log.size() + 1; //checks if mrX has double ticket and if total available rounds have 2 space
		}

		private Set<Move> addMrXDoubleMoves(Set<Move.SingleMove> singleMoves) { //returns set of double moves for mrx
			Set<Move> moves = new HashSet<>();

			singleMoves.removeIf(move -> !mrX.has(move.ticket));	// remove first moves if MrX doesn't have the ticket

			for (Move.SingleMove firstMove : singleMoves) {
				Player mrX2 = mrX.at(firstMove.destination); //mrX2 acting as a bridge in the double move
				Set<Move.SingleMove> secondMoves = getMrXSingleMovesAfter(mrX2); //get the second moves

				secondMoves.removeIf(move -> !mrX.has(move.ticket));// remove second moves if MrX doesn't have the ticket

				for (Move.SingleMove secondMove : secondMoves) {
					if (mrX.tickets().get(firstMove.ticket) > 1 || firstMove.ticket != secondMove.ticket) { // if mrX has more than one type of ticket for its first move or both moves require different tickets
						moves.add(new Move.DoubleMove( //add double move to the set of moves
								mrX.piece(), mrX.location(), firstMove.ticket, firstMove.destination, secondMove.ticket, secondMove.destination));
					}
				}
			}
			return moves;
		}

		private Set<Move> addMrXMoves() {// add all  moves for MrX and return set
			Set<Move> moves = new HashSet<>();

			Set<Move.SingleMove> singleMoves = getMrXSingleMoves(); //set of single moves available
			moves.addAll(singleMoves); //add the single moves to set of moves

			if (canMrXUseDoubleMove()) {
				moves.addAll(addMrXDoubleMoves(singleMoves)); // return double moves and add them
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
				moves.addAll(addMrXMoves()); // if mrX is remaining add mrX moves to set of moves
			} else {
				moves.addAll(addDetectiveMoves()); // else add detective moves
			}

			return ImmutableSet.copyOf(moves);
		}


		private GameState updatedState(Player updatedPlayer, List<LogEntry> newLog) { //provides an updated game state after single or double move processed
			List<Player> updatedDetectives = new ArrayList<>(detectives); // starts with current detectives
			Set<Piece> updatedRemaining = new HashSet<>(remaining); // starts with current remaining

			if (!updatedPlayer.isMrX()) {// if the player moved is a detective, remove them from remaining
				updatedRemaining.remove(updatedPlayer.piece());

				updatedDetectives.removeIf(d -> d.piece().equals(updatedPlayer.piece()));// replaces detective in the list
				updatedDetectives.add(updatedPlayer); // adds the updated version (updated player) to the updated detectives
			}

			boolean detectivesStillMoving = false;
			for (Player d : updatedDetectives) { //checks if there is any detectives left to move
				if (updatedRemaining.contains(d.piece()) && !generateSingleMoves(d).isEmpty()) { //if piece of player d is still in game and has available moves then detectives still moving
					detectivesStillMoving = true;
					break;//loop breaks once we find one detective who can still move
				}
			}


			if (!detectivesStillMoving) {// if all detectives moved, reset turns to MrX
				updatedRemaining.clear();
				updatedRemaining.add(mrX.piece()); // only MrX can move now
			}

			if (updatedPlayer.isMrX()) {// if it's MrX’s turn, reset turns to all detectives after move
				updatedRemaining.clear();
				for (Player d : updatedDetectives) {
					updatedRemaining.add(d.piece()); //all detectives being added back to updated remaining
				}
			}

		// returns updated game state
			return new MyGameState(setup, ImmutableSet.copyOf(updatedRemaining), ImmutableList.copyOf(newLog), updatedPlayer.isMrX() ? updatedPlayer : mrX, updatedDetectives);
		}


		private GameState processSingleMove(Player player, Move.SingleMove singleMove) { //process a single move
			List<LogEntry> newLog = new ArrayList<>(log);	// create a copy of the current move log

			Map<Ticket, Integer> newTickets = new HashMap<>(player.tickets());// create a copy of the player's ticket map
			newTickets.put(singleMove.ticket, newTickets.get(singleMove.ticket) - 1);//reduce the used ticket amount

			// create an updated player
			Player updatedPlayer = new Player(player.piece(), ImmutableMap.copyOf(newTickets), singleMove.destination);

			if (player.isMrX()) {
				int moveIndex = newLog.size();// determine the correct move index based on the log size
				boolean revealMove = setup.moves.get(moveIndex); // determine where move should be revealed or hidden

				// log MrX's move based on the reveal schedule
				newLog.add(revealMove ? LogEntry.reveal(singleMove.ticket, singleMove.destination) : LogEntry.hidden(singleMove.ticket));
			} else {
				// if a detective moves, give used ticket to mrX
				Map<Ticket, Integer> mrXTickets = new HashMap<>(mrX.tickets());
				mrXTickets.put(singleMove.ticket, mrXTickets.getOrDefault(singleMove.ticket, 0) + 1);

				mrX = new Player(mrX.piece(), ImmutableMap.copyOf(mrXTickets), mrX.location());// update MrX with the new ticket count
			}

			// return the updated GameState with the new log and updated player
			return updatedState(updatedPlayer, newLog);
		}

		private GameState processDoubleMove(Player player, Move.DoubleMove doubleMove) {// processes a DoubleMove

			if (!player.isMrX()) { //only mrX can make a double move
				throw new IllegalArgumentException("Only MrX can perform a double move!");
			}

			List<LogEntry> newLog = new ArrayList<>(log);// create a copy of the move log for this update

			// create a copy of the player's ticket map and deduct the used tickets, similar to single move
			Map<Ticket, Integer> newTickets = new HashMap<>(player.tickets());
			newTickets.put(doubleMove.ticket1, newTickets.get(doubleMove.ticket1) - 1); //ticket one used
			newTickets.put(doubleMove.ticket2, newTickets.get(doubleMove.ticket2) - 1); //ticket two used
			newTickets.put(Ticket.DOUBLE, newTickets.get(Ticket.DOUBLE) - 1); // Double move ticket used

			// create an updated version of MrX
			Player updatedMrX = new Player(player.piece(), ImmutableMap.copyOf(newTickets), doubleMove.destination2);

			// determine if the first move should be revealed
			int moveIndex1 = newLog.size();
			newLog.add(setup.moves.get(moveIndex1) ? LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1)
					: LogEntry.hidden(doubleMove.ticket1));

			// determine if the second move should be revealed
			int moveIndex2 = newLog.size();
			newLog.add(setup.moves.get(moveIndex2) ? LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2)
						: LogEntry.hidden(doubleMove.ticket2));


			// return the updated GameState
			return updatedState(updatedMrX, newLog);
		}



		@Nonnull @Override public GameState advance(Move move) {
			Set<Move> availableMoves = getAvailableMoves();
			if (!availableMoves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);	// ensures move is valid

			Player player = move.commencedBy().isMrX() ? mrX : getDetective(move.commencedBy()); // identify the player making the move

			if (player == null) throw new IllegalArgumentException("Player not found");// if player is null (none selected) throw error



			return move.accept(new Move.Visitor<>() {// process the move using the visitor pattern
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
	{return new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, detectives);} //return game state
}