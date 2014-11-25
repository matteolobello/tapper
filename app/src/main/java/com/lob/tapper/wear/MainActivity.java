package com.lob.tapper.wear;

/*
 * Created by Matteo Lobello
 */

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lob.tapper.R;

import java.util.Random;

public class MainActivity extends Activity implements
        DelayedConfirmationView.DelayedConfirmationListener {

    // Views
    private Button mButton, share;
    private TextView confirmation;
    private DelayedConfirmationView delayedConfirmationView;

    // Shared Preferences
    private static final String PREFS_NAME = "PREFERENCES";

    // Handler
    private Runnable r;
    private Handler h;

    // Booleans
    private boolean countdowndone = false,
            gameFinished = false,
            isCountingDown = false,
            mustPlay = false,
            newRecord = false,
            backTapped = false,
            itIsAPhone = false;

    // Integers
    private int score = 0,
            clicksToRestart = 0,
            countdownDone = 0,
            apiVersion;

    // WatchViewStub
    private WatchViewStub stub;

    // onCreate Method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mButton = (Button) stub.findViewById(R.id.button);
                mButton.setBackgroundColor(randomColor());
            }
        });
        h = new Handler();
        r = new Runnable() {
            public void run() {
                backgroundChanger();
                h.postDelayed(this, 1000);
            }
        };
        h.postDelayed(r, 1000);

        apiVersion = android.os.Build.VERSION.SDK_INT;
        setPhoneMode();
    }

    // Set Phone Mode if using a phone
    private void setPhoneMode() {
        try {
            getActionBar().hide();
            if (apiVersion >= Build.VERSION_CODES.KITKAT &&
                    apiVersion != Build.VERSION_CODES.KITKAT_WATCH)
                setImmersiveMode();
            itIsAPhone = true;
        } catch (Exception e) {
            itIsAPhone = false;
        }
    }

    // onResume Method
    @Override
    public void onResume() {
        super.onResume();
        try {
            delayedConfirmationView.setTotalTimeMs(4 * 1000);
        } catch (Exception e) {}
        setPhoneMode();
    }

    // Show DelayedView
    public void showDelayedView() {
        backTapped = false;
        mButton.setVisibility(View.GONE);
        confirmation = (TextView) findViewById(R.id.confirm);
        share = (Button)findViewById(R.id.share);
        confirmation.setVisibility(View.VISIBLE);
        delayedConfirmationView = (DelayedConfirmationView) findViewById(R.id.delayed_confirm);

        if (itIsAPhone) {
            confirmation.setVisibility(View.GONE);
            share.setVisibility(View.VISIBLE);
            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delayedConfirmationView.setTotalTimeMs(10000 * 1000);
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    String shareBody = getResources().getString(R.string.share_body) +
                            " " +
                            record() +
                            " :)" +
                            "\n\nhttps://play.google.com/store/apps/details?id=com.lob.tapper";
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(sharingIntent, String.valueOf(getResources().getString(R.string.share_via))));
                }
            });
        }

        delayedConfirmationView.setVisibility(View.VISIBLE);
        delayedConfirmationView.setTotalTimeMs(4 * 1000);
        delayedConfirmationView.setListener(this);
        delayedConfirmationView.start();
    }

    // Get a random color made by 3 random ints (R, G, B)
    private int randomColor() {
        Random rand = new Random();
        int r = rand.nextInt(255);
        int g = rand.nextInt(255);
        int b = rand.nextInt(255);
        int randomColor = Color.rgb(r, g, b);
        return randomColor;
    }

    // On Button Tap
    public void tap(View arg) {
        h.removeCallbacks(r);
        if (!countdowndone
                && !isCountingDown
                && !mustPlay
                && countdownDone == 0) {
            countdown();
        } else if (mustPlay) {
            game();
        }
    }

    // User didn't cancel, perform the action
    @Override
    public void onTimerFinished(View view) {
        if (!backTapped) {
            Intent restart = getIntent();
            overridePendingTransition(0, 0);
            restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();

            overridePendingTransition(0, 0);
            startActivity(restart);

            Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                    ConfirmationActivity.SUCCESS_ANIMATION);
            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getResources().getString(R.string.have_fun));
            startActivity(intent);
            setPhoneMode();
        }
    }

    // User canceled, abort the action
    @Override
    public void onTimerSelected(View view) {
        share.setVisibility(View.GONE);
        backTapped = true;
        Toast.makeText(getApplicationContext(), "OK", Toast.LENGTH_SHORT).show();
        mButton.setVisibility(View.VISIBLE);
        delayedConfirmationView.setVisibility(View.GONE);
        confirmation.setVisibility(View.GONE);
        playAgain();
    }

    // Play Again
    private void playAgain() {
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clicksToRestart++;
                if (clicksToRestart == 1) {
                    Toast.makeText(getApplicationContext(), R.string.tap_to_restart, Toast.LENGTH_SHORT).show();
                } else if (clicksToRestart == 2) {
                    clicksToRestart = 0;
                    showDelayedView();
                }
            }
        });
    }

    // Pause (1 second)
    private void waitCountdown() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Countdown (3...2...1...)
    private void countdown() {
        mButton.setText("...");
        mButton.setTextSize(50);
        countdownDone++;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mButton.setClickable(false);
                for (int i = 0; i <= 4; i++) {
                    final int value = i;
                    waitCountdown();
                    mButton.post(new Runnable() {
                        @Override
                        public void run() {
                            if (value == 1) {
                                mButton.setText("3...");
                                backgroundChanger();
                            } else if (value == 2) {
                                mButton.setText("2...");
                                backgroundChanger();
                            } else if (value == 3) {
                                mButton.setText("1...");
                                backgroundChanger();
                            } else if (value == 4) {
                                mButton.setText(R.string.go);
                                game();
                                countdowndone = true;
                                mustPlay = true;
                                mButton.setClickable(true);
                                mButton.setTextSize(80);
                            }
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

    // Get record from shared preferences
    private int record() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int record = prefs.getInt("record", 0);
        return record;
    }

    // Game method
    private void game() {

        if (!gameFinished) {
            score++;
            mButton.setText(String.valueOf(score - 1));
            backgroundChanger();
        }

        new CountDownTimer(10000, 1000) { // 10 seconds

            @Override
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                mButton.setClickable(true);
                mButton.setTextSize(23);

                if (score > record()) {
                       SharedPreferences.Editor editor =
                               getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                       .edit();
                       editor.putInt("record", score - 1);
                       editor.commit();

                    mButton.setText(String.valueOf(getResources().getString(R.string.done_new_best)) +
                            " " +
                            String.valueOf(score - 1) +
                            String.valueOf(getResources().getString(R.string.best_score)) +
                            " " +
                            String.valueOf(record()));
                    gameFinished = true;
                    newRecord = true;
                } else {
                    if (!newRecord) {
                        mButton.setText(String.valueOf(getResources().getString(R.string.done_new_score)) +
                                " " +
                                String.valueOf(score - 1) +
                                String.valueOf(getResources().getString(R.string.best_score)) +
                                " " +
                                String.valueOf(record()));
                        gameFinished = true;
                    }
                }
                mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clicksToRestart++;
                        if (clicksToRestart == 1) {
                            Toast.makeText(getApplicationContext(),
                                    R.string.tap_to_restart,
                                    Toast.LENGTH_SHORT)
                                    .show();
                        } else if (clicksToRestart == 2) {
                            clicksToRestart = 0;
                            showDelayedView();
                        }
                    }
                });
            }
        }.start();
    }

    // Change background
    private void backgroundChanger() {

        Integer colorFrom = randomColor();
        Integer colorTo = randomColor();
        ValueAnimator colorAnimation =
                ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        mButton.setBackgroundColor
                                ((Integer) animator.getAnimatedValue());
                    }

                });
        colorAnimation.start();
    }

    // Immersive Mode for Phone
    private void setImmersiveMode() {
        getWindow().getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
    }

}

