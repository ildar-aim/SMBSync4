package com.ildar_aim.smbsync4;

import com.ildar_aim.smbsync4.ISvcCallback;

interface ISvcClient{
	
	void setCallBack(ISvcCallback callback);
	void removeCallBack(ISvcCallback callback);

	void aidlStartSpecificSyncTask(in String[] task_name) ;
	void aidlStartAutoSyncTask() ;
	
	void aidlStartSchedule(in String[] schedule_name_array) ;

	void aidlConfirmReply(int confirmed) ;
	
	void aidlStopService() ;
	
	void aidlSetActivityInBackground() ;
	void aidlSetActivityInForeground() ;
	
}