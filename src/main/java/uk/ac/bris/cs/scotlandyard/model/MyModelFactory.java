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
			}

			@Nonnull @Override
			public ImmutableSet<Observer> getObservers() {
				return Optional.ofNullable(observerSet).orElse(ImmutableSet.of());
			}


			@Override
			public void chooseMove(@Nonnull Move move) {

				if (!board.getAvailableMoves().contains(move)) { //if move isn't in getavailablemoves throw error
					throw new IllegalArgumentException("Invalid move attempted: " + move);
				}

				// Advance the game state
				board = board.advance(move);

				ImmutableSet<Piece> winner = board.getWinner(); // use getwinner to add winners to a set
				if (winner.isEmpty()) { //if there is no winner set nEvent to movemade
					nEvent = Observer.Event.MOVE_MADE;
				} else {
					nEvent = Observer.Event.GAME_OVER; // else set event to gameover
				}

				for (Observer o : observerSet) {// Notify all observers
					o.onModelChanged(board, nEvent);
				}
			}
		};
	}
}