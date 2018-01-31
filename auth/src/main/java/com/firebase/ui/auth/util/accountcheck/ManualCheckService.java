package com.firebase.ui.auth.util.accountcheck;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.Task;

/**
 * Created by Official on 1/30/18.
 */

public abstract class ManualCheckService extends Service {
    private final IBinder mBinder = new ManualCheckUtils.CheckBinder(this);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public abstract Task<Boolean>isAnExistingUser(String phoneNumber);
}
