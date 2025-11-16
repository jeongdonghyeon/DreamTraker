package kr.co.example.dreamtraker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnMusic;
    private Button btnTimer;
    private Button btn_go_stats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 버튼 찾기 (XML ID와 일치해야 함)
        btnMusic = findViewById(R.id.btnMusic);
        btnTimer = findViewById(R.id.btnTimer);
        btn_go_stats = findViewById(R.id.btn_go_stats);

        // 2. 음악 재생 화면으로 이동
        btnMusic.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MusicActivity.class)));

        // 3. 타이머 설정 화면으로 이동
        btnTimer.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SleepTimerActivity.class)));

        // 4. [수정됨] 수면 기록(통계) 화면으로 이동
        // 아까 만든 StatsActivity.class로 연결했습니다.
        btn_go_stats.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, StatsActivity.class)));
    }
}