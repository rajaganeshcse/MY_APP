package com.app.rewardsplanet;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class NetworkIssueActivity extends AppCompatActivity {

    private VideoView videoView;
    private Button btnRefresh;

    private final Handler handler = new Handler();

    /*
     * Prevents offline video from being
     * restarted every 1 second.
     */
    private boolean offlineVideoStarted = false;

    /*
     * Prevents connected video from
     * being started multiple times.
     */
    private boolean connectedVideoStarted = false;

    /*
     * Prevents code from running after
     * Activity has finished.
     */
    private boolean activityFinished = false;


    // =========================================================
    // NETWORK CHECKER
    // =========================================================

    private final Runnable networkChecker = new Runnable() {

        @Override
        public void run() {

            if (activityFinished) {
                return;
            }

            if (isInternetAvailable()) {

                // =============================================
                // INTERNET AVAILABLE
                // =============================================

                if (!connectedVideoStarted) {

                    playConnectedVideo();
                }

            } else {

                // =============================================
                // INTERNET NOT AVAILABLE
                // =============================================

                /*
                 * IMPORTANT:
                 *
                 * Don't call playOfflineVideo() repeatedly.
                 *
                 * It will only start once and then
                 * MediaPlayer handles the looping.
                 */
                if (!offlineVideoStarted
                        && !connectedVideoStarted) {

                    playOfflineVideo();
                }
            }

            // Check again after 1 second
            handler.postDelayed(this, 1000);
        }
    };




    // =========================================================
    // CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_network_issue);

        videoView = findViewById(R.id.network_video);

        btnRefresh = findViewById(R.id.btn_refresh);
        makeFullScreen();


        // =====================================================
        // REFRESH BUTTON
        // =====================================================

        btnRefresh.setOnClickListener(v -> {

            if (activityFinished) {
                return;
            }


            // ================================================
            // INTERNET AVAILABLE
            // ================================================

            if (isInternetAvailable()) {

                if (!connectedVideoStarted) {

                    playConnectedVideo();
                }

            }

            // ================================================
            // STILL OFFLINE
            // ================================================

            else {

                /*
                 * Restart offline video manually.
                 */
                connectedVideoStarted = false;

                offlineVideoStarted = false;

                videoView.stopPlayback();

                playOfflineVideo();
            }
        });


        // =====================================================
        // INITIAL VIDEO
        // =====================================================

        if (isInternetAvailable()) {

            playConnectedVideo();

        } else {

            playOfflineVideo();
        }
    }


    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {
        super.onResume();

        if (!activityFinished) {

            handler.removeCallbacks(networkChecker);

            handler.post(networkChecker);
        }
    }


    // =========================================================
    // PAUSE
    // =========================================================

    @Override
    protected void onPause() {
        super.onPause();

        handler.removeCallbacks(networkChecker);
    }


    // =========================================================
    // OFFLINE VIDEO
    // =========================================================
    //
    // Video plays:
    //
    // START
    //   ↓
    // COMPLETE VIDEO
    //   ↓
    // START AGAIN
    //   ↓
    // COMPLETE VIDEO
    //   ↓
    // START AGAIN
    //   ↓
    // ...
    //
    // It does NOT restart every second.
    // =========================================================

    private void playOfflineVideo() {

        if (connectedVideoStarted
                || activityFinished) {

            return;
        }


        /*
         * Don't start the same offline video
         * again and again.
         */
        if (offlineVideoStarted) {

            return;
        }


        offlineVideoStarted = true;


        videoView.stopPlayback();


        String videoPath =
                "android.resource://"
                        + getPackageName()
                        + "/"
                        + R.raw.network_offline;


        // =====================================================
        // ERROR LISTENER
        // =====================================================

        videoView.setOnErrorListener(
                (mp, what, extra) -> {

                    return true;
                }
        );


        // =====================================================
        // PREPARED LISTENER
        // =====================================================

        videoView.setOnPreparedListener(mp -> {

            if (connectedVideoStarted
                    || activityFinished) {

                return;
            }


            /*
             * VERY IMPORTANT
             *
             * The COMPLETE video will play.
             *
             * When it reaches the END,
             * it automatically starts again
             * from the BEGINNING.
             */
            mp.setLooping(true);


            videoView.start();
        });


        // =====================================================
        // SET VIDEO
        // =====================================================

        videoView.setVideoPath(videoPath);
    }


    // =========================================================
    // CONNECTED VIDEO
    // =========================================================
    //
    // This video plays ONLY ONCE.
    //
    // After completion:
    //
    // NetworkIssueActivity
    //        ↓
    //      finish()
    //        ↓
    // Previous Activity
    // =========================================================

    private void playConnectedVideo() {

        if (connectedVideoStarted
                || activityFinished) {

            return;
        }


        connectedVideoStarted = true;


        /*
         * Offline video is no longer needed.
         */
        offlineVideoStarted = false;


        /*
         * Stop offline video immediately.
         */
        videoView.stopPlayback();


        String videoPath =
                "android.resource://"
                        + getPackageName()
                        + "/"
                        + R.raw.network_connected;


        // =====================================================
        // ERROR LISTENER
        // =====================================================

        videoView.setOnErrorListener(
                (mp, what, extra) -> {

                    /*
                     * Don't finish Activity
                     * if connected video has an error.
                     */
                    return true;
                }
        );


        // =====================================================
        // PREPARED LISTENER
        // =====================================================

        videoView.setOnPreparedListener(mp -> {

            if (activityFinished) {

                return;
            }


            /*
             * Connected video plays ONLY ONCE.
             */
            mp.setLooping(false);


            videoView.start();
        });


        // =====================================================
        // COMPLETION
        // =====================================================

        videoView.setOnCompletionListener(mp -> {

            if (activityFinished) {

                return;
            }


            activityFinished = true;


            /*
             * Stop network checking.
             */
            handler.removeCallbacks(
                    networkChecker
            );


            /*
             * Close NetworkIssueActivity.
             *
             * The Activity that opened this
             * Activity becomes visible again.
             */
            finish();
        });


        // =====================================================
        // SET VIDEO
        // =====================================================

        videoView.setVideoPath(videoPath);
    }


    // =========================================================
    // INTERNET CHECK
    // =========================================================

    private boolean isInternetAvailable() {

        ConnectivityManager connectivityManager =
                (ConnectivityManager)
                        getSystemService(
                                Context.CONNECTIVITY_SERVICE
                        );


        if (connectivityManager == null) {

            return false;
        }


        Network network =
                connectivityManager.getActiveNetwork();


        if (network == null) {

            return false;
        }


        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(
                        network
                );


        if (capabilities == null) {

            return false;
        }


        return capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
                &&
                capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                );
    }


    // =========================================================
    // BACK BUTTON
    // =========================================================
    //
    // Offline:
    //     Back does nothing.
    //
    // Online:
    //     Back works normally.
    // =========================================================

    @Override
    public void onBackPressed() {

        if (isInternetAvailable()) {

            super.onBackPressed();
        }

        // If offline, do nothing.
    }


    private void makeFullScreen() {

        Window window =
                getWindow();


        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(
                    false
            );


            WindowInsetsController controller =
                    window.getInsetsController();


            if (controller != null) {

                controller.setSystemBarsBehavior(
                        WindowInsetsController
                                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {

            window.getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    );
        }


        window.setStatusBarColor(
                Color.TRANSPARENT
        );


        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.LOLLIPOP) {

            window.setNavigationBarColor(
                    Color.TRANSPARENT
            );
        }
    }


    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        activityFinished = true;


        /*
         * Remove all pending callbacks.
         */
        handler.removeCallbacksAndMessages(null);


        /*
         * Stop video.
         */
        if (videoView != null) {

            videoView.stopPlayback();
        }


        super.onDestroy();
    }
}