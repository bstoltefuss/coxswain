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
package svenmeier.coxswain.rower.water;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import svenmeier.coxswain.Application;

public class FileTrace implements ITrace {

    public static final String TRACE_FILE = "waterrower.trace";

    private final BufferedWriter writer;

    public FileTrace(Context context) throws IOException {
        File dir = Environment.getExternalStoragePublicDirectory(Application.TAG);
        dir.mkdirs();
        dir.setReadable(true, false);

        File file = new File(dir, TRACE_FILE);

        writer = new BufferedWriter(new FileWriter(file));

        // input media so file can be found via MTB
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

    }

    @Override
    public void comment(String string) {
        trace('#', string);
    }

    @Override
    public void onOutput(String string) {
        trace('>', string);
    }

    @Override
    public void onInput(String string) {
        trace('<', string);
    }

    private void trace(char prefix, String message) {
        try {
            writer.write(prefix);
            writer.write(message);
            writer.write('\n');
            writer.flush();
        } catch (IOException ignore) {
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException ignore) {
        }
    }
}
