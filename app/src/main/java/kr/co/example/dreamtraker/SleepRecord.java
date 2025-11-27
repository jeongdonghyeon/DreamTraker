package kr.co.example.dreamtraker;

public class SleepRecord {
    private String date;
    private String startTime;
    private String endTime;
    private String duration;
    private String sleepQuality;
    private long durationMillis;

    public SleepRecord(String date, String startTime, String endTime, String duration, String sleepQuality, long durationMillis) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.sleepQuality = sleepQuality;
        this.durationMillis = durationMillis;
    }

    // --- Getter 메서드 (데이터 읽기) ---

    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getDuration() { return duration; }
    public String getSleepQuality() { return sleepQuality; }
    public long getDurationMillis() { return durationMillis; }

    // 🌟 🌟 🌟 추가된 메서드 🌟 🌟 🌟
    // 밀리초를 시, 분, 초로 변환하여 반환
    public long getHours() {
        return (durationMillis / (1000 * 60 * 60));
    }
    public long getMinutes() {
        // 총 밀리초에서 시간을 뺀 나머지를 분으로 변환
        return ((durationMillis / (1000 * 60)) % 60);
    }
    public long getSeconds() {
        // 총 밀리초에서 시와 분을 뺀 나머지를 초로 변환
        return ((durationMillis / 1000) % 60);
    }

    // --- Setter 메서드 (데이터 수정) ---

    public void setDate(String date) { this.date = date; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setSleepQuality(String sleepQuality) { this.sleepQuality = sleepQuality; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }

    @Override
    public String toString() {
        return "SleepRecord{" +
                "date='" + date + '\'' +
                ", duration='" + duration + '\'' +
                ", quality='" + sleepQuality + '\'' +
                ", millis=" + durationMillis +
                '}';
    }
}