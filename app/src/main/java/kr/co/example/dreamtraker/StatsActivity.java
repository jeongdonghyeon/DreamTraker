package kr.co.example.dreamtraker;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class StatsActivity extends AppCompatActivity {

    private BarChart barChart;
    private TextView tvAverageTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ 방금 바꾸신 xml 파일명으로 연결
        setContentView(R.layout.sleep_chart);

        // 뷰 연결 (xml에 있는 ID와 같아야 함)
        barChart = findViewById(R.id.sleepChart);
        tvAverageTime = findViewById(R.id.tvAverageTime);

        // 차트 그리기 실행
        setupChart();

        // 평균 시간 텍스트 업데이트 (예시)
        tvAverageTime.setText("7시간 15분");
    }

    private void setupChart() {
        // 1. 샘플 데이터 생성
        // 나중에는 여기에 DB 데이터를 반복문으로 넣으면 됩니다.
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, 7.5f)); // 월
        entries.add(new BarEntry(1f, 6.0f)); // 화
        entries.add(new BarEntry(2f, 8.2f)); // 수
        entries.add(new BarEntry(3f, 5.5f)); // 목
        entries.add(new BarEntry(4f, 9.0f)); // 금
        entries.add(new BarEntry(5f, 7.2f)); // 토
        entries.add(new BarEntry(6f, 8.5f)); // 일

        // 2. 데이터셋 설정
        BarDataSet dataSet = new BarDataSet(entries, "수면 시간");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(14f);

        // 3. 차트 설정
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        // 4. 디자인 다듬기
        barChart.getDescription().setEnabled(false); // 설명 끄기
        barChart.setFitBars(true); // 막대 정렬
        barChart.animateY(1000); // 애니메이션

        // X축을 아래로 내리기
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);

        // 오른쪽 축 숨기기 (깔끔하게)
        barChart.getAxisRight().setEnabled(false);

        // 갱신
        barChart.invalidate();
    }
}