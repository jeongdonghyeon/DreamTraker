package kr.co.example.dreamtraker;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SleepHistoryActivity extends AppCompatActivity {

    private ListView listViewHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_history);

        listViewHistory = findViewById(R.id.listViewHistory);
        Button btnBack = findViewById(R.id.btnBack); // 🔹 XML에 있는 뒤로가기 버튼

        // 뒤로가기 버튼 클릭 시 현재 액티비티 종료
        btnBack.setOnClickListener(v -> finish());

        ArrayList<String> items = new ArrayList<>();
        for (SleepRecord record : SleepTimerActivity.sleepRecords) {
            items.add(record.getDate() + " - " + record.getDuration() +
                    "\n" + record.getStartTime() + " ~ " + record.getEndTime());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.white_text,
                items
        );
        listViewHistory.setAdapter(adapter);
    }
}
