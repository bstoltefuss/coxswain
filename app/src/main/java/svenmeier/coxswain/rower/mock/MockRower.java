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
package svenmeier.coxswain.rower.mock;

import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.rower.Rower;

/**
 */
public class MockRower implements Rower {

    private long resettedAt = 0;

    private double speed;

    private double strokes;

    private double energy;

    private final Snapshot memory;

    private boolean open;

    public MockRower(Snapshot memory) {
        this.memory = memory;
    }

    @Override
    public boolean open() {
        open = true;

        return true;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String getName() {
        return "Mockrower";
    }

    @Override
    public void reset() {
        resettedAt = System.currentTimeMillis();

        speed = 2.5 + Math.random();

        strokes = 0;
        energy = 0;
    }

    @Override
    public boolean row() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignore) {
        }

        if (open) {
            long now = System.currentTimeMillis();
            if (now > resettedAt + 2000) {
                // delay before achieving anything

                memory.distance.set((int)((now - resettedAt) / 1000 * speed));

                strokes += 0.04;
                memory.strokes.set((int)strokes);

                energy += 0.015;
                memory.energy.set((int)energy);
            }

            memory.speed.set((int)(speed * 100));

            memory.strokeRate.set((int)(26 +  (Math.random() * 3)));

            memory.strokeRatio.set((int)(10 +  (Math.random() * 5)));

            memory.pulse.set((int)(80 +  (Math.random() * 10)));

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        open = false;
    }
}
