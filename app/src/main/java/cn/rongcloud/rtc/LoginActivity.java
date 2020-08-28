package cn.rongcloud.rtc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import cn.rongcloud.rtc.api.RCRTCConfig;
import cn.rongcloud.rtc.api.RCRTCConfig.Builder;
import cn.rongcloud.rtc.api.RCRTCEngine;
import cn.rongcloud.rtc.api.RCRTCRoom;
import cn.rongcloud.rtc.api.callback.IRCRTCResultCallback;
import cn.rongcloud.rtc.api.callback.IRCRTCResultDataCallback;
import cn.rongcloud.rtc.api.stream.RCRTCVideoStreamConfig;
import cn.rongcloud.rtc.base.RCRTCParamsType.RCRTCVideoFps;
import cn.rongcloud.rtc.base.RCRTCParamsType.RCRTCVideoResolution;
import cn.rongcloud.rtc.base.RTCErrorCode;
import cn.rongcloud.rtc.base.RongRTCBaseActivity;
import cn.rongcloud.rtc.call.CallActivity;
import cn.rongcloud.rtc.message.RoomInfoMessage;
import cn.rongcloud.rtc.util.SessionManager;
import cn.rongcloud.rtc.util.UserUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.RongIMClient.ConnectionErrorCode;
import io.rong.imlib.RongIMClient.DatabaseOpenStatus;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends RongRTCBaseActivity {

    private String TAG = "LoginActivity";
    List<String> unGrantedPermissions;
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS", "android.permission.RECORD_AUDIO", "android.permission.INTERNET", "android.permission.CAMERA",
        "android.permission.READ_PHONE_STATE", Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, "android.permission.BLUETOOTH_ADMIN",
        "android.permission.BLUETOOTH"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_login); initIM(); checkPermissions();
    }

    private void initIM() {
        RongIMClient.init(getApplication(), UserUtils.APP_KEY, false);
    }

    private void start() {
        LoadDialog.show(this); if (RongIMClient.getInstance().getCurrentConnectionStatus() == RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) {
            joinRoom();
        } else {
            connectIM();
        }
    }

    private void connectIM() {
        RongIMClient.connect(UserUtils.USER_TOKEN_1, new RongIMClient.ConnectCallback() {
            @Override
            public void onDatabaseOpened(DatabaseOpenStatus code) {
                Log.e(TAG, "RongIMClient connect onDatabaseOpened");
            }

            @Override
            public void onSuccess(String s) {
                Log.e(TAG, "RongIMClient connect onSuccess s :" + s); runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        joinRoom();
                    }
                });
            }

            @Override
            public void onError(ConnectionErrorCode connectionErrorCode) {
                LoadDialog.dismiss(LoginActivity.this); Log.e(TAG, "RongIMClient connect errorCode :" + connectionErrorCode);
            }
        });
    }

    private void joinRoom() {
        initConfig(); RCRTCEngine.getInstance().joinRoom(UserUtils.ROOMID, new IRCRTCResultDataCallback<RCRTCRoom>() {
            @Override
            public void onSuccess(RCRTCRoom rcrtcRoom) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LoadDialog.dismiss(LoginActivity.this);
                        // 加入房间之前 置为默认状态
                        SessionManager.getInstance().put("VideoModeKey", "smooth"); Intent intent = new Intent(LoginActivity.this, CallActivity.class);
                        intent.putExtra(CallActivity.EXTRA_ROOMID, UserUtils.ROOMID); intent.putExtra(CallActivity.EXTRA_USER_NAME, UserUtils.USER_NAME_1);
                        intent.putExtra(CallActivity.EXTRA_CAMERA, false); intent.putExtra(CallActivity.EXTRA_OBSERVER, false); intent.putExtra(CallActivity.EXTRA_AUTO_TEST, false);
                        intent.putExtra(CallActivity.EXTRA_WATER, false); intent.putExtra(CallActivity.EXTRA_MIRROR, false); intent.putExtra(CallActivity.EXTRA_IS_LIVE, false);
                        RCRTCRoom room = RCRTCEngine.getInstance().getRoom(); String userId = room.getLocalUser().getUserId(); String userName = UserUtils.USER_NAME_1;
                        int remoteUserCount = room.getRemoteUsers() != null ? room.getRemoteUsers().size() : 0; intent.putExtra(CallActivity.EXTRA_IS_MASTER, remoteUserCount == 0);
                        int joinMode = RoomInfoMessage.JoinMode.AUDIO_VIDEO;
                        RoomInfoMessage roomInfoMessage = new RoomInfoMessage(userId, userName, joinMode, System.currentTimeMillis(), remoteUserCount == 0); JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("userId", userId); jsonObject.put("userName", userName); jsonObject.put("joinMode", joinMode); jsonObject.put("joinTime", System.currentTimeMillis());
                            jsonObject.put("master", remoteUserCount == 0 ? 1 : 0);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } room.setRoomAttributeValue(jsonObject.toString(), userId, roomInfoMessage, new IRCRTCResultCallback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onFailed(RTCErrorCode errorCode) {

                            }
                        }); startActivityForResult(intent, 1);
                    }
                });
            }

            @Override
            public void onFailed(RTCErrorCode rtcErrorCode) {
                Log.e(TAG, "RongIMClient joinRoom errorCode :" + rtcErrorCode.getReason()); runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LoadDialog.dismiss(LoginActivity.this);
                    }
                });
            }
        });
    }

    private void initConfig() {
        RCRTCConfig config = Builder.create()
            //是否硬解码
            .enableHardwareDecoder(true)
            //是否硬编码
            .enableHardwareEncoder(true).build(); RCRTCEngine.getInstance().init(getApplicationContext(), config);

        RCRTCVideoStreamConfig videoConfigBuilder = RCRTCVideoStreamConfig.Builder.create()
            //设置分辨率
            .setVideoResolution(RCRTCVideoResolution.RESOLUTION_480_640)
            //设置帧率
            .setVideoFps(RCRTCVideoFps.Fps_15)
            //设置最小码率，480P下推荐200
            .setMinRate(200)
            //设置最大码率，480P下推荐900
            .setMaxRate(900).build(); RCRTCEngine.getInstance().getDefaultVideoStream().setVideoConfig(videoConfigBuilder);
    }

    public void connectClick(View view) {
        start();
    }

    private void checkPermissions() {
        unGrantedPermissions = new ArrayList(); for (String permission : MANDATORY_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                unGrantedPermissions.add(permission);
            }
        } if (unGrantedPermissions.size() == 0) { // 已经获得了所有权限，开始加入聊天室

        } else { // 部分权限未获得，重新请求获取权限
            String[] array = new String[unGrantedPermissions.size()]; ActivityCompat.requestPermissions(this, unGrantedPermissions.toArray(array), 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        unGrantedPermissions.clear(); for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                unGrantedPermissions.add(permissions[i]);
            }
        } if (unGrantedPermissions.size() > 0) {
            for (String permission : unGrantedPermissions) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    Toast.makeText(this, getString(R.string.PermissionStr) + permission + getString(R.string.plsopenit), Toast.LENGTH_SHORT).show(); finish();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
                }
            }
        } else {
//            initSDK();
        }
    }
}