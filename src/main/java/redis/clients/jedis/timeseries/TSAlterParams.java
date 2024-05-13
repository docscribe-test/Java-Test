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

  /**
   * Default constructor for TSAlterParams.
   */
  public TSAlterParams() {
  }

  /**
   * Static factory method to create a new instance of TSAlterParams.
   * @return a new instance of TSAlterParams.
   */
  public static TSAlterParams alterParams() {
    return new TSAlterParams();
  }

  /**
   * Set the retention period for TSAlterParams.
   * @param retentionPeriod the retention period to set.
   * @return the updated TSAlterParams instance.
   */
  public TSAlterParams retention(long retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
    return this;
  }

  /**
   * Set the chunk size for TSAlterParams.
   * @param chunkSize the chunk size to set.
   * @return the updated TSAlterParams instance.
   */
  public TSAlterParams chunkSize(long chunkSize) {
    this.chunkSize = chunkSize;
    return this;
  }

  /**
   * Add a label with the specified value to TSAlterParams.
   * @param label the label to add.
   * @param value the value of the label.
   * @return the updated TSAlterParams instance.
   */
  public TSAlterParams label(String label, String value) {
    if (this.labels == null) {
      this.labels = new LinkedHashMap<>();
    }
    this.labels.put(label, value);
    return this;
  }

  /**
   * Reset the labels in TSAlterParams to an empty map.
   * @return the updated TSAlterParams instance.
   */
  public TSAlterParams labelsReset() {
    return this.labels(Collections.emptyMap());
  }

  /**
   * Set the labels for TSAlterParams.
   * @param labels the labels to set.
   * @return the updated TSAlterParams instance.
   */
  public TSAlterParams labels(Map<String, String> labels) {
    this.labels = labels;
    return this;
  }

  @Override
  /**
   * Adds TS.ALTER command arguments based on the set parameters.
   * @param args the CommandArguments to add the parameters to.
   */
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
