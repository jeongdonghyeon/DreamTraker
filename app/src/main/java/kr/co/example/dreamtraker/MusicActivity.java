package kr.co.example.dreamtraker;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MusicActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;

    // UI 요소 변수 (XML ID와 일치하도록 수정)
    private Button btnDarkNature, btnCalmWaves, btnStopAsmr;
    private TextView tvSelectMusicHint;

    // 하단 탭 메뉴 변수
    private LinearLayout navSleep, navAsmr, navChart;

    private int currentPlayingResId = -1; // 현재 재생 중인 ASMR 리소스 ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 레이아웃 파일명을 activity_asmr.xml로 가정하고 연결합니다.
        setContentView(R.layout.activity_music);

        // 1. UI 요소 연결 (findViewById)
        btnDarkNature = findViewById(R.id.btnDarkNature);
        btnCalmWaves = findViewById(R.id.btnCalmWaves);
        btnStopAsmr = findViewById(R.id.btnStopAsmr);
        tvSelectMusicHint = findViewById(R.id.tvSelectMusicHint);

        // 2. 하단 탭 메뉴 연결
        navSleep = findViewById(R.id.navSleep);
        navAsmr = findViewById(R.id.navAsmr);
        navChart = findViewById(R.id.navChart);

        // 3. 버튼 리스너 설정
        btnDarkNature.setOnClickListener(v -> playSound(R.raw.nature));
        btnCalmWaves.setOnClickListener(v -> playSound(R.raw.sea_waves));
        btnStopAsmr.setOnClickListener(v -> stopSound());

        // 4. 하단 내비게이션 리스너 설정
        setupBottomNavigationListeners();

        // 5. 초기 UI 상태 설정 (힌트 텍스트 표시)
        tvSelectMusicHint.setVisibility(View.VISIBLE);
    }

    // ------------------- ASMR 재생 로직 -------------------

    private void playSound(int soundResId) {
        // 이미 같은 ASMR이 재생 중이면 무시
        if (mediaPlayer != null && mediaPlayer.isPlaying() && currentPlayingResId == soundResId) {
            Toast.makeText(this, "이미 재생 중입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        stopSound(false); // 기존 사운드를 조용히 중지 (토스트 메시지 없음)

        try {
            mediaPlayer = MediaPlayer.create(this, soundResId);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            currentPlayingResId = soundResId;

            tvSelectMusicHint.setVisibility(View.GONE);
            Toast.makeText(this, "ASMR 재생 시작", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "ASMR 파일을 찾거나 재생할 수 없습니다. R.raw 리소스를 확인하세요.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopSound(boolean showToast) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            currentPlayingResId = -1;
            tvSelectMusicHint.setVisibility(View.VISIBLE);

            if (showToast) {
                Toast.makeText(this, "ASMR 재생 중지", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 정지 버튼 클릭 시 호출되는 오버로드 메서드
    private void stopSound() {
        stopSound(true); // Toast 메시지 표시
    }

    // ------------------- 하단 탭 메뉴 로직 -------------------

    private void setupBottomNavigationListeners() {
        // 1. 수면 (MainActivity로 이동)
        navSleep.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // 2. ASMR (현재 화면)
        navAsmr.setOnClickListener(v -> {
            Toast.makeText(this, "현재 ASMR 화면입니다.", Toast.LENGTH_SHORT).show();
        });

        // 3. 통계 및 기록 (SleepHistoryActivity로 이동)
        navChart.setOnClickListener(v -> {
            startActivity(new Intent(this, SleepHistoryActivity.class));
            finish();
        });
    }

    // ------------------- 생명 주기 정리 -------------------

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSound(false); // 액티비티 종료 시 미디어는 조용히 정리
    }
}