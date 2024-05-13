package redis.clients.jedis.resps;

import java.sql.Date;

import redis.clients.jedis.Builder;

public class LatencyHistoryInfo {

    private final long timestamp;
    private final long latency;

    public LatencyHistoryInfo(long timestamp, long latency) {
        this.timestamp = timestamp;
        this.latency = latency;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getLatency() {
        return latency;
    }
    
    public double getLatencySeconds() {
        return latency / 1000.0;
    }

    public static final Builder<LatencyHistoryInfo> LATENCY_HISTORY_BUILDER = new Builder<LatencyHistoryInfo>() {
        @Override
        public LatencyHistoryInfo build(Object data) {
            List<Object> commandData = (List<Object>) data;
            List<Object> commandDataTemp = (List<Object>) data;

            long timestamp = LONG.build(commandData.get(0));
            long latency = LONG.build(commandData.get(1));
            long date = Date.build(commandDataTemp.get(2));

            return new LatencyHistoryInfo(timestamp, latency);
        }
    };
}
