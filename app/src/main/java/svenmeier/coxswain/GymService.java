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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;

import propoid.db.Where;
import propoid.util.content.Preference;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.motivator.DefaultMotivator;
import svenmeier.coxswain.motivator.Motivator;
import svenmeier.coxswain.rower.Rower;
import svenmeier.coxswain.rower.mock.MockRower;
import svenmeier.coxswain.rower.water.WaterRower;

public class GymService extends Service {

    public static final String ACTION_STOP = "svenmeier.coxswain.GYM_STOP";

    private BroadcastReceiver receiver;

    private Gym gym;

    private Handler handler = new Handler();

    private Snapshot memory = new Snapshot();

    private Preference<Boolean> headsup;

    private Preference<Boolean> openEnd;

    private Rowing rowing;

    private Foreground foreground = new Foreground();

    public GymService() {
    }

    @Override
    public void onCreate() {
        gym = Gym.instance(this);

        openEnd = Preference.getBoolean(this, R.string.preference_open_end);
        headsup = Preference.getBoolean(this, R.string.preference_integration_headsup);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (ACTION_STOP.equals(action)) {
                    Gym.instance(GymService.this).deselect();
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(ACTION_STOP));
    }

    @Override
    public void onDestroy() {
        if (this.rowing != null) {
            endRowing();
        }

        unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (this.rowing != null) {
            endRowing();
            if (device == null) {
                return START_NOT_STICKY;
            }
        }

        startRowing(device);

        return START_NOT_STICKY;
    }

    private void startRowing(UsbDevice device) {

        memory.clear();

        Rower rower;
        if (device == null) {
            rower = new MockRower(memory);
        } else {
            rower = new WaterRower(this, memory, device);
        }

        rowing = new Rowing(rower);
        new Thread(rowing).start();
    }

    private void endRowing() {
        this.rowing = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Current rowing on rower.
     */
    private class Rowing implements Runnable {

        private final Rower rower;

        private Heart heart;

        private final Motivator motivator;

        private Program program;

        private long headsupSince;

        public Rowing(Rower rower) {
            this.rower = rower;

            this.heart = Heart.create(GymService.this, memory);

            this.motivator = new DefaultMotivator(GymService.this);
        }

        public void run() {
            if (rower.open()) {
                while (true) {
                    if (gym.program != program) {
                        // program changed
                        program = gym.program;

                        memory.clear();
                        rower.reset();
                    }

                    if (GymService.this.rowing != this|| rower.row() == false) {
                        break;
                    }

                    heart.pulse();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (GymService.this.rowing != Rowing.this) {
                                // no longer current
                                return;
                            }

                            if (gym.program ==  null) {
                                foreground.connected(String.format(getString(R.string.gym_notification_connected), rower.getName()));
                                return;
                            } else if (gym.program != program) {
                                // program changed
                                return;
                            }

                            String text = program.name.get();
                            float completion = 0;
                            if (gym.progress != null) {
                                text += " - " +  gym.progress.describe();
                                completion = gym.progress.completion();
                            }
                            foreground.workout(text, completion);

                            Event event = gym.addSnapshot(new Snapshot(memory));
                            motivator.onEvent(event);

                            if (event == Event.PROGRAM_FINISHED && openEnd.get() == false) {
                                gym.deselect();
                            }
                        }
                    });
                }

                rower.close();
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    motivator.destroy();

                    heart.destroy();

                    foreground.stop();
                }
            });
        }

    }

    private class Foreground {

        private String text;

        private int progress = -1;

        private long headsupSince;

        private void connected(String text) {
            GymService service = GymService.this;

            if (text.equals(this.text)) {
                return;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(service, 1, new Intent(service, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder builder = new Notification.Builder(service)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentIntent(pendingIntent);

            builder.setDefaults(Notification.DEFAULT_VIBRATE);
            builder.setContentText(text);

            Notification notification = builder.build();
            startForeground(1, notification);

            this.text = text;
            this.progress = -1;
        }

        private void workout(String text, float completion) {
            GymService service = GymService.this;

            int progress = (int)(completion * 100);

            if (text.equals(this.text) && progress == this.progress) {
                return;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(service, 1, new Intent(service, WorkoutActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder builder = new Notification.Builder(service)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true);

            if (text.equals(this.text)) {
                // no vibration, but needs empty array to keep headsup
                builder.setDefaults(0);
                builder.setVibrate(new long[0]);
            } else {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            }

            builder.setContentText(text);
            builder.setProgress(100, progress, false);

            if (headsUp()) {
                builder.setPriority(Notification.PRIORITY_HIGH);
            } else {
                builder.setPriority(Notification.PRIORITY_DEFAULT);
                builder.addAction(R.drawable.ic_close_black_24dp, getString(R.string.gym_notification_stop),
                        PendingIntent.getBroadcast(service, 0, new Intent(ACTION_STOP), 0));

            }

            Notification notification = builder.build();

            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(1, notification);

            this.text = text;
            this.progress = progress;
        }

        private boolean headsUp() {
            if (headsup.get()) {
                if (gym.hasListener(Object.class)) {
                    headsupSince = 0;
                } else {
                    if (headsupSince == 0) {
                        headsupSince = System.currentTimeMillis();
                    } else {
                        if (System.currentTimeMillis() - headsupSince > 2000) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        public void stop() {
            text = null;
            progress = -1;

            stopForeground(true);
        }
    }

    public static void start(Context context, UsbDevice device) {
        Intent serviceIntent = new Intent(context, GymService.class);

        if (device != null) {
            serviceIntent.putExtra(UsbManager.EXTRA_DEVICE, device);
        }

        context.startService(serviceIntent);
    }
}
