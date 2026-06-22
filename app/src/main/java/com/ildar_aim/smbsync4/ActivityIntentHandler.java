package com.ildar_aim.smbsync4;
/*
The MIT License (MIT)
Copyright (c) 2020 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ildar_aim.smbsync4.Constants.NAME_LIST_SEPARATOR;
import static com.ildar_aim.smbsync4.Constants.QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE;
import static com.ildar_aim.smbsync4.Constants.QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_ALL;
import static com.ildar_aim.smbsync4.Constants.QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_AUTO;
import static com.ildar_aim.smbsync4.Constants.QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_MANUAL;
import static com.ildar_aim.smbsync4.Constants.QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_TEST;
import static com.ildar_aim.smbsync4.Constants.QUERY_SYNC_TASK_INTENT;
import static com.ildar_aim.smbsync4.Constants.REPLY_SYNC_TASK_EXTRA_PARM_SYNC_ARRAY;
import static com.ildar_aim.smbsync4.Constants.REPLY_SYNC_TASK_EXTRA_PARM_SYNC_COUNT;
import static com.ildar_aim.smbsync4.Constants.REPLY_SYNC_TASK_INTENT;
import static com.ildar_aim.smbsync4.Constants.START_SYNC_EXTRA_PARM_SYNC_TASK;

public class ActivityIntentHandler extends Activity {
    private static final Logger log= LoggerFactory.getLogger(ActivityIntentHandler.class);
    private GlobalParameters mGp=null;
    private CommonUtilities mUtil = null;
    private Context c;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(GlobalParameters.setNewLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_transrucent);
        c=ActivityIntentHandler.this;

        if (mGp == null) {
            mGp =GlobalWorkArea.getGlobalParameter(c);
        }
        if (mUtil == null) mUtil = mUtil = new CommonUtilities(c, "IntentHandler", mGp, null);

        mGp.loadConfigList(c, mUtil);

        final Intent received_intent=getIntent();
        if (received_intent.getAction()!=null && !received_intent.getAction().equals("")) {
            if (received_intent.getAction().equals(QUERY_SYNC_TASK_INTENT)) {
                querySyncTask(c, received_intent);
                finish();
            } else {
                String action=received_intent.getAction();
                // This handler is exported, so ANY app can trigger a stored (possibly destructive
                // Mirror/Move) task. We keep the automation feature working but make every external
                // trigger auditable in the log so an unexpected sync can be traced to its caller.
                String caller=getCallingPackage();
                if (caller==null && getReferrer()!=null) caller=getReferrer().getHost();
                mUtil.addLogMsg("I", "", "External sync trigger action="+action+", caller="+caller);
                String task_list=received_intent.getStringExtra(START_SYNC_EXTRA_PARM_SYNC_TASK);
                if (task_list==null) {
                    SyncWorker.startSyncWorkerByAction(c, mGp, mUtil, action, "", "");
                } else {
                    SyncWorker.startSyncWorkerByAction(c, mGp, mUtil, action, "", task_list);
                }
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUtil.addDebugMsg(1, "I", CommonUtilities.getExecutedMethodName() + " entered");

        mUtil.flushLog();
        CommonUtilities.saveMessageList(c, mGp);
        System.gc();
//		android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void querySyncTask(Context c, Intent in) {
        String reply_list = "", sep = "";
        String task_type = QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_AUTO;
        if (in.getStringExtra(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE) != null)
            task_type = in.getStringExtra(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE);
        mUtil.addDebugMsg(1, "I", "extra=" + in.getExtras() + ", str=" + in.getStringExtra(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE));
        int reply_count = 0;
        if (mGp.syncTaskList.size() > 0) {
            for (int i = 0; i < mGp.syncTaskList.size(); i++) {
                SyncTaskItem sti = mGp.syncTaskList.get(i);
                if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_TEST.toLowerCase())) {
                    if (sti.isSyncTestMode()) {
                        reply_list += sep + sti.getSyncTaskName();
                        sep = NAME_LIST_SEPARATOR;
                        reply_count++;
                    }
                } else if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_AUTO.toLowerCase())) {
                    if (sti.isSyncTaskAuto()) {
                        reply_list += sep + sti.getSyncTaskName();
                        sep = NAME_LIST_SEPARATOR;
                        reply_count++;
                    }
                } else if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_MANUAL.toLowerCase())) {
                    if (!sti.isSyncTaskAuto()) {
                        reply_list += sep + sti.getSyncTaskName();
                        sep = NAME_LIST_SEPARATOR;
                        reply_count++;
                    }
                } else if (task_type.toLowerCase().equals(QUERY_SYNC_TASK_EXTRA_PARM_TASK_TYPE_ALL.toLowerCase())) {
                    reply_list += sep + sti.getSyncTaskName();
                    sep = NAME_LIST_SEPARATOR;
                    reply_count++;
                }
            }
        }
        Intent reply = new Intent(REPLY_SYNC_TASK_INTENT);
        reply.putExtra(REPLY_SYNC_TASK_EXTRA_PARM_SYNC_COUNT, reply_count);
        reply.putExtra(REPLY_SYNC_TASK_EXTRA_PARM_SYNC_ARRAY, reply_list);
        mUtil.addDebugMsg(1, "I", "query result, count="+reply_count+", list=["+reply_list+"]");
        // Scope the reply to the requesting app when we can identify it. A global broadcast leaks
        // the user's task names to ANY app registered for REPLY_SYNC_TASK_INTENT (which is exactly
        // what an attacker needs to then trigger a stored destructive task). Falls back to the
        // legacy unscoped broadcast only when the caller cannot be determined (backward compat).
        String target_pkg=getCallingPackage();
        if (target_pkg==null && getReferrer()!=null) target_pkg=getReferrer().getHost();
        if (target_pkg!=null && !target_pkg.equals("")) {
            reply.setPackage(target_pkg);
            mUtil.addDebugMsg(1, "I", "query reply scoped to caller="+target_pkg);
        } else {
            mUtil.addDebugMsg(1, "W", "query reply caller unknown; sending unscoped (legacy) broadcast");
        }
        c.sendBroadcast(reply);
    }

}
