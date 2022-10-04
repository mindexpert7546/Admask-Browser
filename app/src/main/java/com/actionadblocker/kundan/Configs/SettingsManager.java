package com.actionadblocker.kundan.Configs;

import android.os.Environment;

public class SettingsManager  {
    public static final String DOWNLOAD_FOLDER_DIR_NAME="KUNDAN";
    public static final String DOWNLOAD_FOLDER= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + DOWNLOAD_FOLDER_DIR_NAME+  "/";
}
