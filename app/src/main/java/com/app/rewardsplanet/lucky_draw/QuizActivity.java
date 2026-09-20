package com.app.rewardsplanet.lucky_draw;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.repository.UserRepository;
import com.app.rewardsplanet.utils.SuccessAnimationHelper;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";
    private static final int TOTAL_QUESTIONS = 10;
    private static final long QUESTION_TIMER_MS = 15000; // 15 seconds

    // Quiz Reward configuration as per explicit user requirement
    private static final int REWARD_COINS = 25;
    private static final int REWARD_TICKETS = 10;

    // UI
    private ImageButton btnBack;
    private TextView txtQuestionTitle;
    private TextView txtScore;
    private ProgressBar quizProgressBar;
    private TextView txtTimer;
    private TextView txtQuestion;

    private LinearLayout[] optionLayouts = new LinearLayout[4];
    private ImageView[] optionRadios = new ImageView[4];
    private TextView[] optionTexts = new TextView[4];

    private MaterialButton btnWatchAdSubmit;
    private MaterialButton btnSubmit;
    private AdView adView;

    // State
    private UserPref userPref;
    private FirebaseFirestore db;
    private CountDownTimer countDownTimer;

    private List<Question> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int selectedOptionIndex = -1;

    private RewardedAd rewardedAd;
    private boolean isQuizCompleted = false;

    private static class Question {
        String questionText;
        String[] options;
        int correctIndex;

        Question(String questionText, String[] options, int correctIndex) {
            this.questionText = questionText;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        userPref = new UserPref(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupQuestionBank();
        loadBannerAd();
        loadRewardedAd();

        displayQuestion(currentQuestionIndex);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtQuestionTitle = findViewById(R.id.txtQuestionTitle);
        txtScore = findViewById(R.id.txtScore);
        quizProgressBar = findViewById(R.id.quizProgressBar);
        txtTimer = findViewById(R.id.txtTimer);
        txtQuestion = findViewById(R.id.txtQuestion);

        optionLayouts[0] = findViewById(R.id.layoutOption1);
        optionLayouts[1] = findViewById(R.id.layoutOption2);
        optionLayouts[2] = findViewById(R.id.layoutOption3);
        optionLayouts[3] = findViewById(R.id.layoutOption4);

        optionRadios[0] = findViewById(R.id.imgRadio1);
        optionRadios[1] = findViewById(R.id.imgRadio2);
        optionRadios[2] = findViewById(R.id.imgRadio3);
        optionRadios[3] = findViewById(R.id.imgRadio4);

        optionTexts[0] = findViewById(R.id.txtOption1);
        optionTexts[1] = findViewById(R.id.txtOption2);
        optionTexts[2] = findViewById(R.id.txtOption3);
        optionTexts[3] = findViewById(R.id.txtOption4);

        btnWatchAdSubmit = findViewById(R.id.btnWatchAdSubmit);
        btnSubmit = findViewById(R.id.btnSubmit);
        adView = findViewById(R.id.adView);

        btnBack.setOnClickListener(v -> finish());

        for (int i = 0; i < 4; i++) {
            final int index = i;
            optionLayouts[i].setOnClickListener(v -> selectOption(index));
        }

        btnSubmit.setOnClickListener(v -> submitQuestion(false));
        btnWatchAdSubmit.setOnClickListener(v -> submitQuestion(true));
    }

    private void setupQuestionBank() {
        questionList.add(new Question("10 - 5 = ?", new String[]{"5", "14", "1", "11"}, 0));
        questionList.add(new Question("12 + 8 = ?", new String[]{"18", "20", "22", "16"}, 1));
        questionList.add(new Question("15 / 3 = ?", new String[]{"3", "4", "5", "6"}, 2));
        questionList.add(new Question("7 x 6 = ?", new String[]{"42", "36", "48", "40"}, 0));
        questionList.add(new Question("25 + 25 = ?", new String[]{"40", "50", "60", "55"}, 1));
        questionList.add(new Question("50 - 15 = ?", new String[]{"30", "35", "40", "25"}, 1));
        questionList.add(new Question("9 x 9 = ?", new String[]{"72", "81", "90", "63"}, 1));
        questionList.add(new Question("100 / 4 = ?", new String[]{"20", "25", "30", "50"}, 1));
        questionList.add(new Question("14 + 16 = ?", new String[]{"30", "28", "32", "34"}, 0));
        questionList.add(new Question("8 x 5 = ?", new String[]{"35", "40", "45", "50"}, 1));
        questionList.add(new Question("60 - 24 = ?", new String[]{"36", "34", "38", "40"}, 0));
        questionList.add(new Question("18 / 2 = ?", new String[]{"8", "9", "10", "7"}, 1));
        questionList.add(new Question("30 + 45 = ?", new String[]{"70", "75", "80", "65"}, 1));
        questionList.add(new Question("11 x 3 = ?", new String[]{"30", "33", "36", "44"}, 1));
        questionList.add(new Question("80 - 35 = ?", new String[]{"45", "40", "50", "35"}, 0));

        // Keep first question fixed if it matches 10 - 5 = ?, shuffle remaining for variety
        if (questionList.size() > 1) {
            List<Question> rest = new ArrayList<>(questionList.subList(1, questionList.size()));
            Collections.shuffle(rest);
            List<Question> finalQuestions = new ArrayList<>();
            finalQuestions.add(questionList.get(0));
            finalQuestions.addAll(rest);
            questionList = finalQuestions;
        }
    }

    private void displayQuestion(int index) {
        if (index < 0 || index >= TOTAL_QUESTIONS || index >= questionList.size()) return;

        Question q = questionList.get(index);
        selectedOptionIndex = -1;

        txtQuestionTitle.setText("Question " + (index + 1) + "/" + TOTAL_QUESTIONS);
        txtScore.setText("Score: " + score);
        quizProgressBar.setProgress(index + 1);
        txtQuestion.setText(q.questionText);

        for (int i = 0; i < 4; i++) {
            optionTexts[i].setText(q.options[i]);
            optionLayouts[i].setBackgroundResource(R.drawable.bg_quiz_option_unselected);
            optionRadios[i].setImageResource(R.drawable.ic_radio_unchecked);
        }

        startTimer();
    }

    private void selectOption(int index) {
        selectedOptionIndex = index;
        for (int i = 0; i < 4; i++) {
            if (i == index) {
                optionLayouts[i].setBackgroundResource(R.drawable.bg_quiz_option_selected);
                optionRadios[i].setImageResource(R.drawable.ic_radio_checked);
            } else {
                optionLayouts[i].setBackgroundResource(R.drawable.bg_quiz_option_unselected);
                optionRadios[i].setImageResource(R.drawable.ic_radio_unchecked);
            }
        }
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(QUESTION_TIMER_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                txtTimer.setText(String.valueOf(secondsLeft));
            }

            @Override
            public void onFinish() {
                txtTimer.setText("0");
                Toast.makeText(QuizActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();
                advanceToNextQuestion();
            }
        }.start();
    }

    private void submitQuestion(boolean withAd) {
        if (selectedOptionIndex == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show();
            return;
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        Question q = questionList.get(currentQuestionIndex);
        if (selectedOptionIndex == q.correctIndex) {
            score++;
            txtScore.setText("Score: " + score);
        }

        if (withAd && rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                Log.d(TAG, "Ad reward watched!");
            });

            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    loadRewardedAd();
                    advanceToNextQuestion();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    advanceToNextQuestion();
                }
            });
        } else {
            advanceToNextQuestion();
        }
    }

    private void advanceToNextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < TOTAL_QUESTIONS) {
            displayQuestion(currentQuestionIndex);
        } else {
            completeQuiz();
        }
    }

    private void completeQuiz() {
        if (isQuizCompleted) return;
        isQuizCompleted = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Credit 25 Coins and 10 Tickets as explicitly requested by user
        String uid = userPref.getUid();
        if (uid != null && !uid.isEmpty()) {
            db.collection("users").document(uid).update(
                    "coins", FieldValue.increment(REWARD_COINS),
                    "tickets", FieldValue.increment(REWARD_TICKETS)
            ).addOnFailureListener(e -> Log.e(TAG, "Error updating Firestore quiz reward", e));
        }

        userPref.addCoins(REWARD_COINS);
        userPref.setTickets(userPref.getTickets() + REWARD_TICKETS);
        userPref.increaseQuizCount();

        UserRepository.getInstance(this).refreshCurrentUser();

        showRewardResultDialog(REWARD_COINS, REWARD_TICKETS);
    }

    private void showRewardResultDialog(int coins, int tickets) {
        if (isFinishing() || isDestroyed()) return;

        try {
            Dialog d = new Dialog(this);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setContentView(R.layout.dialog_spin_result);

            TextView txtTitle = d.findViewById(R.id.txtTitle);
            TextView txtWinAmount = d.findViewById(R.id.txtWinAmount);
            LinearLayout layoutWinTicket = d.findViewById(R.id.layoutWinTicket);
            TextView txtWinTicketAmount = d.findViewById(R.id.txtWinTicketAmount);
            TextView txtCurrentBalance = d.findViewById(R.id.txtCurrentBalance);
            MaterialButton btnOk = d.findViewById(R.id.btnOk);

            if (txtTitle != null) {
                txtTitle.setText("Daily Quiz Completed! 🎉");
            }

            if (txtWinAmount != null) {
                txtWinAmount.setText("+" + coins + " Coins");
            }

            if (layoutWinTicket != null && txtWinTicketAmount != null) {
                layoutWinTicket.setVisibility(View.VISIBLE);
                txtWinTicketAmount.setText("+" + tickets + " Tickets");
            }

            if (txtCurrentBalance != null) {
                txtCurrentBalance.setText("Balance: " + userPref.getCoins() + " Coins | " + userPref.getTickets() + " Tickets");
            }

            if (btnOk != null) {
                btnOk.setOnClickListener(v -> {
                    try {
                        d.dismiss();
                    } catch (Exception ignored) {}
                    finish();
                });
            }

            d.setOnDismissListener(dialog -> finish());

            if (d.getWindow() != null) {
                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            SuccessAnimationHelper.animate(d);
            d.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing quiz reward dialog: " + e.getMessage());
            finish();
        }
    }

    private void loadBannerAd() {
        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
