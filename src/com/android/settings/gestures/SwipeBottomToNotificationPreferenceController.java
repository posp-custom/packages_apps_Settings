/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures;


import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Handles swipe bottom to expand notification panel gesture.
 **/
public class SwipeBottomToNotificationPreferenceController extends TogglePreferenceController {

    private static final String PREF_KEY = "gesture_swipe_bottom_to_notification";

    public SwipeBottomToNotificationPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return OneHandedSettingsUtils.isSupportOneHandedMode() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            OneHandedSettingsUtils.setSettingsOneHandedModeEnabled(mContext, false);
        }
        OneHandedSettingsUtils.setSwipeDownNotificationEnabled(mContext, isChecked);
        return true;
    }

    @Override
    public boolean isChecked() {
        return OneHandedSettingsUtils.isSwipeDownNotificationEnabled(mContext);
    }

    @Override
    public CharSequence getSummary() {
        // This toggle preference summary will be updated in gesture preference page since we bound
        // it with entry preference in gesture.xml
        return mContext.getText(
                isChecked() ? R.string.gesture_setting_on : R.string.gesture_setting_off);
    }
}