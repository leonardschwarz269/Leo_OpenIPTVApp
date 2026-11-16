package com.kraftplay.openiptv.auto;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

public class OpenIptvMirrorService extends CarActivityService {
    @Override
    public Class<? extends CarActivity> getCarActivity() {
        return OpenIptvMirrorActivity.class;
    }
}
