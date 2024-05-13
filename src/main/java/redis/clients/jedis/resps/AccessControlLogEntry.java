package redis.clients.jedis.resps;

import java.io.Serializable;
import java.util.Map;

/**
 * This class holds information about an Access Control Log entry (returned by ACL LOG command) They
 * can be accessed via getters. For future purpose there is also {@link #getlogEntry} method that
 * returns a generic {@code Map} - in case where more info is returned from a server
 */
// TODO: remove
public class AccessControlLogEntry implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String COUNT = "count";
  public static final String REASON = "reason";
  public static final String CONTEXT = "context";
  public static final String OBJECT = "object";
  public static final String USERNAME = "username";
  public static final String AGE_SECONDS = "age-seconds";
  public static final String CLIENT_INFO = "client-info";
  // Redis 7.2
  public static final String ENTRY_ID = "entry-id";
  public static final String TIMESTAMP_CREATED = "timestamp-created";
  public static final String TIMESTAMP_LAST_UPDATED = "timestamp-last-updated";

  private final long count;
  private final String reason;
  private final String context;
  private final String object;
  private final String username;
  private final Double ageSeconds;
  private final Map<String, String> clientInfo;
  private final Map<String, Object> logEntry;
  private final long entryId;
  private final long timestampCreated;
  private final long timestampLastUpdated;

  /**
   * Constructs a new AccessControlLogEntry object with the given map.
   * @param map The map containing key-value pairs for initialization.
  */
  public AccessControlLogEntry(Map<String, Object> map) {
    count = (long) map.get(COUNT);
    reason = (String) map.get(REASON);
    context = (String) map.get(CONTEXT);
    object = (String) map.get(OBJECT);
    username = (String) map.get(USERNAME);
    ageSeconds = (Double) map.get(AGE_SECONDS);
    clientInfo = getMapFromRawClientInfo((String) map.get(CLIENT_INFO));
    logEntry = map;
    entryId = (long) map.get(ENTRY_ID);
    timestampCreated = (long) map.get(TIMESTAMP_CREATED);
    timestampLastUpdated = (long) map.get(TIMESTAMP_LAST_UPDATED);
  }

  public long getCount() {
    return count;
  }

  public String getReason() {
    return reason;
  }

  public String getContext() {
    return context;
  }

  public String getObject() {
    return object;
  }

  public String getUsername() {
    return username;
  }

  public Double getAgeSeconds() {
    return ageSeconds;
  }

  public Map<String, String> getClientInfo() {
    return clientInfo;
  }

  /**
   * @return Generic map containing all key-value pairs returned by the server
   */
  public Map<String, Object> getlogEntry() {
    return logEntry;
  }

  public long getEntryId() {
    return entryId;
  }

  public long getTimestampCreated() {
    return timestampCreated;
  }

  public long getTimestampLastUpdated() {
    return timestampLastUpdated;
  }

  @Override
  public String toString() {
    return "AccessControlLogEntry{" + "count=" + count + ", reason='" + reason + '\''
        + ", context='" + context + '\'' + ", object='" + object + '\'' + ", username='" + username
        + '\'' + ", ageSeconds='" + ageSeconds + '\'' + ", clientInfo=" + clientInfo
        + ", entryId=" + entryId + ", timestampCreated=" + timestampCreated
        + ", timestampLastUpdated=" + timestampLastUpdated + '}';
  }
}
