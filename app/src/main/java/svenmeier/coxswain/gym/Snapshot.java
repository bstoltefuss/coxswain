/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain.gym;

import propoid.core.Property;
import propoid.core.Propoid;

/**
 */
public class Snapshot extends Propoid {

    public final Property<Workout> workout = property();

    public final Property<Integer> distance = property();

    public final Property<Integer> strokes = property();

    public final Property<Integer> speed = property();

    public final Property<Integer> pulse = property();

    public final Property<Integer> strokeRate = property();

    public final Property<Integer> strokeRatio = property();

    public final Property<Integer> energy = property();

    public Snapshot() {
        clear();
    }

    public Snapshot(Snapshot snapshot) {
        distance.set(snapshot.distance.get());
        strokes.set(snapshot.strokes.get());
        speed.set(snapshot.speed.get());
        pulse.set(snapshot.pulse.get());
        strokeRate.set(snapshot.strokeRate.get());
        strokeRatio.set(snapshot.strokeRatio.get());
        energy.set(snapshot.energy.get());
    }

    public void clear() {
        distance.set(0);
        strokes.set(0);
        speed.set(0);
        pulse.set(0);
        strokeRate.set(0);
        strokeRatio.set(0);
        energy.set(0);
    }
}
