/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import android.annotation.Nullable;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Size;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.wifi.qrcode.QrCamera;
import com.android.settings.wifi.qrcode.QrDecorateView;

public class WifiDppQrCodeScannerFragment extends WifiDppQrCodeBaseFragment implements
        SurfaceTextureListener,
        QrCamera.ScannerCallback {
    private QrCamera mCamera;
    private TextureView mTextureView;
    private QrDecorateView mDecorateView;

    /** true if the fragment working for configurator, false enrollee*/
    private final boolean mConfiguratorMode;

    /** The SSID of the Wi-Fi network which the user specify to enroll */
    private String mSsid;

    @Override
    protected int getLayout() {
        return R.layout.wifi_dpp_qrcode_scanner_fragment;
    }

    @Override
    public int getMetricsCategory() {
        if (mConfiguratorMode) {
            return MetricsProto.MetricsEvent.SETTINGS_WIFI_DPP_CONFIGURATOR;
        } else {
            return MetricsProto.MetricsEvent.SETTINGS_WIFI_DPP_ENROLLEE;
        }
    }

    /**
     * Configurator container activity of the fragment should create instance with this constructor.
     */
    public WifiDppQrCodeScannerFragment() {
        super();

        mConfiguratorMode = true;
    }

    /**
     * Enrollee container activity of the fragment should create instance with this constructor and
     * specify the SSID string of the WI-Fi network to be provisioned.
     */
    public WifiDppQrCodeScannerFragment(String ssid) {
        super();

        mConfiguratorMode = false;
        mSsid = ssid;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHeaderIconImageResource(R.drawable.ic_scan_24dp);

        if (mConfiguratorMode) {
            setTitle(getString(R.string.wifi_dpp_add_device_to_network));

            WifiNetworkConfig wifiNetworkConfig = ((WifiNetworkConfig.Retriever) getActivity())
                .getWifiNetworkConfig();
            if (!WifiNetworkConfig.isValidConfig(wifiNetworkConfig)) {
                throw new IllegalArgumentException("Invalid Wi-Fi network for configuring");
            }
            setDescription(getString(R.string.wifi_dpp_center_qr_code, wifiNetworkConfig.getSsid()));
        } else {
            setTitle(getString(R.string.wifi_dpp_scan_qr_code));

            String description;
            if (TextUtils.isEmpty(mSsid)) {
                description = getString(R.string.wifi_dpp_scan_qr_code_join_unknown_network, mSsid);
            } else {
                description = getString(R.string.wifi_dpp_scan_qr_code_join_network, mSsid);
            }
            setDescription(description);
        }

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextureView = (TextureView) view.findViewById(R.id.preview_view);
        mTextureView.setSurfaceTextureListener(this);

        mDecorateView = (QrDecorateView) view.findViewById(R.id.decorate_view);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.removeItem(Menu.FIRST);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        initCamera(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Do nothing
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        destroyCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Do nothing
    }

    @Override
    public Size getViewSize() {
        return new Size(mTextureView.getWidth(), mTextureView.getHeight());
    }

    @Override
    public Rect getFramePosition(Size previewSize, int cameraOrientation) {
        return new Rect(0, 0, previewSize.getHeight(), previewSize.getHeight());
    }

    @Override
    public void setTransform(Matrix transform) {
        mTextureView.setTransform(transform);
    }

    @Override
    public void handleSuccessfulResult(String qrCode) {
        destroyCamera();
        mDecorateView.setFocused(true);
        // TODO(b/120243131): Add a network by Wi-Fi Network config shared via QR code.
    }

    @Override
    public void handleCameraFailure() {
        destroyCamera();
    }

    private void initCamera(SurfaceTexture surface) {
        // Check if the camera has already created.
        if (mCamera == null) {
            mCamera = new QrCamera(getContext(), this);
            mCamera.start(surface);
        }
    }

    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.stop();
            mCamera = null;
        }
    }
}