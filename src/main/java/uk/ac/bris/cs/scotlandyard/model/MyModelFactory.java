package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.Objects;
import java.util.Optional;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	ImmutableSet<Model.Observer> observers = ImmutableSet.of();

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new Model(){

			@Nonnull @Override
			public Board getCurrentBoard() {
				return null;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if (observer.equals(null)) throw new NullPointerException("Null observer");
				//if (observers.contains(observer)) throw new IllegalArgumentException("Observer already registered");
				for (Observer o : observers) {
					if (o.equals(observer)) throw new IllegalArgumentException("Observer already registered");
				}
				observers.add(observer);

			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if (observer.equals(null)) throw new NullPointerException("Null observer");
				if (observers.contains(observer)) observers.remove(observer);
				throw new IllegalArgumentException("Observer not registered");

			}

			@Nonnull @Override
			public ImmutableSet<Observer> getObservers() {
				return Optional.ofNullable(observers).orElse(ImmutableSet.of());
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
