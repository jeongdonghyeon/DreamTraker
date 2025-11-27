package kr.co.example.dreamtraker;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * '기록 보기' 버튼을 통해 진입하는 수면 기록 상세 화면 (이미지 기반).
 */
public class SleepRecordListActivity extends AppCompatActivity {

    private ImageButton btnClose;
    private TextView tvDetailHours, tvDetailMinutes, tvDetailSeconds;
    private LinearLayout sleepStagesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_history);

        // 1. UI 요소 연결
        btnClose = findViewById(R.id.btnClose);
        tvDetailHours = findViewById(R.id.tvDetailHours);
        tvDetailMinutes = findViewById(R.id.tvDetailMinutes);
        tvDetailSeconds = findViewById(R.id.tvDetailSeconds);

        // 2. 닫기 버튼 설정
        btnClose.setOnClickListener(v -> finish());

        // 3. 데이터 로드 및 표시
        loadAndDisplayRecordDetail();
    }

    private void loadAndDisplayRecordDetail() {

        if (MainActivity.sleepRecords.isEmpty()) {
            Toast.makeText(this, "저장된 수면 기록이 없습니다.", Toast.LENGTH_SHORT).show();
            // 기록이 없으면 00:00:00으로 표시
            tvDetailHours.setText("00");
            tvDetailMinutes.setText("00");
            tvDetailSeconds.setText("00");
            return;
        }

        // 1. 가장 최근 기록(마지막 항목)을 가져옵니다.
        SleepRecord latestRecord = MainActivity.sleepRecords.get(MainActivity.sleepRecords.size() - 1);

        // 2. 기록에서 시, 분, 초를 가져와 UI에 표시합니다.
        // Locale.getDefault()를 사용하여 숫자를 2자리 문자열로 포맷합니다.
        tvDetailHours.setText(String.format(Locale.getDefault(), "%02d", latestRecord.getHours()));
        tvDetailMinutes.setText(String.format(Locale.getDefault(), "%02d", latestRecord.getMinutes()));
        tvDetailSeconds.setText(String.format(Locale.getDefault(), "%02d", latestRecord.getSeconds()));

        Toast.makeText(this, latestRecord.getDuration() + " 기록을 로드했습니다.", Toast.LENGTH_SHORT).show();

        // 3. 수면 단계 데이터 표시 로직 (현재는 XML에 정적으로 include 되어 있음)
        // 만약 동적으로 단계를 표시하려면 여기에 코드를 추가해야 합니다.
    }

    // 이 액티비티는 하단 탭 메뉴가 없으므로 onDestroy는 생략합니다.
}