package redis.clients.jedis.timeseries;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.CHUNK_SIZE;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.DUPLICATE_POLICY;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.LABELS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.RETENTION;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

/**
 * Represents optional arguments of TS.ALTER command.
 */
public class TSAlterParams implements IParams {

  private Long retentionPeriod;
  private Long chunkSize;
  private DuplicatePolicy duplicatePolicy;
  private Map<String, String> labels;

  public TSAlterParams() {
  }

  public static TSAlterParams alterParams() {
    return new TSAlterParams();
  }

  public TSAlterParams retention(long retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
    return this;
  }

  public TSAlterParams chunkSize(long chunkSize) {
    this.chunkSize = chunkSize;
    return this;
  }

  public TSAlterParams label(String label, String value) {
    if (this.labels == null) {
      this.labels = new LinkedHashMap<>();
    }
    this.labels.put(label, value);
    return this;
  }

  public TSAlterParams labelsReset() {
    return this.labels(Collections.emptyMap());
  }

  public TSAlterParams labels(Map<String, String> labels) {
    this.labels = labels;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (retentionPeriod != null) {
      args.add(RETENTION).add(toByteArray(retentionPeriod));
    }

    if (chunkSize != null) {
      args.add(CHUNK_SIZE).add(toByteArray(chunkSize));
    }

    if (duplicatePolicy != null) {
      args.add(DUPLICATE_POLICY).add(duplicatePolicy);
    }

    if (labels != null) {
      args.add(LABELS);
      labels.entrySet().forEach((entry) -> args.add(entry.getKey()).add(entry.getValue()));
    }
  }
}
