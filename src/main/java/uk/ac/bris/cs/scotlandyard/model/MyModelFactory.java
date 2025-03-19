package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	@Nonnull @Override public Model build(GameSetup setup,
										  Player mrX,
										  ImmutableList<Player> detectives) {
		return new Model(){
			ImmutableSet<Model.Observer> observerSet = ImmutableSet.of();
			private List<Observer> newObserverSet = new ArrayList<>(); //intermediary list used to add new elements to ImmutableSet
			private Observer.Event nEvent;
			private Board.GameState board = new MyGameStateFactory().build(setup,mrX,detectives);

			@Nonnull @Override public Board getCurrentBoard() {
				return board;
			}

			@Override public void registerObserver(@Nonnull Observer observer) {
				if (observer.equals(null)) throw new NullPointerException("Null observer");
				if (observerSet.contains(observer)) throw new IllegalArgumentException("Observer already registered");
				newObserverSet.addAll(observerSet);
				newObserverSet.add(observer);
				observerSet = ImmutableSet.copyOf(newObserverSet);
				newObserverSet.clear();

//				newObserverSet.addAll(observerSet); //added first so that findAny can look through the list
//				newObserverSet.stream()
//				.findAny()						//find any: Optional return type, either null or set
//				.filter(o -> o.equals(observer))		//filter: continue stream with o st. o == observer
//				.ifPresentOrElse(o -> {throw new IllegalArgumentException("Observer already registered");}, //find any is required for this line to work otherwise stream doesn't know if it can be evaluated (nothing searched)
//						() -> {newObserverSet.add(observer); //() - no arguments given therefore the next pieces of code are executed without any lambda parameters
//						observerSet = ImmutableSet.copyOf(newObserverSet); // code in {} acts like normal code
//						newObserverSet.clear();}
//				);
//				Stream works, unused for clarity
//				Find Any and Filter have been switched around and stream works either way
//				assumed that when findAny is first, list of Optionals is fed into filter
//				other way round is where findAny matches the predicate of the filter
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if (observer.equals(null)) throw new NullPointerException("Null observer");
				if (observerSet.contains(observer)) {
					newObserverSet.addAll(observerSet);
					newObserverSet.remove(observer);
					observerSet = ImmutableSet.copyOf(newObserverSet);
					newObserverSet.clear();
				}
				else throw new IllegalArgumentException("Observer not registered");
//				newObserverSet.addAll(observerSet);
//				newObserverSet.stream()
//				.findAny()
//				.filter(o -> o.equals(observer))
//				.ifPresentOrElse((o) -> {
//					newObserverSet.remove(o);
//					observerSet = ImmutableSet.copyOf(newObserverSet);
//					newObserverSet.clear();},
//						() -> {newObserverSet.clear();
//					if (observer.equals(null)) throw new NullPointerException("Null observer");
//					else throw new IllegalArgumentException("Observer not registered");});
//				Stream works, unused for same reason as above
			}

			@Nonnull @Override
			public ImmutableSet<Observer> getObservers() {
				return Optional.ofNullable(observerSet).orElse(ImmutableSet.of());
			}



			//					Observer.Event.MOVE_MADE;
//				for (Player player : detectives) {
//					if (getCurrentBoard().getPlayerTickets(player.piece()).isEmpty()) continue;
//					else moveSize += getCurrentBoard().getPlayerTickets(player.piece()).stream().count();
//				}
//				if (moveSize < 1) detectivesCanMove = false;
//
//				if(getCurrentBoard().getWinner().isEmpty() && (detectivesCanMove)){
//					System.out.println("\n winner set: "+getCurrentBoard().getWinner().size());
//					System.out.println("Detective moves: "+moveSize);
//					System.out.println("MrX available moves: "+mrX.tickets().size());
//
//					if ((getCurrentBoard().getWinner().contains(mrX) || getCurrentBoard().getWinner().contains(detectives))) {
//						nEvent = Observer.Event.GAME_OVER;
//					}
//					if (moveSize <= 1 & !getCurrentBoard().getWinner().isEmpty()) nEvent = Observer.Event.GAME_OVER;
//					else nEvent = Observer.Event.MOVE_MADE;
//					for (Observer o : observerSet){
//						o.onModelChanged(getCurrentBoard(),nEvent);
//					}
//                }
//				else {
//					nEvent = Observer.Event.GAME_OVER;
//					for (Observer o : observerSet) {
//						o.onModelChanged(getCurrentBoard(), nEvent);
//					}
//				}
//				Observer.Event.GAME_OVER.;

			@Override
			public void chooseMove(@Nonnull Move move) {

				if (!board.getAvailableMoves().contains(move)) {
					throw new IllegalArgumentException("Invalid move attempted: " + move);
				}

				// Advance the game state
				board = board.advance(move);

				// Debugging: Print winner status
				ImmutableSet<Piece> winner = board.getWinner();
				if (winner.isEmpty()) {
					nEvent = Observer.Event.MOVE_MADE;
				} else {
					nEvent = Observer.Event.GAME_OVER;
				}

				// Notify all observers
				for (Observer o : observerSet) {
					o.onModelChanged(board, nEvent);
				}
			}
		};
	}
}