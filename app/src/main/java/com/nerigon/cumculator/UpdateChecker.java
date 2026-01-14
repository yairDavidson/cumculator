package com.nerigon.cumculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final Context context;
    private static final String VERSION_URL = "https://yairdavidson.github.io/APKs_Updates/version.json";
    public UpdateChecker(Context context) {
        this.context = context;
    }
    /** Call this method from your MainActivity (e.g. in onCreate) */
    public void checkForUpdates() {
        new CheckUpdateTask().execute(VERSION_URL);
    }
    /** Background task to fetch version.json */
    private class CheckUpdateTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream in = conn.getInputStream();
                    StringBuilder response = new StringBuilder();
                    int data;
                    while ((data = in.read()) != -1) {
                        response.append((char) data);
                    }
                    return new JSONObject(response.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null; // failed
        }
        @Override
        protected void onPostExecute(JSONObject result) {
            if (result == null) {
                Toast.makeText(context, "Update check failed1", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int latestVersionCode = result.getInt("versionCode");
                String apkUrl = result.getString("apkUrl");

                int currentVersionCode = getCurrentVersionCode();

                if (latestVersionCode > currentVersionCode) {
                    showUpdateDialog(apkUrl);
                }
            } catch (Exception e) {
                Toast.makeText(context, "Update check failed2", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /** Helper: get current app version code */
    private int getCurrentVersionCode() {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode; // or info.getLongVersionCode() on API 28+
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    /** Show dialog to update */
    private void showUpdateDialog(String apkUrl) {
        new AlertDialog.Builder(context)
                .setTitle("Update Available")
                .setMessage("A new version is available. Install it?")
                .setCancelable(false)
                .setPositiveButton("Update", (dialog, which) -> downloadAndInstallApk(apkUrl))
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void downloadAndInstallApk(String apkUrl) {
        new Thread(() -> {
            // Notify download started
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
            );

            try {
                // Download APK
                URL url = new URL(apkUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                File apkFile = new File(context.getExternalFilesDir(null), "update.apk");
                InputStream in = conn.getInputStream();
                FileOutputStream out = new FileOutputStream(apkFile);

                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.close();
                in.close();

                // Notify download finished
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Download finished!", Toast.LENGTH_SHORT).show()
                );

                // Check unknown apps permission (Android 8+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        !context.getPackageManager().canRequestPackageInstalls()) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        new AlertDialog.Builder((Activity) context)
                                .setTitle("Permission required")
                                .setMessage("You need to allow installation from unknown sources for this app. Tap OK to go to settings.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                            Uri.parse("package:" + context.getPackageName()));
                                    context.startActivity(intent);
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                .show();
                    });
                    return;
                }


                // Launch installer
                Uri apkUri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".provider",
                        apkFile);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, " " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }


}
