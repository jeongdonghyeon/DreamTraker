package kr.co.example.dreamtraker;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SleepTimerActivity extends AppCompatActivity {

    private SeekBar seekBarTime;
    private TextView tvSelectedTime, tvTimer, tvSleepDuration;
    private Button btnStart, btnViewHistory;
    private Spinner spinnerSound;

    private CountDownTimer countDownTimer;
    private MediaPlayer mediaPlayer;
    private boolean isRunning = false;
    private int selectedMinutes = 30;

    private long startTimeMillis = 0;

    // 임시 저장용 (나중에 DB로 바꿀 수 있음)
    public static ArrayList<SleepRecord> sleepRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_timer);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        seekBarTime = findViewById(R.id.seekBarTime);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        tvTimer = findViewById(R.id.tvTimer);
        tvSleepDuration = findViewById(R.id.tvSleepDuration);
        btnStart = findViewById(R.id.btnStart);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        spinnerSound = findViewById(R.id.spinnerSound);

        // Spinner 설정
        String[] sounds = {"파도", "자연"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                sounds
        );
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerSound.setAdapter(adapter);


        // SeekBar 이벤트
        seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedMinutes = progress;
                tvSelectedTime.setText("선택: " + selectedMinutes + "분");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 버튼 이벤트
        btnStart.setOnClickListener(v -> {
            if (!isRunning) startSleep();
            else stopSleep();
        });

        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, SleepHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void startSleep() {
        isRunning = true;
        btnStart.setText("중지");
        startTimeMillis = System.currentTimeMillis();

        String selectedSound = spinnerSound.getSelectedItem().toString();
        int soundRes = selectedSound.equals("파도") ? R.raw.nature : R.raw.sea_waves;

        mediaPlayer = MediaPlayer.create(this, soundRes);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        countDownTimer = new CountDownTimer(selectedMinutes * 60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                stopSleep();
            }
        }.start();
    }

    private void stopSleep() {
        isRunning = false;
        btnStart.setText("수면 시작");

        long endTimeMillis = System.currentTimeMillis();
        long diff = endTimeMillis - startTimeMillis;
        int hours = (int) (diff / (1000 * 60 * 60));
        int minutes = (int) ((diff / (1000 * 60)) % 60);

        String duration = hours + "시간 " + minutes + "분";
        tvSleepDuration.setText("수면 시간: " + duration);

        // 날짜 형식
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(startTimeMillis));
        String startTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(startTimeMillis));
        String endTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(endTimeMillis));

        sleepRecords.add(new SleepRecord(date, startTime, endTime, duration));

        if (countDownTimer != null) countDownTimer.cancel();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
