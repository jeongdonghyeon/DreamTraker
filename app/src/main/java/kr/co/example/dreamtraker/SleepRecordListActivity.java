package kr.co.example.dreamtraker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * '기록 보기' 버튼을 통해 진입하는 수면 기록 상세 화면.
 * 내부 어댑터를 사용하며, 삭제 기능을 포함합니다.
 */
public class SleepRecordListActivity extends AppCompatActivity {

    // 🌟 UI 요소
    private ImageButton btnClose;
    private TextView tvDetailHours, tvDetailMinutes, tvDetailSeconds;

    // 🌟 추가된 편집/삭제 UI
    private TextView btnToggleEdit;
    private Button btnDeleteSelected;

    // 🌟 RecyclerView 요소
    private RecyclerView rvSleepRecords;
    private SleepRecordAdapter adapter;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_history);

        // 1. UI 요소 연결
        btnClose = findViewById(R.id.btnClose);
        tvDetailHours = findViewById(R.id.tvDetailHours);
        tvDetailMinutes = findViewById(R.id.tvDetailMinutes);
        tvDetailSeconds = findViewById(R.id.tvDetailSeconds);

        // 🌟 버튼 연결
        btnToggleEdit = findViewById(R.id.btnToggleEdit);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);

        // 2. 닫기 버튼 설정
        btnClose.setOnClickListener(v -> finish());

        // 3. 기록 상세 시간 표시
        loadAndDisplayRecordDetail();

        // 4. 기록 목록 RecyclerView 설정
        rvSleepRecords = findViewById(R.id.rvSleepRecords);
        setupRecyclerView();

        // 🌟 5. [편집] 버튼 기능
        btnToggleEdit.setOnClickListener(v -> {
            isEditMode = !isEditMode;
            if (isEditMode) {
                btnToggleEdit.setText("취소");
                btnDeleteSelected.setVisibility(View.VISIBLE);
            } else {
                btnToggleEdit.setText("편집");
                btnDeleteSelected.setVisibility(View.GONE);
            }
            if (adapter != null) {
                adapter.setSelectionMode(isEditMode);
            }
        });

        // 🌟 6. [삭제] 버튼 기능
        btnDeleteSelected.setOnClickListener(v -> deleteSelectedItems());
    }

    // ------------------- 기능 로직 -------------------

    private void loadAndDisplayRecordDetail() {
        if (MainActivity.sleepRecords.isEmpty()) {
            tvDetailHours.setText("00");
            tvDetailMinutes.setText("00");
            tvDetailSeconds.setText("00");
            return;
        }
        SleepRecord latestRecord = MainActivity.sleepRecords.get(MainActivity.sleepRecords.size() - 1);
        tvDetailHours.setText(String.format(Locale.getDefault(), "%02d", latestRecord.getHours()));
        tvDetailMinutes.setText(String.format(Locale.getDefault(), "%02d", latestRecord.getMinutes()));
        tvDetailSeconds.setText(String.format(Locale.getDefault(), "%02d", latestRecord.getSeconds()));
    }

    private void setupRecyclerView() {
        // 원본 데이터를 복사해서 역순으로 보여줌
        List<SleepRecord> records = new ArrayList<>(MainActivity.sleepRecords);
        Collections.reverse(records);

        // 내부 어댑터 생성
        adapter = new SleepRecordAdapter(records);
        rvSleepRecords.setLayoutManager(new LinearLayoutManager(this));
        rvSleepRecords.setAdapter(adapter);
    }

    // 🌟 삭제 기능 구현
    private void deleteSelectedItems() {
        if (adapter == null) return;

        Set<Integer> selectedPositions = adapter.getSelectedPositions();
        if (selectedPositions.isEmpty()) {
            Toast.makeText(this, "삭제할 항목을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 현재 어댑터 리스트(역순)를 기준으로 삭제 대상 찾기
        List<SleepRecord> currentAdapterList = adapter.getSleepRecords();
        List<SleepRecord> toRemove = new ArrayList<>();

        for (int pos : selectedPositions) {
            if (pos < currentAdapterList.size()) {
                toRemove.add(currentAdapterList.get(pos));
            }
        }

        // 1. 원본 데이터(MainActivity)에서 제거
        MainActivity.sleepRecords.removeAll(toRemove);

        // 2. 영구 저장 (Gson)
        SharedPreferences sharedPreferences = getSharedPreferences("DreamTrackerData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(MainActivity.sleepRecords);
        editor.putString("sleep_history_list", json);
        editor.apply();

        // 3. 화면 갱신
        List<SleepRecord> newList = new ArrayList<>(MainActivity.sleepRecords);
        Collections.reverse(newList);
        adapter.updateRecords(newList); // 어댑터 데이터 교체

        // 4. 편집 모드 종료
        isEditMode = false;
        btnToggleEdit.setText("편집");
        btnDeleteSelected.setVisibility(View.GONE);
        adapter.setSelectionMode(false);

        Toast.makeText(this, toRemove.size() + "개의 기록 삭제됨", Toast.LENGTH_SHORT).show();

        // 다 지워졌으면 상단 시간 초기화
        if (MainActivity.sleepRecords.isEmpty()) {
            tvDetailHours.setText("00");
            tvDetailMinutes.setText("00");
            tvDetailSeconds.setText("00");
        }
    }

    // ------------------- 내부 어댑터 클래스 (수정됨) -------------------

    public static class SleepRecordAdapter extends RecyclerView.Adapter<SleepRecordAdapter.RecordViewHolder> {

        private List<SleepRecord> sleepRecords;

        // 🌟 체크박스 기능을 위한 변수들
        private boolean isSelectionMode = false;
        private final Set<Integer> selectedPositions = new HashSet<>();

        public SleepRecordAdapter(List<SleepRecord> sleepRecords) {
            this.sleepRecords = sleepRecords;
        }

        // 🌟 데이터 갱신 메서드
        public void updateRecords(List<SleepRecord> newRecords) {
            this.sleepRecords = newRecords;
            notifyDataSetChanged();
        }

        public List<SleepRecord> getSleepRecords() {
            return sleepRecords;
        }

        // 🌟 편집 모드 설정
        public void setSelectionMode(boolean enabled) {
            this.isSelectionMode = enabled;
            this.selectedPositions.clear();
            notifyDataSetChanged();
        }

        public Set<Integer> getSelectedPositions() {
            return selectedPositions;
        }

        @NonNull
        @Override
        public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sleep_record, parent, false);
            return new RecordViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
            SleepRecord record = sleepRecords.get(position);

            // 데이터 바인딩
            holder.tvDuration.setText(String.format("수면 시간: %s", record.getDuration()));
            String dateTimeText = String.format("%s (%s - %s)",
                    record.getDate(), record.getStartTime(), record.getEndTime());
            holder.tvDateTime.setText(dateTimeText);

            // 🌟 체크박스 로직
            if (isSelectionMode) {
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setOnCheckedChangeListener(null); // 리스너 충돌 방지
                holder.checkBox.setChecked(selectedPositions.contains(position));

                holder.checkBox.setOnClickListener(v -> {
                    if (holder.checkBox.isChecked()) selectedPositions.add(holder.getAdapterPosition());
                    else selectedPositions.remove(holder.getAdapterPosition());
                });

                // 아이템 클릭 시 체크박스 토글
                holder.itemView.setOnClickListener(v -> holder.checkBox.performClick());
            } else {
                holder.checkBox.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return sleepRecords.size();
        }

        public static class RecordViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivQualityIcon;
            final TextView tvDuration;
            final TextView tvDateTime;
            final CheckBox checkBox; // 🌟 체크박스 추가

            public RecordViewHolder(@NonNull View itemView) {
                super(itemView);
                ivQualityIcon = itemView.findViewById(R.id.ivQualityIcon);
                tvDuration = itemView.findViewById(R.id.tvDuration);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                checkBox = itemView.findViewById(R.id.checkBoxDelete); // 🌟 ID 연결
            }
        }
    }
}