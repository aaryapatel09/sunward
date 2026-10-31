package app.sunward;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;

public class MainActivity extends Activity implements LocationListener {
    static final int INK=0xff111111, MUTED=0xff686868, WHITE=0xffffffff;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private android.content.SharedPreferences p;
    private LinearLayout content;
    private TextView timer,timerDetail,uv,weather,guidance;
    private Button profile,activity;
    private Button apply,end;
    private CountdownRing ring;
    private LinearLayout sessionActions;
    private TextView ringLabel;
    private LocationManager manager;private boolean locating=false;
    private final Runnable tick=new Runnable(){public void run(){update();handler.postDelayed(this,1000);}};
    private final Runnable locationTimeout=()->{if(locating){finishLocation();p.edit().putString("weatherError","No fresh location yet. Check Location in phone settings and try again; the timer works offline.").apply();update();}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);p=Reminders.prefs(this);Reminders.channels(this);
        manager=getSystemService(LocationManager.class);build();
    }
    @Override public void onResume(){super.onResume();handler.post(tick);
        if(p.getBoolean("active",false)){Reminders.schedule(this);startTracking();}
        if(OutdoorService.locationAllowed(this) && System.currentTimeMillis()-p.getLong("weatherTime",0)>15*Rules.MINUTE)locate();
    }
    @Override public void onPause(){super.onPause();handler.removeCallbacks(tick);finishLocation();}
    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    void space(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    Button button(String label,int color,int fg,Runnable click){
        Button b=new Button(this);b.setText(label);b.setTextSize(16);b.setTextColor(fg);b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(color==INK?0x44ffffff:0x16000000),bg(color,20),null));
        b.setStateListAnimator(null);b.setPadding(dp(16),dp(12),dp(16),dp(12));b.setMinHeight(dp(52));b.setOnClickListener(v->click.run());return b;
    }
    void fullButton(LinearLayout l,Button b){l.addView(b,new LinearLayout.LayoutParams(-1,-2));}
    TextView centered(String value,int size,int color){TextView t=text(value,size,color,false);t.setGravity(Gravity.CENTER);t.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);return t;}
    void flexibleSpace(){View v=new View(this);content.addView(v,new LinearLayout.LayoutParams(1,dp(24),1));}
    void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(WHITE);scroll.setVerticalScrollBarEnabled(false);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(24),dp(16),dp(24),dp(20));scroll.addView(content);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());}return insets;});
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.ic_sun_small);mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(mark,new LinearLayout.LayoutParams(dp(27),dp(27)));
        TextView title=text("Sunward",21,INK,true);title.setPadding(dp(10),0,0,0);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button more=button("More",WHITE,MUTED,()->showMenu());more.setTextSize(14);more.setMinWidth(dp(56));more.setContentDescription("More options and settings");header.addView(more);content.addView(header);
        space(content,24);
        LinearLayout conditions=new LinearLayout(this);conditions.setOrientation(LinearLayout.VERTICAL);conditions.setGravity(Gravity.CENTER);
        conditions.setPadding(dp(16),dp(14),dp(16),dp(14));conditions.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x11000000),bg(0xfff7f7f5,20),null));
        uv=centered("Local UV",17,INK);uv.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));conditions.addView(uv);space(conditions,3);
        weather=centered("Tap to use your location",13,MUTED);conditions.addView(weather);
        conditions.setOnClickListener(v->{if(p.getLong("weatherTime",0)==0)requestLocation();else showWeather();});conditions.setFocusable(true);conditions.setMinimumHeight(dp(72));
        content.addView(conditions,new LinearLayout.LayoutParams(-1,-2));
        flexibleSpace();
        FrameLayout clock=new FrameLayout(this);ring=new CountdownRing(this);clock.addView(ring,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout clockText=new LinearLayout(this);clockText.setOrientation(LinearLayout.VERTICAL);clockText.setGravity(Gravity.CENTER);
        ringLabel=centered("TIME OUTSIDE",10,MUTED);ringLabel.setLetterSpacing(.16f);clockText.addView(ringLabel);space(clockText,10);
        timer=centered("Ready?",60,INK);timer.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));timer.setFontFeatureSettings("tnum");timer.setIncludeFontPadding(false);
        timer.setMaxLines(1);timer.setAutoSizeTextTypeUniformWithConfiguration(24,64,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        clockText.addView(timer,new LinearLayout.LayoutParams(-1,dp(80)));
        FrameLayout.LayoutParams center=new FrameLayout.LayoutParams(dp(198),-2,Gravity.CENTER);clock.addView(clockText,center);
        content.addView(clock,new LinearLayout.LayoutParams(-1,dp(264)));
        space(content,10);timerDetail=centered("Apply sunscreen before heading out.",14,MUTED);content.addView(timerDetail);
        space(content,28);
        apply=button("I applied sunscreen",INK,WHITE,()->begin());apply.setMinHeight(dp(58));fullButton(content,apply);
        sessionActions=new LinearLayout(this);sessionActions.setGravity(Gravity.CENTER_VERTICAL);
        end=button("End session",WHITE,MUTED,()->{Reminders.stop(this);update();});end.setTextSize(13);
        activity=button("Activity",WHITE,MUTED,()->showActivity());activity.setTextSize(13);
        sessionActions.addView(end,new LinearLayout.LayoutParams(0,-2,1));sessionActions.addView(activity,new LinearLayout.LayoutParams(0,-2,1));content.addView(sessionActions);
        flexibleSpace();
        guidance=centered("",13,MUTED);guidance.setPadding(dp(8),dp(8),dp(8),dp(12));guidance.setOnClickListener(v->showSettings());guidance.setContentDescription("Reminder and weather status. Tap for settings.");content.addView(guidance);
        TextView label=text("YOUR SUNSCREEN",10,MUTED,true);label.setLetterSpacing(.12f);label.setPadding(dp(4),0,0,dp(8));content.addView(label);
        profile=button("Add sunscreen",0xfff7f7f5,INK,()->editProfile());profile.setTextSize(14);profile.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);profile.setMinHeight(dp(60));profile.setCompoundDrawablePadding(dp(12));
        android.graphics.drawable.Drawable bottle=getDrawable(R.drawable.ic_bottle);bottle.setBounds(0,0,dp(22),dp(28));profile.setCompoundDrawables(bottle,null,null,null);fullButton(content,profile);
        update();
    }
    void update(){
        if(timer==null)return;
        long now=System.currentTimeMillis();boolean active=p.getBoolean("active",false);long due=p.getLong("due",0);
        if(active){long remaining=Math.max(0,due-now),minutes=(remaining+59999)/60000;
            timer.setText(remaining==0?"Reapply now":String.format(Locale.getDefault(),"%d:%02d",minutes/60,minutes%60));
            timerDetail.setText(remaining==0?"Tap below once you’ve reapplied.":"Next application at "+DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(due)));
            ringLabel.setText(remaining==0?"TIME TO REAPPLY":"HOURS : MINUTES");
            ring.setRemaining((float)remaining/Math.max(1,due-p.getLong("applied",now)),true);
        }else{timer.setText("Ready?");timerDetail.setText("Apply sunscreen before heading out.");ringLabel.setText("TIME OUTSIDE");ring.setRemaining(0,false);}
        apply.setText(active?"I reapplied":"I applied sunscreen");sessionActions.setVisibility(active?View.VISIBLE:View.GONE);
        activity.setText(p.getBoolean("wet",false)?"Water / sweat · on":"Activity");
        String name=p.getString("name","Add sunscreen"),spf=p.getString("spf","");profile.setText(name+(spf.isEmpty()?"":" · SPF "+spf));
        long fetched=p.getLong("weatherTime",0);boolean stale=now-fetched>60*Rules.MINUTE || now-p.getLong("locationTime",0)>60*Rules.MINUTE;
        String error=p.getString("weatherError","");
        if(fetched>0){float value=p.getFloat("uv",0);uv.setText(String.format(Locale.getDefault(),"UV %.1f · %s",value,Rules.uvBand(value)));
            weather.setText(String.format(Locale.getDefault(),"%.0f°C · %s%s",p.getFloat("temperature",0),Weather.description(p.getInt("weatherCode",0)),stale?" · outdated":""));
        }
        String hint="";
        if(active&&!Reminders.notifications(this))hint="Notifications are off. Tap to enable.";
        else if(active&&!Reminders.exact(this))hint="Reminder timing is approximate. Tap to adjust.";
        else if(!error.isEmpty())hint=error.contains("…")?"Updating local weather…":"Weather unavailable. Your timer still works.";
        else if(fetched>0 && stale)hint="Weather is outdated. Refresh your location.";
        else if(fetched>0 && p.getFloat("uv",0)>=6)hint="High UV. Seek shade and cover up.";
        guidance.setText(hint);guidance.setVisibility(hint.isEmpty()?View.GONE:View.VISIBLE);
    }
    void showMenu(){
        new AlertDialog.Builder(this).setTitle("Sunward").setItems(new String[]{"Sunscreen","Weather details","Reminder settings","How it works & privacy"},(d,which)->{
            if(which==0)editProfile();else if(which==1)showWeather();else if(which==2)showSettings();else about();
        }).setNegativeButton("Close",null).show();
    }
    void showActivity(){
        String mode=p.getBoolean("wet",false)?"Finish swimming / sweating":"Start swimming / heavy sweating";
        new AlertDialog.Builder(this).setTitle("Activity").setItems(new String[]{mode,"I toweled off"},(d,which)->{if(which==0)toggleWet();else toweled();}).setNegativeButton("Cancel",null).show();
    }
    void showWeather(){
        long fetched=p.getLong("weatherTime",0);
        String detail=fetched==0?"No local forecast yet.":String.format(Locale.getDefault(),"%s\nUV %.1f · %s\n%.0f°C / %.0f°F · %s\n%d%% cloud cover\nUpdated %s\n\nHourly forecast estimate from Open-Meteo.",p.getString("place",""),p.getFloat("uv",0),Rules.uvBand(p.getFloat("uv",0)),p.getFloat("temperature",0),p.getFloat("temperature",0)*1.8+32,Weather.description(p.getInt("weatherCode",0)),p.getInt("cloud",0),DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(fetched)));
        if(fetched>0 && (System.currentTimeMillis()-fetched>60*Rules.MINUTE || System.currentTimeMillis()-p.getLong("locationTime",0)>60*Rules.MINUTE))detail+="\n\nOutdated reading. Refresh for current conditions.";
        String error=p.getString("weatherError","");if(!error.isEmpty())detail+="\n\n"+error;
        new AlertDialog.Builder(this).setTitle("Local weather").setMessage(detail).setPositiveButton("Refresh",(d,w)->requestLocation()).setNegativeButton("Close",null).show();
    }
    void showSettings(){
        new AlertDialog.Builder(this).setTitle("Reminder settings").setItems(new String[]{
            Reminders.notifications(this)?"Notifications · on":"Notifications · off",
            Reminders.exact(this)?"Precise timing · on":"Precise timing · off",
            OutdoorService.locationAllowed(this)?"Location · enabled":"Location · not enabled"
        },(d,which)->{
            if(which==0)enableNotifications();
            else if(which==1 && Build.VERSION.SDK_INT>=31)startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
            else if(which==2)requestLocation();
        }).setNegativeButton("Close",null).show();
    }
    void begin(){
        if(!Reminders.notifications(this)){new AlertDialog.Builder(this).setTitle("Enable your reminders").setMessage("Allow notifications, then tap the application button again to start your outdoor session.").setPositiveButton("Enable",(d,w)->enableNotifications()).setNegativeButton("Cancel",null).show();return;}
        if(p.getString("spf","").isEmpty()){editProfile();return;}
        Reminders.applied(this);startTracking();update();
        if(!OutdoorService.locationAllowed(this))new AlertDialog.Builder(this).setTitle("Timer started").setMessage("Enable location to add local UV and weather updates during your session. Your reminder also works without it.").setPositiveButton("Use location",(d,w)->requestLocation()).setNegativeButton("Later",null).show();
    }
    void startTracking(){if(OutdoorService.locationAllowed(this))try{startForegroundService(new Intent(this,OutdoorService.class));}catch(RuntimeException e){p.edit().putString("weatherError","Background weather tracking could not start. Refresh manually; your timer still works.").apply();}}
    void enableNotifications(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);
        else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
    }
    void requestLocation(){
        if(!OutdoorService.locationAllowed(this)){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},12);return;}locate();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==12 && OutdoorService.locationAllowed(this)){locate();if(p.getBoolean("active",false))startTracking();}update();}
    void locate(){
        if(locating)return;locating=true;p.edit().putString("weatherError","Finding your location…").apply();update();
        try{
            boolean enabled=false;
            if(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,this);enabled=true;}
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);enabled=true;}
            if(!enabled){finishLocation();p.edit().putString("weatherError","Turn on Location in your phone settings, then refresh.").apply();update();return;}
            handler.postDelayed(locationTimeout,25000);
        }catch(SecurityException e){finishLocation();}
    }
    void finishLocation(){if(manager!=null)manager.removeUpdates(this);locating=false;handler.removeCallbacks(locationTimeout);}
    @Override public void onLocationChanged(Location location){if(System.currentTimeMillis()-location.getTime()>5*Rules.MINUTE)return;finishLocation();p.edit().putString("weatherError","Updating your local forecast…").apply();Weather.fetch(this,location,()->{if(!isDestroyed())update();});}
    @Override public void onProviderEnabled(String provider){}
    @Override public void onProviderDisabled(String provider){}
    @Override public void onStatusChanged(String provider,int status,Bundle extra){}
    void toggleWet(){
        if(!p.getBoolean("active",false)){toast("Start an outdoor session first.");return;}
        if(p.getBoolean("wet",false)){toweled();return;}
        long now=System.currentTimeMillis();long due=Math.min(p.getLong("due",Long.MAX_VALUE),Rules.due(p.getLong("applied",now),now,p.getInt("water",0)));
        p.edit().putBoolean("wet",true).putLong("wetStarted",now).putLong("due",due).putBoolean("alerted",false).apply();Reminders.schedule(this);update();
    }
    void toweled(){
        if(!p.getBoolean("active",false)){toast("Start an outdoor session first.");return;}
        p.edit().putBoolean("wet",false).putLong("wetStarted",0).putLong("due",System.currentTimeMillis()).putBoolean("alerted",false).apply();Reminders.schedule(this);update();
    }
    void editProfile(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(24),dp(8),dp(24),dp(8));
        EditText name=new EditText(this);name.setHint("Sunscreen name");name.setSingleLine(true);name.setText(p.getString("name",""));box.addView(name);
        EditText spf=new EditText(this);spf.setHint("SPF on label (e.g. 50)");spf.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);spf.setText(p.getString("spf",""));box.addView(spf);
        CheckBox broad=new CheckBox(this);broad.setText("Label says broad spectrum (UVA + UVB)");broad.setChecked(p.getBoolean("broad",false));box.addView(broad);
        box.addView(text("Water resistance on the label",14,INK,true));
        Spinner water=new Spinner(this);water.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"None / not sure","40 minutes","80 minutes"}));water.setSelection(p.getInt("water",0)==40?1:p.getInt("water",0)==80?2:0);box.addView(water);
        box.addView(text("Use broad-spectrum SPF 30+ and follow your label. Saving a product never extends a running timer.",13,MUTED,false));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Your sunscreen").setView(box).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            int value;try{value=Integer.parseInt(spf.getText().toString().trim());}catch(Exception e){spf.setError("Enter the SPF on your bottle");return;}
            if(value<1||value>150){spf.setError("Enter a label SPF between 1 and 150");return;}
            String label=name.getText().toString().trim();if(label.isEmpty()){name.setError("Enter a sunscreen name");return;}
            int resistance=water.getSelectedItemPosition()==1?40:water.getSelectedItemPosition()==2?80:0;
            p.edit().putString("name",label).putString("spf",String.valueOf(value)).putBoolean("broad",broad.isChecked()).putInt("water",resistance).apply();
            if(p.getBoolean("active",false) && p.getBoolean("wet",false)){
                long revised=Rules.due(p.getLong("applied",0),p.getLong("wetStarted",0),resistance);
                p.edit().putLong("due",Math.min(p.getLong("due",0),revised)).putBoolean("alerted",false).apply();Reminders.schedule(this);
            }
            dialog.dismiss();update();if(value<30 || !broad.isChecked())new AlertDialog.Builder(this).setTitle("Check your protection").setMessage("AAD recommends broad-spectrum SPF 30 or higher. A higher SPF does not extend the reapplication interval.").setPositiveButton("Got it",null).show();
        }));dialog.show();
    }
    void about(){new AlertDialog.Builder(this).setTitle("Made for time outside").setMessage("Start after applying sunscreen. Reapply at least every 2 hours outdoors. Water/sweat mode uses your bottle’s 40- or 80-minute limit; no confirmed water resistance means reapply immediately. Towel drying or finishing swimming makes reapplication due now. Only confirming a new application resets the timer.\n\nThe circle shows time until your next reminder, not remaining skin protection. UV and weather are model estimates from Open-Meteo, not live skin measurements. High UV adds shade/clothing guidance; SPF and clouds never lengthen the timer.\n\nPrivacy: no account, ads or analytics. Your latest sunscreen and weather snapshot stay on this phone. Coordinates rounded to 2 decimals are sent to Open-Meteo over HTTPS; that provider also receives your IP address. Location is used only while the app is open or your outdoor session is running. End the session to stop tracking.\n\nAndroid may delay approximate alarms. Enable notifications and precise timing. After reboot, timers restore; open Sunward to restart location tracking. Force-stopping the app disables reminders until reopened.\n\nWeather: Open-Meteo (CC BY 4.0). Guidance: American Academy of Dermatology and FDA.")
        .setPositiveButton("Done",null).setNeutralButton("Guidance",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.aad.org/public/everyday-care/sun-protection/shade-clothing-sunscreen/how-to-apply-sunscreen"))))
        .setNegativeButton("Weather source",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://open-meteo.com/")))).show();}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
