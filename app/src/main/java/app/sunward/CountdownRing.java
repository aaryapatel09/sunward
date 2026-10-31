package app.sunward;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Decorative reminder countdown. Does not represent sunscreen effectiveness. */
final class CountdownRing extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds=new RectF();
    private float remaining;private boolean active;
    CountdownRing(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void setRemaining(float fraction,boolean running){
        float next=Math.max(0,Math.min(1,fraction));
        if(next!=remaining || running!=active){remaining=next;active=running;invalidate();}
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float density=getResources().getDisplayMetrics().density;
        float radius=Math.min(getWidth(),getHeight())/2f-8*density;
        float x=getWidth()/2f,y=getHeight()/2f;bounds.set(x-radius,y-radius,x+radius,y+radius);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(2*density);paint.setColor(0xffeeeeea);
        canvas.drawOval(bounds,paint);
        if(active && remaining>0){paint.setColor(0xff202020);paint.setStrokeWidth(3*density);canvas.drawArc(bounds,-90,remaining*360,false,paint);}
        if(!active){paint.setStyle(Paint.Style.FILL);paint.setColor(0xff202020);canvas.drawCircle(x,y-radius,3*density,paint);}
    }
}
