package kr.co.example.dreamtraker;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MusicActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Button btnNature, btnSea, btnStop, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        // ✅ 먼저 findViewById로 버튼들을 연결해야 함
        btnNature = findViewById(R.id.btnNature);
        btnSea = findViewById(R.id.btnSea);
        btnStop = findViewById(R.id.btnStop);
        btnBack = findViewById(R.id.btnBack);

        // ✅ 그 다음에 리스너 설정
        btnBack.setOnClickListener(v -> finish());
        btnNature.setOnClickListener(v -> playSound(R.raw.nature));
        btnSea.setOnClickListener(v -> playSound(R.raw.sea_waves));
        btnStop.setOnClickListener(v -> stopSound());
    }

    private void playSound(int soundResId) {
        stopSound();
        mediaPlayer = MediaPlayer.create(this, soundResId);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void stopSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSound();
    }
}
