package kr.co.example.dreamtraker;

import android.content.Intent;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

// 이 Activity가 이제 메인 화면(수면 타이머) 역할을 합니다.
public class MainActivity extends AppCompatActivity {

    // UI 요소 변수
    private TextView tvTimer, tvSleepDuration;
    private Button btnStart, btnViewHistory;
    private Spinner spinnerSound;
    private NumberPicker npHour, npMinute;
    private LinearLayout navSleep, navAsmr, navChart;

    // 타이머 및 미디어 로직 변수
    private CountDownTimer countDownTimer;
    private MediaPlayer mediaPlayer;
    private boolean isRunning = false;
    private int selectedTotalMinutes = 30; // 초기 설정 시간 (30분)
    private long startTimeMillis = 0;

    // 기록 저장소 (SleepRecord.java는 별도로 존재해야 합니다)
    public static ArrayList<SleepRecord> sleepRecords = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🌟 레이아웃을 activity_main.xml로 설정합니다.
        setContentView(R.layout.activity_main);

        // 1. UI 요소 연결 (findViewById)
        tvTimer = findViewById(R.id.tvTimer);
        tvSleepDuration = findViewById(R.id.tvSleepDuration);
        btnStart = findViewById(R.id.btnStart);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        spinnerSound = findViewById(R.id.spinnerSound);
        npHour = findViewById(R.id.npHour);
        npMinute = findViewById(R.id.npMinute);

        // 하단 탭 메뉴 항목 연결
        navSleep = findViewById(R.id.navSleep);
        navAsmr = findViewById(R.id.navAsmr);
        navChart = findViewById(R.id.navChart);

        // 2. 초기 설정
        setupNumberPickers();
        setupSpinner();

        // 3. 이벤트 리스너 설정
        btnStart.setOnClickListener(v -> {
            if (!isRunning) startSleep();
            else stopSleep();
        });

        // 🚨 이 부분 수정: '기록 보기' 버튼 클릭 시 SleepRecordListActivity로 이동
        btnViewHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, SleepRecordListActivity.class));
        });

        setupBottomNavigationListeners();
    }

    // ------------------- 초기 설정 메서드 -------------------

    private void setupSpinner() {
        String[] sounds = {"파도", "자연"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item, // 기본 레이아웃 사용
                sounds
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // 기본 드롭다운 사용
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

        NumberPicker.OnValueChangeListener listener = (picker, oldVal, newVal) -> updateSelectedTime();
        npHour.setOnValueChangedListener(listener);
        npMinute.setOnValueChangedListener(listener);

        updateSelectedTime();
    }

    // ------------------- 로직 및 업데이트 메서드 -------------------

    private void updateSelectedTime() {
        int hours = npHour.getValue();
        int minutes = npMinute.getValue();
        selectedTotalMinutes = (hours * 60) + minutes;

        // 상단 '00:00' 텍스트 업데이트 (선택된 시간 표시)
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", hours, minutes));

        // 하단 문구 업데이트
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

        String selectedSound = spinnerSound.getSelectedItem().toString();
        // R.raw.nature과 R.raw.sea_waves 리소스 ID는 프로젝트에 존재해야 합니다.
        int soundRes = selectedSound.equals("파도") ? R.raw.nature : R.raw.sea_waves;

        try {
            mediaPlayer = MediaPlayer.create(this, soundRes);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (Exception e) {
            Toast.makeText(this, "오디오 리소스를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        }

        // CountDownTimer 설정
        long totalMillis = (long) selectedTotalMinutes * 60 * 1000;
        countDownTimer = new CountDownTimer(totalMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                stopSleep(true);
            }
        }.start();

        npHour.setEnabled(false);
        npMinute.setEnabled(false);
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

        // 기록 로직
        if (diff < 5000 && !isTimerFinish) { // 5초 미만은 기록 저장 안 함
            tvSleepDuration.setText("수면 시간을 설정해주세요.");
        } else {
            long totalSeconds = diff / 1000;
            int hours = (int) (totalSeconds / 3600);
            int minutes = (int) ((totalSeconds % 3600) / 60);

            String duration = hours + "시간 " + minutes + "분";
            tvSleepDuration.setText("측정된 수면 시간: " + duration);

            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(startTimeMillis));
            String startTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(startTimeMillis));
            String endTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(endTimeMillis));

            // SleepRecord 클래스가 정의되어 있다고 가정
            sleepRecords.add(new SleepRecord(
                    date,
                    startTime,
                    endTime,
                    duration,
                    "양호", // 수면 질은 임시로 '양호'로 설정
                    diff
            ));
            Toast.makeText(this, "수면 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        }

        npHour.setEnabled(true);
        npMinute.setEnabled(true);
        updateSelectedTime();
    }

    private void stopSleep() {
        stopSleep(false);
    }

    /**
     * 하단 탭 메뉴 클릭 리스너 설정
     */
    private void setupBottomNavigationListeners() {
        // 1. 수면 (현재 화면)
        navSleep.setOnClickListener(v -> Toast.makeText(this, "현재 수면 타이머 화면입니다.", Toast.LENGTH_SHORT).show());

        // 2. ASMR (MusicActivity로 이동)
        navAsmr.setOnClickListener(v -> {
            startActivity(new Intent(this, MusicActivity.class));
            // MusicActivity로 이동 시 MainActivity를 종료하지 않아 뒤로가기를 통해 쉽게 돌아올 수 있도록 합니다.
        });

        // 3. 통계 및 기록 (SleepHistoryActivity로 이동)
        navChart.setOnClickListener(v -> {
            // 🚨 하단 탭은 통계가 포함된 SleepHistoryActivity로 이동합니다.
            startActivity(new Intent(this, SleepHistoryActivity.class));
        });
    }

    // ------------------- 생명 주기 메서드 -------------------

    @Override
    protected void onDestroy() {
        super.onDestroy(); // 🌟 필수 호출
        // 액티비티가 파괴될 때 미디어와 타이머를 해제하여 메모리 누수를 방지합니다.
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (countDownTimer != null) countDownTimer.cancel();
    }
}