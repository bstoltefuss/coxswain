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
package svenmeier.coxswain.view;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by sven on 19.08.15.
 */
public class Utils {

    public static float dpToPx(Context context, int dp) {

        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static <T> void collect(Class<? extends T> t, View view, List<T> list) {
        if (t.isInstance(view)) {
            list.add((T) view);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(t, group.getChildAt(i), list);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getParent(Fragment fragment, Class<T> t) {
        while (true) {
            if (t.isInstance(fragment)) {
                return (T) fragment;
            }

            Fragment parentFragment = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                parentFragment = fragment.getParentFragment();
            }
            if (parentFragment == null) {
                break;
            }
            fragment = parentFragment;
        }

        Activity activity = fragment.getActivity();
        if (activity != null && t.isInstance(activity)) {
            return (T) activity;
        }

        throw new IllegalStateException("fragment does not have requested parent " + t.getSimpleName());
    }
}
