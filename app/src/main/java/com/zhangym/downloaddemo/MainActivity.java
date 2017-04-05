package com.zhangym.downloaddemo;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private long downloadId;
    private static final String TAG = "zhangym@MainActivity";
    private MyReceiver mMyReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMyReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(mMyReceiver, filter);

        findViewById(R.id.btn_start_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AndPermission.hasPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    startDownload();
                } else {
                    AndPermission.with(MainActivity.this)
                            .requestCode(100)
                            .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission
                                    .READ_EXTERNAL_STORAGE)
                            .send();
                }
            }
        });
    }

    /**
     * 下载
     */
    private void startDownload() {
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Uri uri = Uri.parse("http://apk.hiapk.com/appdown/com.zhangym.search_tools");
        DownloadManager.Request request = new DownloadManager.Request(uri);
        // 设置在下载过程中跟下载结束后都显示通知栏
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // 设置在WIFI跟手机网络环境下下载
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request
                .NETWORK_MOBILE);
        // 设置下载位置为外部公共的下载目录，文件名为xiaoboshi.apk，并且可以被Media Scanner扫描到
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "xiaoboshi.apk");
        // 设置允许系统扫描到下载的文件
        request.allowScanningByMediaScanner();
        downloadId = manager.enqueue(request);
    }

    private PermissionListener mPermissionListener = new PermissionListener() {
        @Override
        public void onSucceed(int requestCode, List<String> grantPermissions) {
            startDownload();
        }

        @Override
        public void onFailed(int requestCode, List<String> deniedPermissions) {
            Toast.makeText(MainActivity.this, "未获取sd卡读写权限，无法下载！", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        AndPermission.onRequestPermissionsResult(requestCode, permissions, grantResults, mPermissionListener);
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == completeDownloadId) {
                if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                    installApk();
                }
            }
        }
    }

    /**
     * 下载完成后自动安装
     */
    private void installApk() {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "xiaoboshi.apk");
            if (file.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri;
                // 判断若版本大于23，则必须使用FileProvider的方式，不然下载完毕后不会自动安装
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    uri = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName()
                            + ".provider", file);
                } else {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    uri = Uri.fromFile(file);
                }
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "下载文件不完整！", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "安装失败");
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mMyReceiver);
    }
}
