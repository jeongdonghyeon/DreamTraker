package kr.co.example.dreamtraker;

public class SleepRecord {
    private String date;
    private String startTime;
    private String endTime;
    private String duration;

    public SleepRecord(String date, String startTime, String endTime, String duration) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
    }

    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getDuration() { return duration; }
}
