package kr.co.example.dreamtraker;

import android.content.Intent;
import android.content.SharedPreferences; // 💾 추가
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat.Type;

import com.google.gson.Gson; // 💾 추가
import com.google.gson.reflect.TypeToken; // 💾 추가

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvTimer, tvSleepDuration;
    private Button btnStart, btnViewHistory;
    private Spinner spinnerSound;
    private NumberPicker npHour, npMinute;
    private LinearLayout navSleep, navAsmr, navChart;
    private LinearLayout bottomNavigationBar;

    private CountDownTimer countDownTimer;
    private static MediaPlayer mediaPlayer;
    private static boolean isRunning = false;
    private static long startTimeMillis = 0;
    private static long targetEndTimeMillis = 0;
    private static int selectedTotalMinutes = 30;

    public static ArrayList<SleepRecord> sleepRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        // 💾 1. 앱 실행 시 저장된 데이터 불러오기
        loadSleepRecords();

        tvTimer = findViewById(R.id.tvTimer);
        tvSleepDuration = findViewById(R.id.tvSleepDuration);
        btnStart = findViewById(R.id.btnStart);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        spinnerSound = findViewById(R.id.spinnerSound);
        npHour = findViewById(R.id.npHour);
        npMinute = findViewById(R.id.npMinute);
        navSleep = findViewById(R.id.navSleep);
        navAsmr = findViewById(R.id.navAsmr);
        navChart = findViewById(R.id.navChart);
        bottomNavigationBar = findViewById(R.id.bottom_navigation_bar);

        applyNavigationBarPadding();
        setupNumberPickers();
        setupSpinner();

        btnStart.setOnClickListener(v -> {
            if (!isRunning) startSleep();
            else stopSleep();
        });

        btnViewHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, SleepRecordListActivity.class));
        });

        setupBottomNavigationListeners();
    }

    private void applyNavigationBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationBar, (v, insets) -> {
            int bottomInset = insets.getInsets(Type.systemBars()).bottom;
            int topPadding = v.getPaddingTop();
            int leftPadding = v.getPaddingLeft();
            int rightPadding = v.getPaddingRight();
            float density = getResources().getDisplayMetrics().density;
            int originalBottomPaddingDp = 12;
            int originalBottomPaddingPx = (int) (originalBottomPaddingDp * density);
            int dynamicBottomPadding = bottomInset + originalBottomPaddingPx;
            v.setPadding(leftPadding, topPadding, rightPadding, dynamicBottomPadding);
            return insets;
        });
    }

    private void setupSpinner() {
        String[] sounds = {"파도", "자연"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sounds
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSound.setAdapter(adapter);
    }

    private void setupNumberPickers() {
        npHour.setMinValue(0);
        npHour.setMaxValue(12);
        npHour.setWrapSelectorWheel(true);
        npHour.setValue(0);

        npMinute.setMinValue(0);
        npMinute.setMaxValue(59);
        npMinute.setWrapSelectorWheel(true);
        npMinute.setValue(30);

        NumberPicker.OnValueChangeListener listener = (picker, oldVal, newVal) -> {
            if (!isRunning) updateSelectedTime();
        };
        npHour.setOnValueChangedListener(listener);
        npMinute.setOnValueChangedListener(listener);

        updateSelectedTime();
    }

    private void updateSelectedTime() {
        int hours = npHour.getValue();
        int minutes = npMinute.getValue();
        selectedTotalMinutes = (hours * 60) + minutes;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:00", hours, minutes));

        if (selectedTotalMinutes > 0) {
            tvSleepDuration.setText(String.format(Locale.getDefault(),
                    "%d시간 %d분 후에 알람이 종료됩니다.", hours, minutes));
        } else {
            tvSleepDuration.setText("수면 시간을 설정해주세요.");
        }
    }

    private void startSleep() {
        if (selectedTotalMinutes <= 0) {
            Toast.makeText(this, "수면 시간을 설정해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        isRunning = true;
        btnStart.setText("중지");
        startTimeMillis = System.currentTimeMillis();
        long durationMillis = (long) selectedTotalMinutes * 60 * 1000;
        targetEndTimeMillis = startTimeMillis + durationMillis;

        String selectedSound = spinnerSound.getSelectedItem().toString();
        int soundRes = selectedSound.equals("파도") ? R.raw.nature : R.raw.sea_waves;

        try {
            mediaPlayer = MediaPlayer.create(this, soundRes);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (Exception e) {
            Toast.makeText(this, "오디오 리소스를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        }

        startCountDownTimer(durationMillis);
        npHour.setEnabled(false);
        npMinute.setEnabled(false);
    }

    private void startCountDownTimer(long millisInFuture) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerUI(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00:00");
                stopSleep(true);
            }
        }.start();
    }

    private void updateTimerUI(long millisUntilFinished) {
        int totalSeconds = (int) (millisUntilFinished / 1000);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void stopSleep(boolean isTimerFinish) {
        isRunning = false;
        btnStart.setText("시작");

        if (countDownTimer != null) countDownTimer.cancel();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        long endTimeMillis = System.currentTimeMillis();
        long diff = endTimeMillis - startTimeMillis;

        if (diff < 5000 && !isTimerFinish) {
            tvSleepDuration.setText("수면 시간을 설정해주세요.");
        } else {
            long totalSeconds = diff / 1000;
            int hours = (int) (totalSeconds / 3600);
            int minutes = (int) ((totalSeconds % 3600) / 60);
            int seconds = (int) (totalSeconds % 60);

            String duration = String.format(Locale.getDefault(), "%d시간 %d분 %d초", hours, minutes, seconds);
            tvSleepDuration.setText("측정된 수면 시간: " + duration);

            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(startTimeMillis));
            String startTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(startTimeMillis));
            String endTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(endTimeMillis));

            sleepRecords.add(new SleepRecord(
                    date,
                    startTime,
                    endTime,
                    duration,
                    "양호",
                    diff
            ));

            // 💾 2. 기록 추가 후 데이터 저장
            saveSleepRecords();

            Toast.makeText(this, "수면 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        }

        npHour.setEnabled(true);
        npMinute.setEnabled(true);
        updateSelectedTime();
    }

    private void stopSleep() {
        stopSleep(false);
    }

    private void setupBottomNavigationListeners() {
        navSleep.setOnClickListener(v -> Toast.makeText(this, "현재 수면 타이머 화면입니다.", Toast.LENGTH_SHORT).show());
        navAsmr.setOnClickListener(v -> startActivity(new Intent(this, MusicActivity.class)));
        navChart.setOnClickListener(v -> startActivity(new Intent(this, SleepHistoryActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndRestoreState();
    }

    private void checkAndRestoreState() {
        if (isRunning) {
            btnStart.setText("중지");
            npHour.setEnabled(false);
            npMinute.setEnabled(false);

            long currentTime = System.currentTimeMillis();
            long remainingMillis = targetEndTimeMillis - currentTime;

            if (remainingMillis > 0) {
                startCountDownTimer(remainingMillis);
            } else {
                stopSleep(true);
            }
        } else {
            btnStart.setText("시작");
            npHour.setEnabled(true);
            npMinute.setEnabled(true);
            updateSelectedTime();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isRunning) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if (countDownTimer != null) countDownTimer.cancel();
        }
    }

    // ------------------- 💾 데이터 저장/로드 메서드 (SharedPreferences + Gson) -------------------

    private void saveSleepRecords() {
        SharedPreferences sharedPreferences = getSharedPreferences("DreamTrackerData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        // 리스트를 JSON 문자열로 변환하여 저장
        String json = gson.toJson(sleepRecords);
        editor.putString("sleep_history_list", json);
        editor.apply();
    }

    private void loadSleepRecords() {
        SharedPreferences sharedPreferences = getSharedPreferences("DreamTrackerData", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("sleep_history_list", null);

        if (json != null) {
            java.lang.reflect.Type type = new TypeToken<ArrayList<SleepRecord>>() {}.getType();
            sleepRecords = gson.fromJson(json, type);
        } else {
            sleepRecords = new ArrayList<>();
        }
    }
}