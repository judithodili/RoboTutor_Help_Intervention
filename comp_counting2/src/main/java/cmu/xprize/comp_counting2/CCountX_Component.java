package cmu.xprize.comp_counting2;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import cmu.xprize.comp_logging.CErrorManager;
import cmu.xprize.util.ILoadableObject;
import cmu.xprize.util.IScope;
import cmu.xprize.util.JSON_Helper;
import cmu.xprize.util.TCONST;

/**
 * Created by kevindeland on 10/20/17.
 */

public class CCountX_Component extends PercentRelativeLayout implements ILoadableObject {

    // Infrastructure
    protected final Handler mainHandler  = new Handler(Looper.getMainLooper());
    protected HashMap queueMap     = new HashMap();
    protected HashMap           nameMap      = new HashMap();
    protected boolean           _qDisabled   = false;

    // Layout
    private RelativeLayout Scontent;
    private CCountX_SurfaceView surfaceView;
    private TextView counterText;
    private TextView counterTextTen;
    private TextView counterTextHundred;
    private TextView counterTextSum;
    private TextView stimulusText;
    protected TextView checkOne;
    protected TextView checkTen;
    protected TextView checkHundred;

    // DataSource Variables
    protected int _dataIndex = 0;
    protected String level;
    protected String task;
    protected String layout;
    private int startingNumber;
    protected int countStart;
    protected int countTarget;
    protected int currentCount;
    protected int currentValue;
    protected int delta;
    protected int tenPower;
    protected int drawIndex;
    protected String mode = "";
    protected float bedge;
    protected boolean tenInited=false;


    // json loadable
    public String bootFeatures;
    public CCountX_Data[] dataSource;


    // View Things
    static protected Context mContext;

    protected TableLayout dotTable;
    protected TableRow[] dotTableRows;

    protected int numDotsCounted;

    private LocalBroadcastManager bManager;

    private ArrayList<Boolean> dotsTapped;


    // Animation stuff
    private boolean isRunning = false;


    static final String TAG = "CCount_Component";

    // REMOVE private CCount_DotBag dotBag;


    public CCountX_Component(Context context) {
        super(context);
        init(context, null);
    }


