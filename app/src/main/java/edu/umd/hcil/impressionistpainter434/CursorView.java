package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by MarkCreamer on 11/22/16.
 */
public class CursorView extends View {
    private boolean touching = false;
    private Point cursor = new Point();
    private Paint paint = new Paint();
    private Bitmap bitmap;

    public CursorView(Context context) {
        super(context);
        this.setBackgroundColor(Color.TRANSPARENT);

        paint.setColor(Color.BLACK);
    }

    public CursorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundColor(Color.TRANSPARENT);

        paint.setColor(Color.BLACK);
    }

    public CursorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setBackgroundColor(Color.TRANSPARENT);

        paint.setColor(Color.BLACK);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the cursor if the user's touching the screen
        if(touching) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            canvas.drawCircle(cursor.x,cursor.y,20,paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cursor.x,cursor.y,20,paint);
        }
    }

    public void setTouchPoint(int x, int y) {
        cursor.set(x,y);
    }

    public void setTouching(boolean input) {
        touching = input;
    }

    public void setBitmap(Bitmap input) {
        bitmap = input;
    }
}
