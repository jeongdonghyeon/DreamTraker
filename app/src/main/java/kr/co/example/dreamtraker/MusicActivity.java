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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat.Type;


public class MusicActivity extends AppCompatActivity {

    // 🌟 1. MediaPlayer를 static으로 변경하여 Activity가 파괴되어도 재생이 유지되도록 합니다.
    private static MediaPlayer mediaPlayer;

    // UI 요소 변수
    private Button btnDarkNature, btnCalmWaves, btnStopAsmr;
    private TextView tvSelectMusicHint;

    // 하단 탭 메뉴 변수
    private LinearLayout navSleep, navAsmr, navChart;
    private LinearLayout bottomNavigationBar; // <-- 시스템 Insets 처리를 위해 추가

    private static int currentPlayingResId = -1; // 🌟 static으로 변경하여 재생 상태를 유지합니다.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Edge-to-Edge 설정: 뷰가 시스템 바 영역까지 확장되도록 설정합니다.
        // 이 설정을 해야 시스템 바 영역의 Insets 정보를 받을 수 있습니다.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_music); // XML 파일명 확인

        // 2. UI 요소 연결
        btnDarkNature = findViewById(R.id.btnDarkNature);
        btnCalmWaves = findViewById(R.id.btnCalmWaves);
        btnStopAsmr = findViewById(R.id.btnStopAsmr);
        tvSelectMusicHint = findViewById(R.id.tvSelectMusicHint);

        // 3. 하단 탭 메뉴 연결 및 Insets 처리 대상 뷰 연결
        navSleep = findViewById(R.id.navSleep);
        navAsmr = findViewById(R.id.navAsmr);
        navChart = findViewById(R.id.navChart);
        bottomNavigationBar = findViewById(R.id.bottom_navigation_bar); // <-- 하단 바 레이아웃 연결

        // 🌟 🌟 🌟 4. 시스템 Insets (내비게이션 바 높이) 동적 처리 로직 추가 🌟 🌟 🌟
        applyNavigationBarPadding();

        // 5. 버튼 리스너 설정
        btnDarkNature.setOnClickListener(v -> playSound(R.raw.nature));
        btnCalmWaves.setOnClickListener(v -> playSound(R.raw.sea_waves));
        btnStopAsmr.setOnClickListener(v -> stopSound());

        // 6. 하단 내비게이션 리스너 설정
        setupBottomNavigationListeners();

        // 7. onCreate에서는 초기화만 수행하고, 상태 복원은 onStart에서 합니다.
    }

    // ------------------- Insets 처리 전용 메서드 -------------------

    private void applyNavigationBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationBar, (v, insets) -> {

            // 시스템 바 (상단 상태 표시줄 + 하단 내비게이션 바)의 Insets 정보를 가져옵니다.
            int bottomInset = insets.getInsets(Type.systemBars()).bottom;

            // 기존의 패딩 값을 가져옵니다.
            int topPadding = v.getPaddingTop();
            int leftPadding = v.getPaddingLeft();
            int rightPadding = v.getPaddingRight();

            // XML에서 paddingTop이 12dp로 설정되어 있으므로,
            // 원래 의도했던 수직 여백을 대칭적으로 유지하기 위해 12dp를 px로 변환하여 더합니다.
            // XML에서 paddingBottom을 0dp로 설정했다고 가정합니다.
            float density = getResources().getDisplayMetrics().density;
            int originalBottomPaddingDp = 12; // XML에서 paddingTop과 대칭적으로 유지하고 싶은 값
            int originalBottomPaddingPx = (int) (originalBottomPaddingDp * density);

            // 시스템 내비게이션 바 높이 + 원래 의도한 하단 여백을 새로운 하단 패딩으로 설정
            int dynamicBottomPadding = bottomInset + originalBottomPaddingPx;

            v.setPadding(
                    leftPadding,
                    topPadding,
                    rightPadding,
                    dynamicBottomPadding
            );

            // Insets을 소비(consume)하지 않고 반환하여 다른 뷰에도 적용될 수 있게 합니다.
            return insets;
        });
    }

    // ------------------- (이하 생략: 기존 로직 그대로 유지) -------------------

    // 🌟 🌟 🌟 생명 주기 메서드 추가 (상태 복원) 🌟 🌟 🌟
    @Override
    protected void onStart() {
        super.onStart();
        // Activity가 화면에 나타날 때마다 재생 상태를 확인하고 UI를 업데이트합니다.
        checkAndRestoreState();
    }

    /**
     * 현재 MediaPlayer의 상태를 확인하여 UI를 업데이트합니다.
     */
    private void checkAndRestoreState() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            tvSelectMusicHint.setVisibility(View.GONE);
            // 필요하다면 여기서 어떤 버튼이 눌려있는지 시각적으로 표시하는 로직 추가 가능
        } else {
            tvSelectMusicHint.setVisibility(View.VISIBLE);
        }
    }


    // ------------------- ASMR 재생 로직 -------------------

    private void playSound(int soundResId) {
        // 이미 같은 ASMR이 재생 중이면 무시
        if (mediaPlayer != null && mediaPlayer.isPlaying() && currentPlayingResId == soundResId) {
            Toast.makeText(this, "이미 재생 중입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        stopSound(false); // 기존 사운드를 조용히 중지

        try {
            // 🌟 mediaPlayer가 static이지만, null일 경우에만 새로 생성합니다.
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, soundResId);
            } else {
                // 만약 이전에 release 되지 않고 stop만 된 경우, 새 리소스로 재설정해야 합니다.
                mediaPlayer.reset();
                mediaPlayer = MediaPlayer.create(this, soundResId);
            }

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
        // ASMR 재생을 유지하도록 설계되었으므로, 여기서 release하지 않습니다.
    }
}