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
			List<Observer> newObserverSet = new ArrayList<>();

			@Nonnull @Override
			public Board getCurrentBoard() {
				return null;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if (observer.equals(null)) throw new NullPointerException("Null observer");
				for (Observer o : observerSet) {
					if (o.equals(observer)) throw new IllegalArgumentException("Observer already registered");
				}
				newObserverSet.addAll(observerSet);
				newObserverSet.add(observer);
				observerSet = ImmutableSet.copyOf(newObserverSet);
				newObserverSet.clear();

//				observers.stream()
//				.filter(o -> o == observer)
//				.findAny()
//				.ifPresentOrElse(o -> {throw new IllegalArgumentException("Observer already registered");}, () -> observers.add(observer));
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if (this.equals(null)) throw new IllegalArgumentException("Observers list is empty");
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

				// TODO Advance the model with move, then notify all observers of what what just happened.
				//  you may want to use getWinner() to determine whether to send out Event.MOVE_MADE or Event.GAME_OVER

			}
		};
		// TODO
		//throw new RuntimeException("Implement me!");
	}
}
