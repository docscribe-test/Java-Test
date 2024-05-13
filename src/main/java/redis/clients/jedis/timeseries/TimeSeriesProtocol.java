package redis.clients.jedis.timeseries;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class TimeSeriesProtocol {

  public static final byte[] PLUS = SafeEncoder.encode("+");
  public static final byte[] MINUS = SafeEncoder.encode("-");

  /**
  * This enum represents commands for Time Series operations.
  */
  public enum TimeSeriesCommand implements ProtocolCommand {

    CREATE("TS.CREATE"),
    RANGE("TS.RANGE"),
    REVRANGE("TS.REVRANGE"),
    MRANGE("TS.MRANGE"),
    MREVRANGE("TS.MREVRANGE"),
    CREATERULE("TS.CREATERULE"),
    DELETERULE("TS.DELETERULE"),
    ADD("TS.ADD"),
    MADD("TS.MADD"),
    DEL("TS.DEL"),
    INCRBY("TS.INCRBY"),
    DECRBY("TS.DECRBY"),
    INFO("TS.INFO"),
    GET("TS.GET"),
    MGET("TS.MGET"),
    ALTER("TS.ALTER"),
    QUERYINDEX("TS.QUERYINDEX");

    /**
    * The raw byte array representation of the command or keyword.
    */
    private final byte[] raw;

    /**
    * Constructor for TimeSeriesCommand enum.
    * @param alt The alternative representation of the command.
    */
    private TimeSeriesCommand(String alt) {
      raw = SafeEncoder.encode(alt);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  /**
  * This enum represents keywords for Time Series operations.
  */
  public enum TimeSeriesKeyword implements Rawable {

    RESET,
    FILTER,
    AGGREGATION,
    LABELS,
    RETENTION,
    TIMESTAMP,
    WITHLABELS,
    SELECTED_LABELS,
    COUNT,
    ENCODING,
    COMPRESSED,
    UNCOMPRESSED,
    CHUNK_SIZE,
    DUPLICATE_POLICY,
    ON_DUPLICATE,
    ALIGN,
    FILTER_BY_TS,
    FILTER_BY_VALUE,
    GROUPBY,
    REDUCE,
    DEBUG,
    LATEST,
    EMPTY,
    BUCKETTIMESTAMP;

    private final byte[] raw;

    /**
    * Constructor for TimeSeriesKeyword enum.
    */
    private TimeSeriesKeyword() {
      raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }

    /**
    * Get the first TimeSeriesKeyword.
    * @return The first TimeSeriesKeyword.
    */
    public static TimeSeriesKeyword getFirst() {
      return values()[0];
    }

    /**
    * Get the last TimeSeriesKeyword.
    * @return The last TimeSeriesKeyword.
    */
    public static TimeSeriesKeyword getLast() {
      return values()[values().length - 1];
    }
  }
}
