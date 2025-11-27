package kr.co.example.dreamtraker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class SleepHistoryActivity extends AppCompatActivity {

    private RecyclerView rvSleepRecords;
    private SleepRecordAdapter adapter;
    private TextView tvNoRecords; // 기록이 없을 때 보여줄 TextView (XML에 없지만 추가 구현)

    // 하단 탭 메뉴
    private LinearLayout navSleep, navAsmr, navChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🌟 레이아웃 파일을 sleep_chart.xml로 설정합니다.
        setContentView(R.layout.sleep_chart);

        // UI 요소 연결
        rvSleepRecords = findViewById(R.id.rvSleepRecords);
        // tvNoRecords = findViewById(R.id.tvNoRecords); // XML에 추가해야 사용 가능

        // 하단 탭 메뉴 연결
        navSleep = findViewById(R.id.navSleep);
        navAsmr = findViewById(R.id.navAsmr);
        navChart = findViewById(R.id.navChart);

        // 1. 데이터 가져오기 (MainActivity의 static 리스트 참조)
        List<SleepRecord> records = MainActivity.sleepRecords;

        // 최신 기록이 위에 오도록 리스트를 뒤집습니다.
        Collections.reverse(records);

        // 2. RecyclerView 설정
        if (records.isEmpty()) {
            Toast.makeText(this, "저장된 수면 기록이 없습니다.", Toast.LENGTH_LONG).show();
            // if (tvNoRecords != null) tvNoRecords.setVisibility(View.VISIBLE);
            // rvSleepRecords.setVisibility(View.GONE);
        } else {
            // if (tvNoRecords != null) tvNoRecords.setVisibility(View.GONE);
            // rvSleepRecords.setVisibility(View.VISIBLE);

            adapter = new SleepRecordAdapter(records);
            rvSleepRecords.setLayoutManager(new LinearLayoutManager(this));
            rvSleepRecords.setAdapter(adapter);
        }

        // 3. 하단 내비게이션 리스너 설정
        setupBottomNavigationListeners();
    }

    /**
     * 하단 탭 메뉴 클릭 리스너 설정
     */
    private void setupBottomNavigationListeners() {
        // 1. 수면 (MainActivity로 이동)
        navSleep.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // 2. ASMR (MusicActivity로 이동)
        navAsmr.setOnClickListener(v -> {
            startActivity(new Intent(this, MusicActivity.class));
            finish();
        });

        // 3. 통계 및 기록 (현재 화면)
        navChart.setOnClickListener(v -> {
            Toast.makeText(this, "현재 통계 및 기록 화면입니다.", Toast.LENGTH_SHORT).show();
        });
    }

    // 통계 화면에서는 보통 데이터가 변경되지 않으므로 onResume/onDestroy 정리는 생략합니다.
}