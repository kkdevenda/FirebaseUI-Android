package com.firebase.ui.auth.util.accountcheck;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

/**
 * Created by Official on 1/30/18.
 */

public final class ManualCheckUtils {
    private ManualCheckUtils(){throw new AssertionError("No instance for you!");}


    public static Task<Boolean> isAnExistingUser(
            final Context context,
            final String phoneNumber,
            final Class<? extends ManualCheckService> listener
            ){
        final ServiceConnection keepServiceAliveConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {}

            @Override
            public void onServiceDisconnected(ComponentName name) {}
        };
        bindService(context, listener, keepServiceAliveConnection);
        return  getCheckTask(context, listener, phoneNumber);
    }

    private static Task<Boolean> getCheckTask(Context context, Class<? extends ManualCheckService> listener, final String phoneNumber) {
        return getDataTask(context, listener, new CheckServiceConnection() {
            @Override
            protected Task<Boolean> getDataTask(ManualCheckService service) {
                return service.isAnExistingUser(phoneNumber);
            }
        });
    }

    private static Task<Boolean> getDataTask(final Context context,
                                          final Class<? extends ManualCheckService> listener,
                                          final CheckServiceConnection connection) {
        TaskCompletionSource<Boolean> task = new TaskCompletionSource<>();
        bindService(context, listener, connection.setTask(task));
        return task.getTask().continueWith(new Continuation<Boolean, Boolean>() {
            @Override
            public Boolean then(@NonNull Task<Boolean> task) {
                unbindService(context, connection);
                return task.getResult();
            }
        });
    }

    private static void bindService(Context context,
                                    Class<? extends ManualCheckService> listener,
                                    ServiceConnection connection) {
        Context appContext = context.getApplicationContext();
        appContext.bindService(
                new Intent(appContext, listener),
                connection,
                Context.BIND_AUTO_CREATE);
    }

    private static void unbindService(Context context, ServiceConnection connection) {
        context.getApplicationContext().unbindService(connection);
    }


    public static final class CheckBinder extends Binder{
        private final ManualCheckService mService;

        public CheckBinder(ManualCheckService manualCheckService){
            mService = manualCheckService;
        }

        public ManualCheckService getService(){return  mService;}
    }

    private abstract static class CheckServiceConnection implements ServiceConnection {
        protected TaskCompletionSource<Boolean> mTask;

        protected CheckServiceConnection setTask(TaskCompletionSource<Boolean> task) {
            mTask = task;
            return this;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Task<Boolean> task = getDataTask(((CheckBinder) service).getService());

            if (task == null) { task = Tasks.forResult(null); }

            task.continueWith(new Continuation<Boolean, Boolean>() {
                @Override
                public Boolean then(@NonNull Task<Boolean> task) {
                    mTask.setResult(null);
                    return null;
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTask.trySetException(new IllegalStateException("ManualCheckService disconnected"));
        }

        @Nullable
        protected abstract Task<Boolean> getDataTask(ManualCheckService service);
    }
}
