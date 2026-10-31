package app.sunward;
import android.app.*;
import android.content.*;
import android.os.Build;
import android.content.pm.PackageManager;

public final class Reminders {
    static final String DUE="app.sunward.DUE", APPLY="app.sunward.APPLY", STOP="app.sunward.STOP";
    static final int ALERT=21, ONGOING=22, UV=23;
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("sunward",Context.MODE_PRIVATE);}
    static void channels(Context c){
        NotificationManager n=c.getSystemService(NotificationManager.class);
        n.createNotificationChannel(new NotificationChannel("reminders","Reapplication reminders",NotificationManager.IMPORTANCE_HIGH));
        n.createNotificationChannel(new NotificationChannel("outdoors","Outdoor session",NotificationManager.IMPORTANCE_LOW));
        n.createNotificationChannel(new NotificationChannel("uv","High UV guidance",NotificationManager.IMPORTANCE_DEFAULT));
    }
    static boolean notifications(Context c){
        if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        NotificationManager n=c.getSystemService(NotificationManager.class);
        NotificationChannel ch=n.getNotificationChannel("reminders");
        return n.areNotificationsEnabled() && (ch==null || ch.getImportance()!=NotificationManager.IMPORTANCE_NONE);
    }
    static boolean exact(Context c){return Build.VERSION.SDK_INT<31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
    static PendingIntent action(Context c,String a){return PendingIntent.getBroadcast(c,a.hashCode(),new Intent(c,ReminderReceiver.class).setAction(a),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static PendingIntent open(Context c){return PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static void schedule(Context c){
        SharedPreferences p=prefs(c); if(!p.getBoolean("active",false))return;
        long due=p.getLong("due",0); if(due==0)return;
        AlarmManager a=c.getSystemService(AlarmManager.class);
        long at=Math.max(System.currentTimeMillis()+1000,due);
        try { if(exact(c))a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,action(c,DUE));
              else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,action(c,DUE)); }
        catch(SecurityException e){a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,action(c,DUE));}
    }
    static void applied(Context c){
        SharedPreferences p=prefs(c);long now=System.currentTimeMillis();
        // Do not assume swimming has ended when the wearer reapplies.
        long wet=p.getBoolean("wet",false)?now:0;
        p.edit().putBoolean("active",true).putLong("applied",now).putLong("wetStarted",wet)
            .putLong("due",Rules.due(now,wet,p.getInt("water",0))).putBoolean("alerted",false).commit();
        c.getSystemService(NotificationManager.class).cancel(ALERT);schedule(c);
    }
    static void stop(Context c){
        prefs(c).edit().putBoolean("active",false).putBoolean("wet",false).putLong("wetStarted",0).commit();
        c.getSystemService(AlarmManager.class).cancel(action(c,DUE));
        c.getSystemService(NotificationManager.class).cancelAll();c.stopService(new Intent(c,OutdoorService.class));
    }
    static Notification ongoing(Context c){
        return new Notification.Builder(c,"outdoors").setSmallIcon(R.drawable.ic_notification).setContentTitle("Sunward · outdoor session")
            .setContentText("Location & UV updates on. Tap to check your timer.").setContentIntent(open(c)).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"End session",action(c,STOP)).build()).build();
    }
    static void notifyDue(Context c){
        SharedPreferences p=prefs(c);if(!p.getBoolean("active",false)||p.getBoolean("alerted",false))return;
        if(p.getLong("due",0)>System.currentTimeMillis()+1500){schedule(c);return;}
        if(!notifications(c))return;
        String detail="Reapply now, then tap ‘I reapplied’ to start the next timer.";
        Notification n=new Notification.Builder(c,"reminders").setSmallIcon(R.drawable.ic_notification).setContentTitle("Time to reapply sunscreen")
            .setContentText(detail).setStyle(new Notification.BigTextStyle().bigText(detail)).setContentIntent(open(c))
            .setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER)
            .addAction(new Notification.Action.Builder(null,"I reapplied",action(c,APPLY)).build())
            .addAction(new Notification.Action.Builder(null,"End session",action(c,STOP)).build()).build();
        c.getSystemService(NotificationManager.class).notify(ALERT,n);
        p.edit().putBoolean("alerted",true).commit();
    }
}
