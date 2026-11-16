package com.kraftplay.openiptv.auto;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

public class OpenIptvCarService extends CarActivityService {
    @Override
    public Class<? extends CarActivity> getCarActivity() {
        return OpenIptvCarActivity.class;
    }
}
