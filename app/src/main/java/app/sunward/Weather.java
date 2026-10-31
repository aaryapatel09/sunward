package app.sunward;
import android.content.*;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class Weather {
    private static final ExecutorService executor=Executors.newSingleThreadExecutor();
    private static final AtomicBoolean busy=new AtomicBoolean();
    static void fetch(Context context,Location location,Runnable done){
        Context c=context.getApplicationContext();
        if(!busy.compareAndSet(false,true))return;
        executor.execute(()->{
            HttpURLConnection connection=null;
            try {
                if(System.currentTimeMillis()-location.getTime()>30*Rules.MINUTE)throw new IOException("Location is old. Refresh outdoors.");
                String coords=String.format(Locale.US,"latitude=%.2f&longitude=%.2f",location.getLatitude(),location.getLongitude());
                URL url=new URL("https://api.open-meteo.com/v1/forecast?"+coords+"&current=temperature_2m,weather_code,cloud_cover,is_day&hourly=uv_index&forecast_days=2&timeformat=unixtime&timezone=GMT");
                connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(12000);connection.setReadTimeout(12000);
                if(connection.getResponseCode()!=200)throw new IOException("Weather provider unavailable");
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(InputStream in=connection.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){bytes.write(buf,0,n);if(bytes.size()>1000000)throw new IOException("Invalid response");}}
                JSONObject root=new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
                JSONObject current=root.getJSONObject("current"),hourly=root.getJSONObject("hourly");
                JSONArray time=hourly.getJSONArray("time"),values=hourly.getJSONArray("uv_index");
                long[] times=new long[time.length()];for(int j=0;j<times.length;j++)times[j]=time.getLong(j);
                int index=Rules.nearestHour(times,System.currentTimeMillis()/1000);
                double uv=index<0||values.isNull(index)?Double.NaN:values.getDouble(index);
                if(!Double.isFinite(uv)||uv<0)throw new IOException("UV forecast unavailable");
                double temp=current.getDouble("temperature_2m");
                if(!Double.isFinite(temp))throw new IOException("Weather data unavailable");
                Reminders.prefs(c).edit().putFloat("uv",(float)uv).putFloat("temperature",(float)temp)
                    .putInt("cloud",current.getInt("cloud_cover")).putInt("weatherCode",current.getInt("weather_code"))
                    .putLong("weatherTime",System.currentTimeMillis()).putLong("uvTime",times[index]*1000)
                    .putLong("locationTime",location.getTime()).putString("place",String.format(Locale.US,"Near %.2f°, %.2f°",location.getLatitude(),location.getLongitude()))
                    .putString("weatherError","").apply();
                if(uv>=6 && Reminders.prefs(c).getBoolean("active",false) && Reminders.notifications(c)
                    && System.currentTimeMillis()-Reminders.prefs(c).getLong("uvAlert",0)>120*Rules.MINUTE){
                    c.getSystemService(android.app.NotificationManager.class).notify(Reminders.UV,
                    new android.app.Notification.Builder(c,"uv").setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(Rules.uvBand(uv)+" UV near you")
                    .setContentText("Seek shade and use protective clothing. Keep your reapplication timer.")
                    .setStyle(new android.app.Notification.BigTextStyle().bigText("Seek shade and use protective clothing. Keep your reapplication timer."))
                    .setContentIntent(Reminders.open(c)).setAutoCancel(true).build());
                    Reminders.prefs(c).edit().putLong("uvAlert",System.currentTimeMillis()).apply();
                }
            } catch(Exception e){Reminders.prefs(c).edit().putString("weatherError","Couldn’t update UV/weather. Check connection and location; your timer still works.").apply();}
            finally{if(connection!=null)connection.disconnect();busy.set(false);if(done!=null)new Handler(Looper.getMainLooper()).post(done);}
        });
    }
    static String description(int code){
        if(code==0)return "Clear";if(code<=3)return "Cloudy";if(code<=48)return "Fog";
        if(code<=67)return "Rain";if(code<=77)return "Snow";if(code<=82)return "Rain showers";
        if(code<=86)return "Snow showers";return "Thunderstorms";
    }
}
