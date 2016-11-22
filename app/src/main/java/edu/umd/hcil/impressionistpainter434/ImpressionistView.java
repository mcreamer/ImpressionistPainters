package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.face.Face;

import junit.runner.BaseTestRunner;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 * Editted by Mark
 */
public class ImpressionistView extends View {

    private ImageView _imageView;
    private CursorView _cursorView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
//    private Point _lastPoint = null;
//    private long _lastPointTime = -1;
//    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;

    private Paint _whitePaint = new Paint(Color.WHITE);
    private int _minRadius = 5;
    private int _maxRadius = 30;

    private SparseArray<Face> faces;
    private SparseArray<Rect> canvasFaceBoxes;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);
        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

        _whitePaint.setColor(Color.WHITE);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){
        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
            canvasFaceBoxes = new SparseArray<>();
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    public void setCursorView(CursorView input) {
        _cursorView = input;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        Toast.makeText(getContext(), "Drawing Cleared", Toast.LENGTH_SHORT).show();
        _offScreenCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Rect frame = getBitmapPositionInsideImageView(_imageView);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);

            // Clean the margins
            canvas.drawRect(0,0,getWidth(),frame.top,_whitePaint);
            canvas.drawRect(0,frame.bottom,getWidth(),getHeight(),_whitePaint);
            canvas.drawRect(0,0,frame.left,getHeight(),_whitePaint);
            canvas.drawRect(frame.right,0,getWidth(),getHeight(),_whitePaint);

            // Uncomment to show face boxes
            //drawFaceBoxes(canvas);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(frame, _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        float x = motionEvent.getX(), y = motionEvent.getY();

        if(_imageView.getDrawable() == null)
            return true;

        Bitmap bitmap = ((BitmapDrawable)_imageView.getDrawable()).getBitmap();
        Rect bounds = getBitmapPositionInsideImageView(_imageView);
        if(!bounds.contains((int)x,(int)y))
            return true;

        // Apply scaling and bounds
        float scalex = bitmap.getWidth()/(float)(bounds.width());
        float scaley = bitmap.getHeight()/(float)(bounds.height());
        float picX = (x-bounds.left)*scalex;
        float picY = (y-bounds.top)*scaley;

        if(picX < 0 || picX > bitmap.getWidth() || picY < 0 || picY > bitmap.getHeight())
            return true;
        int pictureColor = bitmap.getPixel((int)picX,(int)picY);
        _paint.setColor(pictureColor);
        _paint.setStrokeWidth(3);

        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                _cursorView.setTouching(true);
            case MotionEvent.ACTION_MOVE:
                _cursorView.setTouchPoint((int)x,(int)y);

                float faceScale = 1f;
                float size = _defaultRadius;

                // If the cursor is on a face, reduce the drawing size for more fine tuned precision
                if(faces != null) {
                    for(int i = 0; i < canvasFaceBoxes.size(); i++) {
                        Rect canvasFaceBox = canvasFaceBoxes.get(i);

                        if(canvasFaceBox.contains((int)x,(int)y)) {
                            faceScale = 1/2.0f;
                            size *= 1/2.0f;
                        }
                    }
                }

                if(_brushType == BrushType.Circle) {
                    _offScreenCanvas.drawCircle(x,y,size,_paint);
                }
                else if(_brushType == BrushType.Square) {
                    _offScreenCanvas.drawRect(x-size,y-size,x+size,y+size,_paint);
                }
                else if(_brushType == BrushType.CircleSplatter) {
                    for(int i = 0; i < 5; i++) {
                        int x1 = (int)(x+Math.random()*2*size-size);
                        int y1 = (int)(y+Math.random()*2*size-size);
                        int radius = _minRadius+(int)(Math.random()*(_maxRadius-_minRadius));
                        _offScreenCanvas.drawCircle(x1, y1, radius*faceScale, _paint);
                    }
                }
                else if(_brushType == BrushType.Line) {
                    int x1 = (int)(x+Math.random()*2*size-size);
                    int y1 = (int)(y+Math.random()*2*size-size);

                    int x2 = (int)(x+Math.random()*2*size-size);
                    int y2 = (int)(y+Math.random()*2*size-size);
                    _offScreenCanvas.drawLine(x1,y1,x2,y2, _paint);
                }
                else if(_brushType == BrushType.LineSplatter) {
                    for(int i = 0; i < 10; i++) {
                        int x1 = (int)(x+Math.random()*2*size-size);
                        int y1 = (int)(y+Math.random()*2*size-size);

                        int x2 = (int)(x+Math.random()*2*size-size);
                        int y2 = (int)(y+Math.random()*2*size-size);
                        _offScreenCanvas.drawLine(x1,y1,x2,y2, _paint);
                    }
                }
                else if(_brushType == BrushType.Radial) {
                    // Find the closest face to orbit
                    Point center = new Point();

                    Rect faceBounds;
                    Point boxCenter = new Point();
                    double dist,minDist = Math.max(getWidth(),getHeight());
                    for(int i = 0; i < canvasFaceBoxes.size(); i++) {
                        faceBounds = canvasFaceBoxes.get(i);
                        boxCenter.set((faceBounds.right+faceBounds.left)/2,(faceBounds.bottom+faceBounds.top)/2);

                        dist = Math.sqrt(Math.pow(boxCenter.x-x,2)+Math.pow(boxCenter.y-y,2));
                        if(dist < minDist) {
                            center.set(boxCenter.x,boxCenter.y);
                            minDist = dist;
                        }
                    }

                    // The ratio that helps determine the line length in relation to the center
                    float ratio = 0.25f;

                    double line_radius = ratio*Math.sqrt(Math.pow(center.x-x,2)+Math.pow(center.y-y,2));

                    float n_x1 = (float)(x - line_radius*Math.cos(Math.atan((y-center.y)/(x-center.x))));
                    float n_y1 = (float)(y - line_radius*Math.sin(Math.atan((y-center.y)/(x-center.x))));

                    float n_x2 = (float)(x + line_radius*Math.cos(Math.atan((y-center.y)/(x-center.x))));
                    float n_y2 = (float)(y + line_radius*Math.sin(Math.atan((y-center.y)/(x-center.x))));

                    _offScreenCanvas.drawLine(n_x1,n_y1,n_x2,n_y2,_paint);
                }
                else if(_brushType == BrushType.Ring) {
                    // Find the closest face to orbit
                    Point center = new Point();

                    Rect faceBounds;
                    Point boxCenter = new Point();
                    double dist,minDist = Math.max(getWidth(),getHeight());
                    for(int i = 0; i < canvasFaceBoxes.size(); i++) {
                        faceBounds = canvasFaceBoxes.get(i);
                        boxCenter.set((faceBounds.right+faceBounds.left)/2,(faceBounds.bottom+faceBounds.top)/2);

                        dist = Math.sqrt(Math.pow(boxCenter.x-x,2)+Math.pow(boxCenter.y-y,2));
                        if(dist < minDist) {
                            center.set(boxCenter.x,boxCenter.y);
                            minDist = dist;
                        }
                    }

                    // The ratio that helps determine the line length in relation to the center
                    double ring_length = 50;
                    float n_x1 = (float)(x - ring_length/2*Math.cos(Math.atan(-(x-center.x)/(y-center.y))));
                    float n_y1 = (float)(y - ring_length/2*Math.sin(Math.atan(-(x-center.x)/(y-center.y))));

                    float n_x2 = (float)(x + ring_length/2*Math.cos(Math.atan(-(x-center.x)/(y-center.y))));
                    float n_y2 = (float)(y + ring_length/2*Math.sin(Math.atan(-(x-center.x)/(y-center.y))));

                    _offScreenCanvas.drawLine(n_x1,n_y1,n_x2,n_y2,_paint);
                }
                _cursorView.invalidate();
                break;
            case MotionEvent.ACTION_UP:
                _cursorView.setTouching(false);
                break;
        }

        invalidate();
        return true;
    }

    //Edited from https://code.tutsplus.com/tutorials/an-introduction-to-face-detection-on-android--cms-25212
    private void drawFaceBoxes(Canvas canvas) {
        //paint should be defined as a member variable rather than
        //being created on each onDraw request, but left here for
        //emphasis.
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        Rect rect;
        for(int i = 0; i < canvasFaceBoxes.size(); i++) {
            rect = canvasFaceBoxes.get(i);
            canvas.drawRect(rect, paint);
        }
    }

    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }

    public Bitmap getBitmap() {
        return _offScreenBitmap;
    }

    //Edited from https://code.tutsplus.com/tutorials/an-introduction-to-face-detection-on-android--cms-25212
    public void setFaces(SparseArray<Face> faces) {
        this.faces = faces;

        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;

        if(_imageView.getDrawable() != null) {
            Bitmap bitmap = ((BitmapDrawable) _imageView.getDrawable()).getBitmap();
            Rect bounds = getBitmapPositionInsideImageView(_imageView);
            float scalex = bitmap.getWidth() / (float) (bounds.width());
            float scaley = bitmap.getHeight() / (float) (bounds.height());

            for (int i = 0; i < faces.size(); i++) {
                Face face = faces.valueAt(i);

                left = (int)(face.getPosition().x / scalex + bounds.left);
                top = (int)(face.getPosition().y / scaley + bounds.top);
                right = (int)((face.getPosition().x + face.getWidth()) / scalex + bounds.left);
                bottom = (int)((face.getPosition().y + face.getHeight()) / scaley + bounds.top);

                Rect newRect = new Rect(left, top, right, bottom);
                canvasFaceBoxes.put(i,newRect);
            }
        }
    }
}

