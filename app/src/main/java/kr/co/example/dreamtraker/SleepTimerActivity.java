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

public class SleepTimerActivity extends AppCompatActivity {

    private TextView tvTimer, tvSleepDuration;
    private Button btnStart, btnViewHistory;
    private Spinner spinnerSound;
    private NumberPicker npHour, npMinute;
    private LinearLayout navSleep, navAsmr, navChart; // 하단 탭 메뉴 View 변수 추가

    private CountDownTimer countDownTimer;
    private MediaPlayer mediaPlayer;
    private boolean isRunning = false;
    private int selectedTotalMinutes = 30;
    private long startTimeMillis = 0;

    // 임시 저장용 (나중에 DB로 바꿀 수 있음)
    // SleepRecord 클래스가 별도로 정의되어 있어야 합니다.
    public static ArrayList<SleepRecord> sleepRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_timer); // XML 파일 이름 확인

        // UI 요소 연결
        tvTimer = findViewById(R.id.tvTimer);
        tvSleepDuration = findViewById(R.id.tvSleepDuration);
        btnStart = findViewById(R.id.btnStart);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        spinnerSound = findViewById(R.id.spinnerSound);
        npHour = findViewById(R.id.npHour);
        npMinute = findViewById(R.id.npMinute);

        // 하단 탭 메뉴 항목 연결 (XML의 LinearLayout ID 사용)
        // XML 구조를 보고 ID가 있다면 해당 ID를 사용하거나, findViewById를 통해 직접 연결해야 합니다.
        // 여기서는 임시적으로 하단 탭 메뉴 항목들을 직접 찾지 않고,
        // 탭 메뉴 리스너를 button_layout이 아닌 하단 탭바 전체에 걸거나,
        // 하단 탭바의 각 항목(LinearLayout)에 ID를 부여했다고 가정하고 진행합니다.
        // 현재 XML에는 하단 탭 메뉴 각 항목에 ID가 없으므로, 예시를 위해 ID가 있다고 가정합니다.
        // **주의: XML에 ID가 없으면 오류 발생**

        // navSleep, navAsmr, navChart는 하단 탭 메뉴의 각 LinearLayout의 ID라고 가정합니다.
        navSleep = findViewById(R.id.navSleep); // @+id/navSleep 가정
        navAsmr = findViewById(R.id.navAsmr);   // @+id/navAsmr 가정
        navChart = findViewById(R.id.navChart); // @+id/navChart 가정


        // NumberPicker 설정
        setupNumberPickers();

        // Spinner 설정 (기존 유지)
        String[] sounds = {"파도", "자연"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item, // R.layout.spinner_item 대신 기본 레이아웃 사용
                sounds
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // 드롭다운 레이아웃도 기본으로 변경
        spinnerSound.setAdapter(adapter);

        // --- 이벤트 리스너 설정 ---

        // 1. 시작/중지 버튼 이벤트
        btnStart.setOnClickListener(v -> {
            if (!isRunning) startSleep();
            else stopSleep();
        });

        // 2. 기록 보기 버튼 이벤트
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, SleepHistoryActivity.class);
            startActivity(intent);
        });

        // 3. 하단 탭 메뉴 이벤트
        setupBottomNavigationListeners();

        // 초기 수면 시간 텍스트 업데이트 (XML 레이아웃 이미지에 맞게 제거)
        // tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", npHour.getValue(), npMinute.getValue()));
    }

    /**
     * NumberPicker의 최소/최대값 설정 및 리스너 연결
     */
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

    /**
     * NumberPicker 값 변경 시 총 분을 계산하고 타이머 텍스트를 업데이트합니다.
     */
    private void updateSelectedTime() {
        int hours = npHour.getValue();
        int minutes = npMinute.getValue();

        selectedTotalMinutes = (hours * 60) + minutes;

        // XML의 상단 00:00 영역 대신, NumberPicker 중앙 텍스트 업데이트 (사용자에게 현재 선택 시간을 보여줌)
        // tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", hours, minutes));

        // 이미지에 있는 "6시간 28분 후에 알람이 울립니다."와 유사한 문구 구현이 필요함.
        // 현재 코드에는 이 TextView가 없으므로, tvSleepDuration을 대체하여 사용합니다.
        if (selectedTotalMinutes > 0) {
            tvSleepDuration.setText(String.format(Locale.getDefault(),
                    "%d시간 %d분 후에 알람이 울립니다.", hours, minutes));
        } else {
            tvSleepDuration.setText("수면 시간을 설정해주세요.");
        }
    }


    /**
     * 수면 시작 로직 (타이머 시작, 미디어 플레이어 시작)
     */
    private void startSleep() {
        if (selectedTotalMinutes <= 0) {
            Toast.makeText(this, "수면 시간을 설정해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        isRunning = true;
        btnStart.setText("중지");
        startTimeMillis = System.currentTimeMillis();

        String selectedSound = spinnerSound.getSelectedItem().toString();
        // R.raw.nature과 R.raw.sea_waves 리소스 ID는 확인이 필요합니다.
        int soundRes = selectedSound.equals("파도") ? R.raw.nature : R.raw.sea_waves;

        // 미디어 플레이어 시작
        try {
            mediaPlayer = MediaPlayer.create(this, soundRes);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (Exception e) {
            Toast.makeText(this, "오디오 리소스를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            // 미디어 플레이어 실패해도 타이머는 계속 진행
        }

        // CountDownTimer 설정: selectedTotalMinutes 사용
        long totalMillis = (long) selectedTotalMinutes * 60 * 1000;
        countDownTimer = new CountDownTimer(totalMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                // 남은 시간을 tvTimer에 표시
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                stopSleep(true); // 타이머 종료로 인한 정지
            }
        }.start();

        // NumberPicker 비활성화
        npHour.setEnabled(false);
        npMinute.setEnabled(false);
    }

    /**
     * 수면 중지 로직 (미디어 중지, 타이머 중지, 기록 저장)
     */
    private void stopSleep(boolean isTimerFinish) {
        isRunning = false;
        btnStart.setText("시작"); // 버튼 텍스트 수정: '수면 시작' -> '시작'

        if (countDownTimer != null) countDownTimer.cancel();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        long endTimeMillis = System.currentTimeMillis();
        long diff = endTimeMillis - startTimeMillis;

        // 수면 시작 후 바로 정지했을 경우 (5초 미만) 기록하지 않음
        if (diff < 5000 && !isTimerFinish) {
            tvSleepDuration.setText("수면 시간을 설정해주세요."); // 텍스트 초기화
            Toast.makeText(this, "수면 기록을 시작하지 않았습니다.", Toast.LENGTH_SHORT).show();
        } else {
            // 기록 로직
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
                    "양호",
                    diff
            ));
            Toast.makeText(this, "수면 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        }

        // NumberPicker 재활성화 및 텍스트 업데이트
        npHour.setEnabled(true);
        npMinute.setEnabled(true);
        updateSelectedTime();
    }

    // 오버로드: 버튼 클릭 시 호출되는 기본 stopSleep (타이머 종료가 아님)
    private void stopSleep() {
        stopSleep(false);
    }

    /**
     * 하단 탭 메뉴 클릭 리스너 설정
     * (XML에 navSleep, navAsmr, navChart ID가 존재해야 합니다.)
     */
    private void setupBottomNavigationListeners() {
        // 수면 (현재 화면)
        navSleep.setOnClickListener(v -> Toast.makeText(this, "수면 타이머 화면입니다.", Toast.LENGTH_SHORT).show());

        // ASMR (다른 액티비티 또는 Fragment로 이동)
        navAsmr.setOnClickListener(v -> {
            Toast.makeText(this, "ASMR 기능으로 이동합니다.", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, AsmrActivity.class);
            // startActivity(intent);
        });

        // 통계 및 기록 (SleepHistoryActivity로 이동)
        navChart.setOnClickListener(v -> {
            Intent intent = new Intent(this, SleepHistoryActivity.class);
            startActivity(intent);
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (countDownTimer != null) countDownTimer.cancel();
    }
}