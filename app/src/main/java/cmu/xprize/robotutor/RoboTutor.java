//*********************************************************************************
//
//    Copyright(c) 2016 Carnegie Mellon University. All Rights Reserved.
//    Copyright(c) Kevin Willows All Rights Reserved
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************

package cmu.xprize.robotutor;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

import cmu.xprize.robotutor.tutorengine.CTutor;
import cmu.xprize.robotutor.tutorengine.CTutorEngine;
import cmu.xprize.robotutor.tutorengine.ITutorSceneAnimator;
import cmu.xprize.robotutor.tutorengine.ITutorSceneImpl;
import cmu.xprize.util.IReadyListener;
import cmu.xprize.util.ProgressLoading;
import cmu.xprize.util.TCONST;
import cmu.xprize.robotutor.tutorengine.CTutorAssetManager;
import cmu.xprize.util.TTSsynthesizer;
import edu.cmu.xprize.listener.Listener;


/**
 * <h2>Class Overview</h2>
 * <hr>
 * This class represents the root activity for a Tutor Manager that can display one of many
 * instructional tutors.  Tutors may also link to other Activities that themselves represent
 * Tutor Managers and can vector to specific tutors contained therein.
 * <br>
 * <h3>Developer Overview</h3>
 *
 */
public class RoboTutor extends Activity implements IReadyListener {

    private CTutorEngine        tutorEngine;
    private ITutorSceneAnimator tutorContainer;
    private ProgressLoading     progressLoading;

    public TTSsynthesizer       TTS;
    public Listener             ASR;
    static public String        EXTERNFILES;

    static private boolean isReady = false;


    private final  String  TAG = "RoboTutor";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.robo_tutor);

        // Get the primary container for tutors
        tutorContainer = (ITutorSceneAnimator)findViewById(R.id.tutor_container);

        EXTERNFILES = getApplicationContext().getExternalFilesDir("").getPath();

        // Debug - determine platform dependent memory limit
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memAvail = am.getMemoryClass();
        Log.i(TAG, "Available Memory: " + memAvail);

        // Create the common TTS service
        // Async
        //
        TTS = new TTSsynthesizer(this);
        TTS.initializeTTS(this);

        // Start an async task to update the listener assets if required.
        // Async
        //
        ASR = new Listener("configassets");
        ASR.configListener(this);

        // Start the async task to initialize the tutor
        //
        new tutorConfigTask().execute();

        // Show the loader
        //
        progressLoading = new ProgressLoading(this);
        progressLoading.show();
    }


    /**
     * Moves new assets to an external folder so the Sphinx code can access it.
     *
     */
    class tutorConfigTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Boolean doInBackground(Void... unused) {

            boolean result = false;

            CTutorAssetManager tutorAssetManager = new CTutorAssetManager(getApplicationContext());

            try {
                // TODO: Don't do this in production
                // At the moment we always reinstall the tutor spec data - for development
                if(CTutorEngine.CacheSource.equals(TCONST.EXTERN)) {
                    tutorAssetManager.installAssets(TCONST.TUTORROOT);
                    Log.i(TAG, "INFO: Tutor Assets installed:");
                }

                if(!tutorAssetManager.fileCheck(TCONST.INSTALL_FLAG)) {
                    tutorAssetManager.installAssets(TCONST.LTK_ASSETS);
                    Log.i(TAG, "INFO: LTK Assets copied:");

                    tutorAssetManager.extractAsset(TCONST.LTK_DATA_FILE, TCONST.LTK_DATA_FOLDER);
                    Log.i(TAG, "INFO: LTK Assets installed:");
                }
                result = true;

            } catch (IOException e) {
                // TODO: Manage exceptions
                e.printStackTrace();
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            isReady = result;

            onServiceReady("ROOT", result ? 1 : 0);
        }
    }


    /**
     * Callback used by services to announce ready state
     * @param serviceName
     */
    @Override
    public void onServiceReady(String serviceName, int status) {
        Log.i("TutorEngine",serviceName + " : is : " + status);

        // As the services come online push a global reference to CTutor
        //
        switch(serviceName) {
            case TCONST.TTS:
                CTutor.TTS = TTS;
                break;

            case TCONST.ASR:
                CTutor.ASR = ASR;
                break;


        }
        startEngine();
    }


    /**
     * Start the tutor engine once everything is intialized.
     *
     * There are several async init tasks and they all call this when they're finished.
     * The last one ready passes all the tests and starts the engine.
     *
     * TODO: Manage initialization failures
     *
     */
    private void startEngine() {

        if(TTS.isReady() && ASR.isReady() && isReady) {

            progressLoading.hide();

            // Initialize the Engine - set the EXTERN File path for file installs
            // Load the default tutor defined in assets/tutors/engine_descriptor.json
            // TODO: Handle tutor creation failure
            tutorEngine = CTutorEngine.getTutorEngine(RoboTutor.this, tutorContainer);
            tutorEngine.initialize();
        }
    }
}
