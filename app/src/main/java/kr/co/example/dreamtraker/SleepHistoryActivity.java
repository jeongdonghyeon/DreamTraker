package kr.co.example.dreamtraker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SleepHistoryActivity extends AppCompatActivity {

    // --- UI 요소 ---
    private RecyclerView rvSleepRecords;
    private SleepRecordAdapter adapter;
    private LinearLayout navSleep, navAsmr, navChart;
    private LinearLayout bottomNavigationBar;
    private LinearLayout llSleepStageContainer;
    private TextView tvSleepStageResult;

    // --- 센서 관련 (백그라운드 유지를 위해 static 사용) ---
    private SensorManager sensorManager;
    private Sensor accelerometer;

    // 리스너와 상태 변수는 static으로 유지하여 화면 전환 시에도 데이터 유지
    private static SensorEventListener sensorEventListener;
    private static boolean isMeasuring = false;
    private static long measureStartMillis = 0L;

    // 뒤척임 감지 변수
    private static int movementSpikes = 0;
    private static float lastX, lastY, lastZ;
    private static boolean isFirstValue = true;
    private static final float MOVEMENT_THRESHOLD = 3.0f;

    // --- 코골이 관련 (MediaRecorder) ---
    private static SoundMeter mSoundMeter = null;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private static int snoringCount = 0;
    private static List<Integer> snoringPerInterval = new ArrayList<>();
    private static final int SNORING_THRESHOLD = 5000; // 소리 크기 임계값
    private static final int CHECK_INTERVAL = 1000;    // 1초마다 체크
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // UI 컨트롤
    private Button btnStartSensor, btnStopSensor;
    private TextView tvSensorStatus, tvSensorTimer, tvMovementCount, tvTotalSleepTime, tvSnoringCount;
    private TextView tvSnoreStartTime, tvSnoreEndTime, tvSnoringTotalCount;

    private final Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    // 차트 뷰
    private LineChart sleepChart;
    private BarChart barChartMovement;
    private BarChart barChartSnoring;

    // 시간대별 데이터 저장용
    private static List<Integer> movementPerInterval = new ArrayList<>();
    private static long lastIntervalTime = 0L;
    private static final long INTERVAL_MILLIS = 10 * 60 * 1000; // 10분 단위 기록

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 엣지 투 엣지 적용 (시스템 바 뒤로 그리기)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.sleep_chart);

        // 1. UI 요소 연결
        rvSleepRecords = findViewById(R.id.rvSleepRecords);
        navSleep = findViewById(R.id.navSleep);
        navAsmr = findViewById(R.id.navAsmr);
        navChart = findViewById(R.id.navChart);
        bottomNavigationBar = findViewById(R.id.bottom_navigation_bar);

        btnStartSensor = findViewById(R.id.btnStartSensor);
        btnStopSensor = findViewById(R.id.btnStopSensor);
        tvSensorStatus = findViewById(R.id.tvSensorStatus);
        tvSensorTimer = findViewById(R.id.tvSensorTimer);
        tvMovementCount = findViewById(R.id.tvMovementCount);
        tvTotalSleepTime = findViewById(R.id.tvTotalSleepTime);
        tvSnoringCount = findViewById(R.id.tvSnoringCount); // 레이아웃에 이 ID가 있어야 함

        llSleepStageContainer = findViewById(R.id.llSleepStageContainer);
        tvSleepStageResult = findViewById(R.id.tvSleepStageResult);

        // 결과 통계 텍스트 뷰 (레이아웃에 존재한다고 가정)
        tvSnoreStartTime = findViewById(R.id.tvSnoreStartTime);
        tvSnoreEndTime = findViewById(R.id.tvSnoreEndTime);
        tvSnoringTotalCount = findViewById(R.id.tvSnoringCount); // 중복 주의, 필요 시 ID 확인

        sleepChart = findViewById(R.id.sleepChart);
        barChartMovement = findViewById(R.id.barChartMovement);
        barChartSnoring = findViewById(R.id.barChartSnoring);

        // 네비게이션 바 패딩 적용 (시스템 바와 겹치지 않게)
        if (bottomNavigationBar != null) applyNavigationBarPadding();

        // 2. RecyclerView 초기화 (데이터 역순 표시)
        List<SleepRecord> records = new ArrayList<>(MainActivity.sleepRecords);
        Collections.reverse(records);
        adapter = new SleepRecordAdapter(records);
        rvSleepRecords.setLayoutManager(new LinearLayoutManager(this));
        rvSleepRecords.setAdapter(adapter);

        // 3. 차트 초기화
        setupLineChart();
        setupBarChartMovement();
        setupBarChartSnoring();
        if (!MainActivity.sleepRecords.isEmpty()) {
            visualizeSleepStages(MainActivity.sleepRecords.get(MainActivity.sleepRecords.size() - 1));
        }

        // 4. 가속도 센서 매니저 준비
        sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // 🌟 [수정됨] 센서 리스너가 없으면 생성 (여기에 핵심 로직 복원)
        if (sensorEventListener == null) {
            setupSensorListener();
        }

        // 버튼 클릭 이벤트
        btnStartSensor.setOnClickListener(v -> startMeasurement());
        btnStopSensor.setOnClickListener(v -> stopMeasurement());

        // 🌟 [수정됨] 하단 탭 이동 기능 복구
        setupBottomNavigationListeners();

        // 5. 화면 복귀 시 상태 복원
        if (isMeasuring) {
            startTimerRunnable();
            // 코골이 체크 핸들러 재시작
            mHandler.removeCallbacks(checkSnoringAndInterval);
            mHandler.post(checkSnoringAndInterval);

            btnStartSensor.setEnabled(false);
            btnStopSensor.setEnabled(true);
            tvSensorStatus.setText("측정중");
        } else {
            // 초기 상태 텍스트
            if (tvSnoringCount != null) tvSnoringCount.setText("코골이: 0 회");
            if (tvMovementCount != null) tvMovementCount.setText("뒤척임: 0 회");
            tvSensorTimer.setText("00:00:00");
        }

        // 권한 체크 (마이크)
        checkPermissions();
    }

    // --- 생명주기 관리 ---

    @Override
    protected void onStart() {
        super.onStart();
        if (isMeasuring) {
            // 화면에 돌아왔을 때 타이머 다시 갱신 시작
            startTimerRunnable();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 화면 꺼지면 UI 갱신만 중단 (센서 및 측정은 백그라운드 static 변수로 계속됨)
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 측정 중이 아닐 때만 센서 해제 (측정 중이면 앱 꺼져도 센서 유지 시도 - 완전한 백그라운드는 서비스 필요하지만 요청사항 반영)
        if (!isMeasuring) {
            if (sensorManager != null && sensorEventListener != null) {
                sensorManager.unregisterListener(sensorEventListener);
            }
            if (mSoundMeter != null) {
                mSoundMeter.stop();
                mSoundMeter = null;
            }
            mHandler.removeCallbacks(checkSnoringAndInterval);
        }
        timerHandler.removeCallbacksAndMessages(null);
    }

    // --- 기능 구현부 ---

    // 🌟 [수정 완료] 하단 네비게이션 탭 이동 기능
    private void setupBottomNavigationListeners() {
        navSleep.setOnClickListener(v -> {
            // MainActivity로 이동
            Intent intent = new Intent(SleepHistoryActivity.this, MainActivity.class);
            // 기존 스택 정리하고 이동하려면 아래 플래그 추가 (선택사항)
            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // 현재 액티비티 종료
        });

        navAsmr.setOnClickListener(v -> {
            // MusicActivity로 이동
            Intent intent = new Intent(SleepHistoryActivity.this, MusicActivity.class);
            startActivity(intent);
            finish(); // 현재 액티비티 종료
        });

        navChart.setOnClickListener(v -> {
            // 현재 화면이므로 토스트만 띄움
            Toast.makeText(this, "현재 수면 분석 화면입니다.", Toast.LENGTH_SHORT).show();
        });
    }

    // 🌟 [수정 완료] 센서 리스너 로직 복원 (가속도 센서 작동)
    private void setupSensorListener() {
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // 측정 중이 아니거나 가속도 센서가 아니면 리턴
                if (!isMeasuring) return;
                if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                if (isFirstValue) {
                    lastX = x;
                    lastY = y;
                    lastZ = z;
                    isFirstValue = false;
                    return;
                }

                float deltaX = Math.abs(x - lastX);
                float deltaY = Math.abs(y - lastY);
                float deltaZ = Math.abs(z - lastZ);

                // 움직임 감지 임계값 체크
                if (deltaX > MOVEMENT_THRESHOLD || deltaY > MOVEMENT_THRESHOLD || deltaZ > MOVEMENT_THRESHOLD) {
                    movementSpikes++;
                    // 로그 확인용 (필요시 주석 해제)
                    // Log.d("Sensor", "Movement detected! Total: " + movementSpikes);
                }

                lastX = x;
                lastY = y;
                lastZ = z;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // 필요 없음
            }
        };
    }

    // 측정 시작
    private void startMeasurement() {
        if (accelerometer == null) {
            Toast.makeText(this, "가속도 센서가 없는 기기입니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isMeasuring) return;

        // 마이크 권한 확인 후 코골이 미터 시작
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (mSoundMeter == null) {
                mSoundMeter = new SoundMeter();
                mSoundMeter.start(this); // Context 전달
            }
        } else {
            Toast.makeText(this, "마이크 권한이 없어 코골이는 측정되지 않습니다.", Toast.LENGTH_SHORT).show();
        }

        // 변수 초기화
        movementSpikes = 0;
        snoringCount = 0;
        movementPerInterval.clear();
        snoringPerInterval.clear();
        isFirstValue = true;

        measureStartMillis = System.currentTimeMillis();
        lastIntervalTime = measureStartMillis; // 구간 측정 시작 시간 초기화
        isMeasuring = true;

        // 센서 리스너 등록
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        // 주기적 체크 (코골이 및 10분 구간 저장) 시작
        mHandler.post(checkSnoringAndInterval);

        // UI 업데이트
        btnStartSensor.setEnabled(false);
        btnStopSensor.setEnabled(true);
        tvSensorStatus.setText("측정중");

        startTimerRunnable();
        Toast.makeText(this, "수면 측정을 시작합니다.", Toast.LENGTH_SHORT).show();
    }

    // 측정 종료
    private void stopMeasurement() {
        if (!isMeasuring) return;

        // 1. 코골이 측정 종료
        if (mSoundMeter != null) {
            mSoundMeter.stop();
            mSoundMeter = null;
        }
        mHandler.removeCallbacks(checkSnoringAndInterval);

        // 2. 가속도 센서 해제
        sensorManager.unregisterListener(sensorEventListener);
        isMeasuring = false;

        // 3. 마지막 구간 데이터 저장
        if (movementSpikes > 0 || snoringCount > 0) {
            movementPerInterval.add(movementSpikes);
            snoringPerInterval.add(snoringCount);
            // 종료 시엔 초기화
            movementSpikes = 0;
            snoringCount = 0;
        }

        // 4. UI 및 타이머 정지
        btnStartSensor.setEnabled(true);
        btnStopSensor.setEnabled(false);
        tvSensorStatus.setText("측정 완료");
        timerHandler.removeCallbacks(timerRunnable);

        // 5. 결과 계산 및 저장
        long endMillis = System.currentTimeMillis();
        long durationMillis = endMillis - measureStartMillis;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        String date = dateFormat.format(new Date(measureStartMillis));
        String startTime = timeFormat.format(new Date(measureStartMillis));
        String endTime = timeFormat.format(new Date(endMillis));

        long totalSeconds = durationMillis / 1000;
        int hours = (int) (totalSeconds / 3600);
        int minutes = (int) ((totalSeconds % 3600) / 60);
        String durationStr = hours + "시간 " + minutes + "분";

        // 총 뒤척임 수 합산 (구간별 데이터 합산)
        int totalMovement = 0;
        for (int m : movementPerInterval) totalMovement += m;

        // SleepRecord 객체 생성 및 저장
        SleepRecord rec = new SleepRecord(date, startTime, endTime, durationStr, "뒤척임 " + totalMovement + "회", durationMillis);
        MainActivity.sleepRecords.add(rec);

        // 영구 저장 (SharedPreferences)
        saveSleepRecords();

        // 6. 결과 화면 갱신
        List<SleepRecord> records = new ArrayList<>(MainActivity.sleepRecords);
        Collections.reverse(records);
        adapter = new SleepRecordAdapter(records);
        rvSleepRecords.setAdapter(adapter);

        setupLineChart();
        setupBarChartMovement();
        setupBarChartSnoring();
        visualizeSleepStages(rec);

        tvTotalSleepTime.setText(durationStr);
        Toast.makeText(this, "수면 데이터가 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    // 타이머 UI 업데이트 (1초마다)
    private void startTimerRunnable() {
        timerHandler.removeCallbacks(timerRunnable);
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isMeasuring) return;

                long diff = System.currentTimeMillis() - measureStartMillis;
                int seconds = (int) (diff / 1000) % 60;
                int minutes = (int) ((diff / 1000) / 60);
                int hours = minutes / 60;
                minutes = minutes % 60;

                tvSensorTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                tvMovementCount.setText("뒤척임: " + movementSpikes + " 회");
                if (tvSnoringCount != null) {
                    tvSnoringCount.setText("코골이: " + snoringCount + " 회");
                }

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    // 코골이 체크 및 10분 구간 저장 로직
    private Runnable checkSnoringAndInterval = new Runnable() {
        @Override
        public void run() {
            if (!isMeasuring) return;

            // 1. 코골이 체크
            if (mSoundMeter != null) {
                double amplitude = mSoundMeter.getAmplitude();
                if (amplitude > SNORING_THRESHOLD) {
                    snoringCount++;
                }
            }

            // 2. 10분 구간 체크
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastIntervalTime >= INTERVAL_MILLIS) {
                snoringPerInterval.add(snoringCount);
                movementPerInterval.add(movementSpikes);

                // 구간 리셋
                snoringCount = 0;
                movementSpikes = 0;
                lastIntervalTime = currentTime;
            }

            mHandler.postDelayed(this, CHECK_INTERVAL);
        }
    };

    // 데이터 영구 저장
    private void saveSleepRecords() {
        SharedPreferences sharedPreferences = getSharedPreferences("DreamTrackerData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(MainActivity.sleepRecords);
        editor.putString("sleep_history_list", json);
        editor.apply();
    }

    // 권한 요청
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "마이크 권한 승인됨", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "권한이 없어 코골이 측정이 불가능합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void applyNavigationBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationBar, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset + (int)(12 * getResources().getDisplayMetrics().density));
            return insets;
        });
    }

    // --- 차트 설정 메소드들 (변경 없음, 그대로 사용) ---

    private void setupBarChartSnoring() {
        if (barChartSnoring == null) return;
        List<BarEntry> entries = new ArrayList<>();
        int totalSnore = 0;
        for (int i = 0; i < snoringPerInterval.size(); i++) {
            int count = snoringPerInterval.get(i);
            totalSnore += count;
            entries.add(new BarEntry(i, count));
        }

        if (tvSnoringTotalCount != null) tvSnoringTotalCount.setText(totalSnore + "회");
        if (!MainActivity.sleepRecords.isEmpty()) {
            SleepRecord lastRecord = MainActivity.sleepRecords.get(MainActivity.sleepRecords.size() - 1);
            if (tvSnoreStartTime != null) tvSnoreStartTime.setText(lastRecord.getStartTime());
            if (tvSnoreEndTime != null) tvSnoreEndTime.setText(lastRecord.getEndTime());
        }

        if (entries.isEmpty()) {
            barChartSnoring.setNoDataText("코골이 데이터가 없습니다.");
            barChartSnoring.setNoDataTextColor(Color.WHITE);
            barChartSnoring.invalidate(); // 갱신 필요
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "코골이");
        dataSet.setColor(Color.parseColor("#9FA8DA"));
        dataSet.setDrawValues(false);
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);
        barChartSnoring.setData(barData);

        // 차트 스타일링
        barChartSnoring.setTouchEnabled(false);
        barChartSnoring.getDescription().setEnabled(false);
        barChartSnoring.getLegend().setEnabled(false);
        XAxis xAxis = barChartSnoring.getXAxis();
        xAxis.setDrawGridLines(false); xAxis.setDrawAxisLine(false); xAxis.setDrawLabels(false);
        barChartSnoring.getAxisLeft().setDrawGridLines(false);
        barChartSnoring.getAxisLeft().setDrawAxisLine(false);
        barChartSnoring.getAxisLeft().setDrawLabels(false);
        barChartSnoring.getAxisLeft().setAxisMinimum(0f);
        barChartSnoring.getAxisRight().setEnabled(false);
        barChartSnoring.invalidate();
    }

    private void setupBarChartMovement() {
        if (barChartMovement == null) return;
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < movementPerInterval.size(); i++) {
            entries.add(new BarEntry(i, movementPerInterval.get(i)));
        }

        if (entries.isEmpty()) {
            barChartMovement.setNoDataText("데이터 없음");
            barChartMovement.setNoDataTextColor(Color.WHITE);
            barChartMovement.invalidate();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "뒤척임");
        dataSet.setColor(Color.parseColor("#9187A5"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barChartMovement.setData(barData);

        XAxis xAxis = barChartMovement.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

        barChartMovement.getAxisLeft().setTextColor(Color.WHITE);
        barChartMovement.getAxisRight().setEnabled(false);
        barChartMovement.getDescription().setEnabled(false);
        barChartMovement.getLegend().setEnabled(false);
        barChartMovement.invalidate();
    }

    private void setupLineChart() {
        if (sleepChart == null) return;
        List<SleepRecord> records = MainActivity.sleepRecords;
        if (records.isEmpty()) {
            sleepChart.setNoDataText("수면 기록이 없습니다.");
            sleepChart.setNoDataTextColor(Color.WHITE);
            return;
        }

        List<Entry> entries = new ArrayList<>();
        final ArrayList<String> dates = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            SleepRecord record = records.get(i);
            float hours = record.getDurationMillis() / (1000f * 60f * 60f);
            entries.add(new Entry(i, hours));
            if(record.getDate().length() >= 5)
                dates.add(record.getDate().substring(5));
            else
                dates.add(record.getDate());
        }

        LineDataSet dataSet = new LineDataSet(entries, "수면 시간");
        dataSet.setColor(Color.parseColor("#A797FF"));
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#A797FF"));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setLineWidth(2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#805F4B8B"));

        LineData lineData = new LineData(dataSet);
        sleepChart.setData(lineData);

        XAxis xAxis = sleepChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#AAAAAA"));
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 0 && value < dates.size()) return dates.get((int) value);
                return "";
            }
        });

        sleepChart.getAxisLeft().setTextColor(Color.parseColor("#AAAAAA"));
        sleepChart.getAxisRight().setEnabled(false);
        sleepChart.getDescription().setEnabled(false);
        sleepChart.getLegend().setEnabled(false);
        sleepChart.invalidate();
    }

    private void visualizeSleepStages(SleepRecord record) {
        llSleepStageContainer.removeAllViews();
        long totalMinutes = record.getDurationMillis() / (1000 * 60);

        if (totalMinutes <= 0) {
            tvSleepStageResult.setText("수면 데이터가 부족합니다.");
            return;
        }

        tvSleepStageResult.setText("수면 단계 분석 (총 " + totalMinutes + "분)");

        // 간단한 알고리즘으로 단계 추정
        int movementMinutes = 0;
        try {
            movementMinutes = Integer.parseInt(record.getSleepQuality().replaceAll("[^0-9]", "")) * 5;
        } catch (Exception e) { movementMinutes = 0; }

        movementMinutes = Math.min((int)(totalMinutes * 0.4), movementMinutes); // 최대 40% 제한
        int remainingMin = (int)totalMinutes - movementMinutes;

        int deepSleepMin = (int) (remainingMin * 0.45);
        int remSleepMin = (int) (remainingMin * 0.35);
        int lightSleepMin = remainingMin - deepSleepMin - remSleepMin;
        int awakeMin = movementMinutes;

        // 그래프 그리기
        LinearLayout barLayout = new LinearLayout(this);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(24));
        barParams.setMargins(0, dpToPx(16), 0, dpToPx(16));
        barLayout.setLayoutParams(barParams);
        barLayout.setOrientation(LinearLayout.HORIZONTAL);

        addStageView(barLayout, deepSleepMin, "#5F4B8B", totalMinutes);
        addStageView(barLayout, lightSleepMin, "#9187A5", totalMinutes);
        addStageView(barLayout, remSleepMin, "#B0A8B9", totalMinutes);
        addStageView(barLayout, awakeMin, "#E0E0E0", totalMinutes);
        llSleepStageContainer.addView(barLayout);

        addLegendText("깊은 수면", deepSleepMin, "#5F4B8B");
        addLegendText("얕은 수면", lightSleepMin, "#9187A5");
        addLegendText("렘 수면", remSleepMin, "#B0A8B9");
        addLegendText("뒤척임/깸", awakeMin, "#E0E0E0");
    }

    private void addStageView(LinearLayout parent, int minutes, String colorHex, long totalMinutes) {
        if (minutes <= 0) return;
        View view = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (float) minutes / totalMinutes);
        view.setLayoutParams(params);
        view.setBackgroundColor(Color.parseColor(colorHex));
        parent.addView(view);
    }

    private void addLegendText(String label, int minutes, String colorHex) {
        if (minutes <= 0) return;
        TextView tv = new TextView(this);
        tv.setText("● " + label + ": " + minutes + "분");
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTextSize(14);
        tv.setPadding(0, dpToPx(4), 0, 0);
        llSleepStageContainer.addView(tv);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // --- SoundMeter Helper Class ---
    public static class SoundMeter {
        private MediaRecorder mRecorder = null;
        private File tempFile = null;

        public void start(Context context) {
            if (mRecorder == null) {
                try {
                    File cacheDir = context.getCacheDir();
                    tempFile = File.createTempFile("temp_audio", ".3gp", cacheDir);

                    mRecorder = new MediaRecorder();
                    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mRecorder.setOutputFile(tempFile.getAbsolutePath());

                    mRecorder.prepare();
                    mRecorder.start();
                } catch (IOException | RuntimeException e) {
                    mRecorder = null;
                    e.printStackTrace();
                }
            }
        }

        public void stop() {
            if (mRecorder != null) {
                try {
                    mRecorder.stop();
                } catch (RuntimeException e) {
                    // 녹음 시간이 너무 짧거나 실패 시 무시
                }
                mRecorder.release();
                mRecorder = null;
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }

        public double getAmplitude() {
            if (mRecorder != null) {
                try {
                    return mRecorder.getMaxAmplitude();
                } catch (RuntimeException e) {
                    return 0;
                }
            }
            return 0;
        }
    }
}