package app.sunward;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.android.gms.common.moduleinstall.*;
import android.os.Handler;
import android.os.Looper;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import java.util.concurrent.*;

/** One disposable editor: cancel/dismiss never persists a product or resets the timer. */
final class SunscreenEditor {
    private final MainActivity owner;
    private final android.content.SharedPreferences prefs;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private AlertDialog dialog,child;
    private EditText name,spf;
    private Spinner water;
    private CheckBox broad,confirmed;
    private TextView message;
    private Button scan,enter;
    private boolean closed=false,busy=false;
    private int generation=0;
    private String selectedCode="";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ModuleInstallClient moduleClient;
    private InstallStatusListener installListener;
    private int scanAttempt=0;
    private boolean cameraOpen=false;
    private final Runnable moduleTimeout=()->scannerFailure("Scanner download is taking too long. Try again on a stable connection, or enter the barcode number.");
    SunscreenEditor(MainActivity owner){this.owner=owner;prefs=Reminders.prefs(owner);}
    void show(){
        LinearLayout box=new LinearLayout(owner);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(owner.dp(24),owner.dp(8),owner.dp(24),owner.dp(16));
        scan=owner.button("Scan barcode",0xfff2f2f0,MainActivity.INK,()->scan());owner.fullButton(box,scan);
        enter=owner.button("Enter barcode number",MainActivity.WHITE,MainActivity.MUTED,()->enterCode());enter.setTextSize(13);owner.fullButton(box,enter);
        message=owner.text("Scan to look up a product, or enter your details below.",13,MainActivity.MUTED,false);box.addView(message);owner.space(box,12);
        name=new EditText(owner);name.setHint("Sunscreen name");name.setSingleLine(true);name.setText(prefs.getString("name",""));box.addView(name);
        spf=new EditText(owner);spf.setHint("SPF on label (e.g. 50)");spf.setInputType(InputType.TYPE_CLASS_NUMBER);spf.setText(prefs.getString("spf",""));box.addView(spf);
        broad=new CheckBox(owner);broad.setText("Broad spectrum (UVA + UVB) on label");broad.setChecked(prefs.getBoolean("broad",false));box.addView(broad);
        box.addView(owner.text("Water resistance on label",14,MainActivity.INK,true));
        water=new Spinner(owner);water.setAdapter(new ArrayAdapter<String>(owner,android.R.layout.simple_spinner_dropdown_item,new String[]{"None / not sure","40 minutes","80 minutes"}));water.setSelection(prefs.getInt("water",0)==40?1:prefs.getInt("water",0)==80?2:0);box.addView(water);
        confirmed=new CheckBox(owner);confirmed.setText("I checked these details against my sunscreen label");confirmed.setVisibility(View.GONE);box.addView(confirmed);
        owner.space(box,12);box.addView(owner.text("SPF 30+ and broad-spectrum coverage recommended. Saving never extends a running timer.",12,MainActivity.MUTED,false));
        Button source=owner.button("Product data · Open Beauty Facts",MainActivity.WHITE,MainActivity.MUTED,()->owner.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://world.openbeautyfacts.org"))));source.setTextSize(12);owner.fullButton(box,source);
        ScrollView scroll=new ScrollView(owner);scroll.addView(box);
        dialog=new AlertDialog.Builder(owner).setTitle("Your sunscreen").setView(scroll).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        dialog.setOnDismissListener(d->close());
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->save()));dialog.show();
    }
    private boolean alive(){return !closed&&!owner.isFinishing()&&!owner.isDestroyed();}
    private void busy(boolean value){busy=value;if(!alive())return;scan.setEnabled(!value);enter.setEnabled(!value);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!value);}
    private void scan(){
        if(busy)return;
        if(!prefs.getBoolean("barcodeLookupConsent",false)){
            child=new AlertDialog.Builder(owner).setTitle("Scan your sunscreen").setMessage("Google’s scanner reads the barcode on your phone. The barcode number is then sent to Open Beauty Facts to find the product; the service also sees your IP address. Camera images aren’t sent to that database.\n\nThe scanner may need a one-time Google Play services download. You’ll review the match before saving.")
                .setPositiveButton("Continue",(d,w)->{prefs.edit().putBoolean("barcodeLookupConsent",true).apply();launchScanner();}).setNegativeButton("Cancel",null).show();return;
        }
        launchScanner();
    }
    private void launchScanner(){
        if(!alive())return;busy(true);message.setText("Opening scanner…");
        GmsBarcodeScannerOptions options=new GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13,Barcode.FORMAT_EAN_8,Barcode.FORMAT_UPC_A,Barcode.FORMAT_UPC_E,Barcode.FORMAT_ITF).enableAutoZoom().build();
        final GmsBarcodeScanner scanner=GmsBarcodeScanning.getClient(owner,options);
        final int attempt=++scanAttempt;cameraOpen=false;
        try{
            moduleClient=ModuleInstall.getClient(owner);
            handler.postDelayed(moduleTimeout,60000);
            moduleClient.areModulesAvailable(scanner).addOnSuccessListener(result->{
                if(!alive()||attempt!=scanAttempt)return;
                if(result.areModulesAvailable()){openCamera(scanner,attempt);return;}
                message.setText("Downloading the scanner for first use…");
                installListener=update->{
                    if(!alive()||attempt!=scanAttempt)return;
                    int state=update.getInstallState();
                    if(state==ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED)openCamera(scanner,attempt);
                    else if(state==ModuleInstallStatusUpdate.InstallState.STATE_FAILED||state==ModuleInstallStatusUpdate.InstallState.STATE_CANCELED)
                        scannerFailure("Scanner download failed. Check your connection or enter the barcode number.");
                };
                moduleClient.installModules(ModuleInstallRequest.newBuilder().addApi(scanner).setListener(installListener).build())
                    .addOnSuccessListener(response->{if(alive()&&attempt==scanAttempt&&response.areModulesAlreadyInstalled())openCamera(scanner,attempt);})
                    .addOnFailureListener(error->{if(attempt==scanAttempt)scannerFailure("Scanner download unavailable. Update Google Play services or enter the barcode number.");});
            }).addOnFailureListener(error->{if(attempt==scanAttempt)scannerFailure("Scanner unavailable. Update Google Play services or enter the barcode number.");});
        }catch(RuntimeException e){scannerFailure("Scanner unavailable on this device. Enter the barcode number instead.");}
    }
    private void clearModuleListener(){
        handler.removeCallbacks(moduleTimeout);
        if(moduleClient!=null&&installListener!=null){moduleClient.unregisterListener(installListener);installListener=null;}
    }
    private void scannerFailure(String text){
        scanAttempt++;clearModuleListener();if(alive()){busy(false);message.setText(text);}
    }
    private void openCamera(GmsBarcodeScanner scanner,int attempt){
        if(!alive()||attempt!=scanAttempt||cameraOpen)return;cameraOpen=true;clearModuleListener();message.setText("Opening scanner…");
        scanner.startScan()
            .addOnSuccessListener(result->{if(!alive()||attempt!=scanAttempt)return;busy(false);String code=ProductRules.cleanCode(result.getRawValue());if(result.getFormat()==Barcode.FORMAT_UPC_E)code=ProductRules.expandUpce(code);lookup(code);})
            .addOnCanceledListener(()->{if(alive()&&attempt==scanAttempt){busy(false);message.setText("Scan canceled. Your details haven’t changed.");}})
            .addOnFailureListener(error->{if(attempt==scanAttempt)scannerFailure("Couldn’t open the scanner. Try again, or enter the barcode number.");});
    }
    private void enterCode(){
        EditText input=new EditText(owner);input.setHint("Barcode digits");input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setSingleLine(true);
        LinearLayout box=new LinearLayout(owner);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(owner.dp(24),0,owner.dp(24),0);
        box.addView(owner.text("The number will be sent to Open Beauty Facts for lookup.",13,MainActivity.MUTED,false));box.addView(input);
        child=new AlertDialog.Builder(owner).setTitle("Enter barcode").setView(box).setPositiveButton("Look up",null).setNegativeButton("Cancel",null).create();
        child.setOnShowListener(d->child.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String code=ProductRules.cleanCode(input.getText().toString());if(!ProductRules.validCode(code)){input.setError("Check the full 8, 12, 13 or 14 digits, including the last digit");return;}child.dismiss();lookup(code);
        }));child.show();
    }
    private void lookup(String code){
        if(!alive())return;
        if(!ProductRules.validCode(code)){message.setText("This isn’t a valid product barcode. Try the UPC/EAN barcode on your bottle, or enter details manually.");return;}
        long now=SystemClock.elapsedRealtime(),last=prefs.getLong("lastProductLookupElapsed",-10000);
        if(now>=last && now-last<5000){message.setText("Please wait a few seconds before another lookup.");return;}
        prefs.edit().putLong("lastProductLookupElapsed",now).apply();busy(true);message.setText("Looking up "+code+"…");final int request=++generation;
        executor.execute(()->{
            try{ProductLookup.Product product=ProductLookup.fetch(code);owner.runOnUiThread(()->{if(!alive()||request!=generation)return;busy(false);review(product);});}
            catch(ProductLookup.LookupException error){owner.runOnUiThread(()->{if(!alive()||request!=generation)return;busy(false);message.setText(error.getMessage());});}
        });
    }
    private void review(ProductLookup.Product product){
        message.setText("Match found. Check that it is your sunscreen.");
        String detail=product.displayName()+"\n\nBarcode: "+product.code+"\n\nOnly use this match if it is your sunscreen. Product records can be incomplete. Check SPF, broad-spectrum coverage and water resistance against your bottle.";
        child=new AlertDialog.Builder(owner).setTitle("Is this your sunscreen?").setMessage(detail)
            .setPositiveButton("Use this product",(d,w)->{
                selectedCode=product.code;name.setText(product.displayName());spf.setText(product.spf);
                // Never carry protection details from the previous sunscreen to a scanned product.
                broad.setChecked(false);water.setSelection(0);confirmed.setChecked(false);confirmed.setVisibility(View.VISIBLE);
                message.setText("From Open Beauty Facts · "+product.code+"\nConfirm the label details below before saving.");
            }).setNegativeButton("Not my sunscreen",(d,w)->message.setText("Match discarded. Enter your sunscreen manually or scan again.")).show();
    }
    private void save(){
        if(busy)return;
        int value;try{value=Integer.parseInt(spf.getText().toString().trim());}catch(Exception e){spf.setError("Enter the SPF on your bottle");return;}
        if(value<1||value>150){spf.setError("Enter a label SPF between 1 and 150");return;}
        String label=name.getText().toString().trim();if(label.isEmpty()){name.setError("Enter a sunscreen name");return;}
        if(!selectedCode.isEmpty()&&!confirmed.isChecked()){message.setText("Check your bottle’s label and tick the confirmation before saving.");confirmed.requestFocus();return;}
        int resistance=water.getSelectedItemPosition()==1?40:water.getSelectedItemPosition()==2?80:0;
        prefs.edit().putString("name",label).putString("spf",String.valueOf(value)).putBoolean("broad",broad.isChecked()).putInt("water",resistance).apply();
        if(prefs.getBoolean("active",false)&&prefs.getBoolean("wet",false)){
            long revised=Rules.due(prefs.getLong("applied",0),prefs.getLong("wetStarted",0),resistance);
            prefs.edit().putLong("due",Math.min(prefs.getLong("due",0),revised)).putBoolean("alerted",false).apply();Reminders.schedule(owner);
        }
        boolean lowProtection=value<30||!broad.isChecked();dialog.dismiss();owner.update();
        if(lowProtection)new AlertDialog.Builder(owner).setTitle("Check your protection").setMessage("AAD recommends broad-spectrum SPF 30 or higher. A higher SPF does not extend the reapplication interval.").setPositiveButton("Got it",null).show();
    }
    void close(){if(closed)return;closed=true;generation++;scanAttempt++;clearModuleListener();executor.shutdownNow();if(child!=null)child.dismiss();if(dialog!=null&&dialog.isShowing())dialog.dismiss();}
}
