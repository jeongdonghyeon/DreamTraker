package kr.co.example.dreamtraker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SleepRecordAdapter extends RecyclerView.Adapter<SleepRecordAdapter.RecordViewHolder> {

    private final List<SleepRecord> sleepRecords;

    public SleepRecordAdapter(List<SleepRecord> sleepRecords) {
        this.sleepRecords = sleepRecords;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // item_sleep_record.xml 레이아웃을 인플레이트합니다.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sleep_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        SleepRecord record = sleepRecords.get(position);

        // 🌟 데이터 바인딩 (item_sleep_record.xml의 TextView에 데이터 연결)

        // 수면 시간 표시: "수면 시간: 7시간 30분"
        holder.tvDuration.setText(String.format("수면 시간: %s", record.getDuration()));

        // 날짜 및 시간 범위 표시: "2023년 10월 26일 (23:30 - 07:00)"
        String dateTimeText = String.format("%s (%s - %s)",
                record.getDate(), record.getStartTime(), record.getEndTime());
        holder.tvDateTime.setText(dateTimeText);

        // 수면 질에 따른 아이콘/색상 변경 로직 (선택 사항)
        // 현재는 모든 아이콘이 @drawable/sleep으로 고정되어 있습니다.
        // 필요하다면, record.getSleepQuality() 값에 따라 holder.ivQualityIcon.setImageResource(...)를 사용해 아이콘을 변경할 수 있습니다.
    }

    @Override
    public int getItemCount() {
        return sleepRecords.size();
    }

    // 뷰 홀더 (item_sleep_record.xml의 뷰들을 참조)
    public static class RecordViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivQualityIcon;
        final TextView tvDuration;
        final TextView tvDateTime;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            ivQualityIcon = itemView.findViewById(R.id.ivQualityIcon);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
        }
    }
}