    public CCountX_Component(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CCountX_Component(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {

        mContext = context;

        inflate(getContext(), R.layout.tapping, this);

        Scontent = (RelativeLayout) findViewById(R.id.Scontent);
        surfaceView = (CCountX_SurfaceView) findViewById(R.id.imageSurface);
        surfaceView.setComponent(this);
        counterText = (TextView) findViewById(R.id.counterText);
        checkOne = (TextView) findViewById(R.id.checkOne);
        checkTen = (TextView) findViewById(R.id.checkTen);
        checkHundred = (TextView) findViewById(R.id.checkHundred);
        stimulusText = (TextView) findViewById(R.id.stimulusText);

        bManager = LocalBroadcastManager.getInstance(getContext());
        drawIndex =-10;


        //mainHandler.post(animationRunnable);

    }

    private int runCount = 0;
    private int runWait = 100;
    private Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {

            if(isRunning && surfaceView!= null) {
                surfaceView.wiggleFruit();
                mainHandler.postDelayed(animationRunnable, 10);
                runCount++;

            }

            // if we're done counting, apply behavior to queue and stop thread
            if (surfaceView.doneMovingToTenFrame) {
                postFinalCount();
                applyBehavior(COUNTX_CONST.DONE_MOVING_TO_TEN_FRAME);
                isRunning = false;

            }
        }
    };

    public void next() {


        try {
            if (dataSource != null) {
                updateDataSet(dataSource[_dataIndex]);

                _dataIndex++;
                numDotsCounted = 0;

                surfaceView.hideTenFrame();

                // reset target text
                stimulusText.setText("");
            }
        } catch (Exception e) {
            CErrorManager.logEvent(TAG, "Data Exhuasted: call past end of data", e, false);
        }

    }

    public boolean dataExhausted() {
        return _dataIndex >= dataSource.length;
    }

    protected void updateDataSet(CCountX_Data data) {
        Log.d(TCONST.COUNTING_DEBUG_LOG, "updateDateSet");

        // first load dataset into fields
        loadDataSet(data);


        resetView();
        if(mode != "placevalue"){
        surfaceView.initTenFrame();}
        else{
            surfaceView.initTenFramePlaceValue();
        }
        //surfaceView.resetCounter(); // functionality already called above

        // reset vieresetView();
        // update stimulus

        // then update visuals
        // addDots(countTarget);
    }

    /**
     * Loads from current dataset into the private DataSource fields
     *
     * @param data the current element in the DataSource array.
     */
    protected void loadDataSet(CCountX_Data data) {
        level = data.level;
        task = data.task;
        layout = data.layout; // NOV_1 make this consistent w/ Anthony
        countStart = data.dataset[0];
        if (data.tenPower.length==1){
            mode = "countingx";
            if (data.tenPower.equals("one")){
                tenPower = 1;
            } else if (data.tenPower.equals("ten")){
                tenPower = 10;
            } else {
                tenPower = 100;
            }
            countTarget = data.dataset[1]*tenPower;

        } else {
            mode = "placevalue";
            countTarget = data.dataset[1];
            tenPower = 1;
        }


        Log.d(TCONST.COUNTING_DEBUG_LOG, "target=" + countTarget + ";index=" + _dataIndex);
    }

    /**
     * Reset the view
     */
    protected void resetView() {

        // reset the TextView

        if (mode=="placevalue"){
            surfaceView.clearObjectsToNumber(countStart);
        } else {
            String initialCount = String.valueOf(countStart);
            counterText.setText(initialCount);
            surfaceView.clearObjectsToNumber(countStart);
        }


        // reset the surfaceView

    }

    public void updateCount(int count) {
        // reset the TextView
        currentCount = count;
        String initialCount = String.valueOf(count);
        counterText.setText(initialCount);

        if(currentCount == countTarget) {
            applyBehavior(COUNTX_CONST.DONE_COUNTING_TO_N);
        }
    }

    public void updateCountPlaceValue(int hundred, int ten, int one,int currentV){
//        String initialOne = String.valueOf(one);
//        counterText.setText(initialOne);
//
//        String initialTen = String.valueOf(ten*10);
//        counterTextTen.setText(initialTen);
//
//        String initialHundred = String.valueOf(hundred*100);
//        counterTextHundred.setText(initialHundred);
//
//        String sum = String.valueOf(hundred*100+ten*10+one);
//        counterTextSum.setText(sum);

        if(one == countTarget%10) {
            surfaceView.reachTarget[2] = true;
        }

        if(ten*10 == countTarget%100-countTarget%10){
            surfaceView.reachTarget[1] = true;

        }

        if(hundred*100 == countTarget-countTarget%100){
            surfaceView.reachTarget[0] = true;

        }
        currentValue = currentV;
        if(surfaceView.reachTarget[0] && surfaceView.reachTarget[1] && surfaceView.reachTarget[2]) {
            applyBehavior(COUNTX_CONST.DONE_COUNTING_TO_N);
        }
    }

    public void setCheck(boolean hundred,boolean ten,boolean one){
        if (hundred){
            checkHundred.setVisibility(View.VISIBLE);
        }

        if(ten){
            checkTen.setVisibility(View.VISIBLE);
        }

        if(one){
            checkOne.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Point at a view
     */
    public void pointAtCenterOfActivity() {
        Log.d(TCONST.COUNTING_DEBUG_LOG, "pointing at something");

        int[] screenCoord = new int[2];
        surfaceView.getLocationOnScreen(screenCoord);

        PointF targetPoint = new PointF(screenCoord[0] + surfaceView.getWidth()/2,
                screenCoord[1] + surfaceView.getHeight()/2);
        Intent msg = new Intent(TCONST.POINTAT);
        msg.putExtra(TCONST.SCREENPOINT, new float[]{targetPoint.x, targetPoint.y});

        bManager.sendBroadcast(msg);
    }

    /**
     * Update stimulus and point to it
     */
    public void updateAndIndicateStimulus() {
        Log.d(TCONST.COUNTING_DEBUG_LOG, "indicating stimulus");

        // update target text
        String stimulus = String.valueOf(countTarget);
        stimulusText.setText(stimulus);

        // point to it using RoboFinger

        int[] screenCoord = new int[2];
        stimulusText.getLocationOnScreen(screenCoord);

        PointF targetPoint = new PointF(screenCoord[0] + stimulusText.getWidth()/2,
                screenCoord[1] + stimulusText.getHeight()/2);
        Intent msg = new Intent(TCONST.POINTAT);
        msg.putExtra(TCONST.SCREENPOINT, new float[]{targetPoint.x, targetPoint.y});

        bManager.sendBroadcast(msg);

    }

    /**
     * Overridden by child class.
     */
    public void playChime() {

    }

    public void playCount(int count){

    }

    /**
     * Overridden by child class.
     */
    public void postFinalCount() {

    }

    public void setBedge(float length){
        if(bedge == 0){
            bedge = length;
        }
    }

    public void demonstrateTenFrame() {
        if (mode == "placevalue"){
            checkHundred.setVisibility(View.INVISIBLE);
            checkTen.setVisibility(View.INVISIBLE);
            checkOne.setVisibility(View.INVISIBLE);
            surfaceView.displayAddition("hundred");
            playCount(countTarget-countTarget%100);
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            surfaceView.displayAddition("ten");
                            playCount(countTarget%100-countTarget%10);
                        }
                    },
                    4000
            );

            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            surfaceView.displayAddition("one");
                            playCount(countTarget%10);
                        }
                    },
                    8000
            );
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            surfaceView.displayAddition("final");
                            playCount(countTarget);
                        }
                    },
                    12000
            );
        } else {
            surfaceView.showTenFrame();
            isRunning = true;
            mainHandler.post(animationRunnable);
        }
        //surfaceView.moveItemsToTenFrame();
    }

    /**
     * prevent the student from tapping
     */
    public void disableTapping() {
        surfaceView.enableTapping(false);
    }

    /**
     * allow the student to tap
     */
    public void enableTapping() {
        surfaceView.enableTapping(true);

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_DOWN) {
            //handleClick();
        }

        return true;
    }


    // Must override in TClass
    // TClass domain where TScope lives providing access to tutor scriptables
    //
    public boolean applyBehavior(String event) {
        return false;
    }

    /**
     * Load the data source
     *
     * @param jsonData
     */
    @Override
    public void loadJSON(JSONObject jsonData, IScope scope) {

        JSON_Helper.parseSelf(jsonData, this, CClassMap.classMap, scope);
        _dataIndex = 0;
    }

    public RelativeLayout getContainer() {
        return Scontent;
    }



    public void postEvent(String event) {
        postEvent(event, 0);
    }

    public void postEvent(String event, Integer delay) {

        post(event, delay);
    }

    public void post(String command, long delay) {

        enQueue(new Queue(command, command), delay);
    }

    private void enQueue(Queue qCommand, long delay) {

        if(!_qDisabled) {
            queueMap.put(qCommand, qCommand);

            if(delay > 0) {
                mainHandler.postDelayed(qCommand, delay);
            }
            else {
                mainHandler.post(qCommand);
            }
        }
    }

    /**
     * This is how Component-specific commands are added to the Queue.
     */
    public class Queue implements Runnable {

        String _name;
        String _command;
        String _target;
        String _item;

        Queue(String name, String command) {
            _name = name;
            _command = command;

            nameMap.put(name, this);
        }

        @Override
        public void run() {

            Log.d("COUNTINGX_DEBUG_TAG", "Running queue: _command=" + _command);
            try {
                if(_name != null) {
                    nameMap.remove(_name);
                }

                queueMap.remove(this);

                switch(_command) {
                    case COUNTX_CONST.PLAY_CHIME:
                        applyBehavior(_command);
                        break;

                    default:
                        break;
                }

            }  catch(Exception e) {
                CErrorManager.logEvent(TAG, "Run Error:", e, false);
            }

        }
    }

}