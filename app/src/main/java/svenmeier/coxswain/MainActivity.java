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
package svenmeier.coxswain;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.view.ProgramsFragment;
import svenmeier.coxswain.view.PerformanceFragment;
import svenmeier.coxswain.view.WorkoutsFragment;


public class MainActivity extends AbstractActivity {

    public static String TAG = "coxswain";

    private Gym gym;

    private ViewPager pager;

    private ViewGroup programView;

    private TextView programNameView;

    private TextView programCurrentView;

    private Gym.Listener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(this);

        setContentView(R.layout.layout_main);

        checkUsbDevice(getIntent());

        pager = (ViewPager) findViewById(R.id.main_pager);
        pager.setAdapter(new MainAdapter(getFragmentManager()));

        programView = (ViewGroup) findViewById(R.id.main_program);
        programView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorkoutActivity.restart(MainActivity.this);
            }
        });

        programNameView = (TextView) findViewById(R.id.main_program_name);
        programCurrentView = (TextView) findViewById(R.id.main_program_current);

        findViewById(R.id.main_current_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Gym.instance(MainActivity.this).deselect();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (gym.current != null && gym.progress == null) {
            // workout is still running due to "open end", time to stop it now

            gym.deselect();
        }

        listener = new Gym.Listener() {
            @Override
            public void changed() {
                updateProgram();
            }
        };
        listener.changed();
        gym.addListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        gym.removeListener(listener);
    }

    private void updateProgram() {
        Program program = gym.program;
        if (program == null) {
            programView.setVisibility(View.GONE);
            programView.setEnabled(false);
        } else {
            programView.setVisibility(View.VISIBLE);
            programView.setEnabled(true);

            programNameView.setText(gym.program.name.get());

            String description = getString(R.string.gym_ready);
            if (gym.progress != null) {
                description = gym.progress.describe();
            }
            programCurrentView.setText(description);
        }
    }

    /**
     * Check whether the intent contains a {@link UsbDevice}, and pass it to {@link GymService}.
     *
     * @param intent possible USB device connect
     */
    private void checkUsbDevice(Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            GymService.start(this, device);

            // removeBinding device so that a successive orientation changes
            // do not trigger repeated service starts
            intent.removeExtra(UsbManager.EXTRA_DEVICE);

            if (gym.program == null) {
                // try to unlock device - has no effect if this activity is already running :/
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            } else {
                // program is already selected so restart workout
                WorkoutActivity.start(this);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        checkUsbDevice(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_programs, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (BuildConfig.DEBUG == false) {
            menu.findItem(R.id.action_mock).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_mock) {
            GymService.start(this, null);

            return true;
        } else if (id == R.id.action_settings) {
            startActivity(SettingsActivity.createIntent(this));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class MainAdapter extends FragmentStatePagerAdapter {

        public MainAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return getString(R.string.programs);
            } else if (position == 1) {
                return getString(R.string.workouts);
            } else {
                return getString(R.string.performance);
            }
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new ProgramsFragment();
            } else if (position == 1) {
                return new WorkoutsFragment();
            } else {
                return new PerformanceFragment();
            }
        }
    }
}
