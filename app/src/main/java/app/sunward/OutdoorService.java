package app.sunward;
import android.Manifest;
import android.app.Service;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;

public class OutdoorService extends Service implements LocationListener {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LocationManager manager;private Location latest;
    private final Runnable refresh=new Runnable(){public void run(){
        if(!Reminders.prefs(OutdoorService.this).getBoolean("active",false)){stopSelf();return;}
        if(latest!=null)Weather.fetch(OutdoorService.this,latest,null);
        handler.postDelayed(this,15*Rules.MINUTE);
    }};
    public static boolean locationAllowed(Context c){return c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(!Reminders.prefs(this).getBoolean("active",false)||!locationAllowed(this)){stopSelf();return START_NOT_STICKY;}
        Reminders.channels(this);startForeground(Reminders.ONGOING,Reminders.ongoing(this));
        manager=getSystemService(LocationManager.class);
        try {
            manager.removeUpdates(this);
            if(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,15*Rules.MINUTE,0,this);
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED && manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,15*Rules.MINUTE,0,this);
        }catch(SecurityException e){stopSelf();return START_NOT_STICKY;}
        handler.removeCallbacks(refresh);handler.post(refresh);return START_NOT_STICKY;
    }
    @Override public void onLocationChanged(Location l){latest=l;Weather.fetch(this,l,null);}
    @Override public void onProviderDisabled(String provider){Reminders.prefs(this).edit().putString("weatherError","Location is off. Turn it on to refresh local UV; your timer still works.").apply();}
    @Override public void onProviderEnabled(String provider){}
    @Override public void onStatusChanged(String provider,int status,Bundle extras){}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(manager!=null)manager.removeUpdates(this);super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
