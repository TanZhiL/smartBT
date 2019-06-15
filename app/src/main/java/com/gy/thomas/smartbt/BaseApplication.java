package com.gy.thomas.smartbt;

import android.app.Application;
import android.content.Context;


import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class BaseApplication extends Application {
    private static final String TAG = "BaseApplication";
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        initApplication();
    }



    /**
     * 初始化App
     */
    private void initApplication() {

        mContext = getApplicationContext();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/lishu.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

    }


    public static Context getContext() {
        return mContext;
    }

}
