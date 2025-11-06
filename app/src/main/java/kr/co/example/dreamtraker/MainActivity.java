package kr.co.example.dreamtraker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnMusic;
    private Button btnTimer;
    private Button btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 이 레이아웃이 위 XML과 동일한 파일명인지 확인

        // 반드시 setContentView 다음에 findViewById를 호출해야 한다
        btnMusic = findViewById(R.id.btnMusic);
        btnTimer = findViewById(R.id.btnTimer);
        btnHistory = findViewById(R.id.btnHistory);

        btnMusic.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MusicActivity.class)));

        btnTimer.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SleepTimerActivity.class)));

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SleepHistoryActivity.class)));
    }
}
