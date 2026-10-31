package app.sunward;
import android.content.*;
public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        Reminders.channels(c);String a=i.getAction();
        if(Reminders.STOP.equals(a))Reminders.stop(c);
        else if(Reminders.APPLY.equals(a))Reminders.applied(c);
        else if(Reminders.DUE.equals(a))Reminders.notifyDue(c);
        else if(Reminders.prefs(c).getBoolean("active",false)){
            if(Reminders.prefs(c).getLong("due",0)<=System.currentTimeMillis())Reminders.notifyDue(c);
            else Reminders.schedule(c);
        }
    }
}
