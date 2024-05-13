package redis.clients.jedis.search;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.util.SafeEncoder;

public class RediSearchUtil {


  public static Map<String, String> toStringMap(Map<String, Object> input) {
    return toStringMap(input, false);
  }


  public static Map<String, String> toStringMap(Map<String, Object> input, boolean stringEscape) {
    Map<String, String> output = new HashMap<>(input.size());
    for (Map.Entry<String, Object> entry : input.entrySet()) {
      String key = entry.getKey();
      Object obj = entry.getValue();
      if (key == null || obj == null) {
        throw new NullPointerException("A null argument cannot be sent to Redis.");
      }
      String str;
      if (obj instanceof byte[]) {
        str = SafeEncoder.encode((byte[]) obj);
      } else if (obj instanceof redis.clients.jedis.GeoCoordinate) {
        redis.clients.jedis.GeoCoordinate geo = (redis.clients.jedis.GeoCoordinate) obj;
        str = geo.getLongitude() + "," + geo.getLatitude();
      } else if (obj instanceof String) {
        str = stringEscape ? escape((String) obj) : (String) obj;
      } else {
        str = String.valueOf(obj);
      }
      output.put(key, str);
    }
    return output;
  }

  /**
  * Converts an array of floats to a byte array.
  * @param input The array of floats to convert.
  * @return The resulting byte array.
  */
  public static byte[] toByteArray(float[] input) {
    byte[] bytes = new byte[Float.BYTES * input.length];
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(input);
    return bytes;
  }

  @Deprecated
  public static byte[] ToByteArray(float[] input) {
    return toByteArray(input);
  }

  private static final Set<Character> ESCAPE_CHARS = new HashSet<>(Arrays.asList(//
      ',', '.', '<', '>', '{', '}', '[', //
      ']', '"', '\'', ':', ';', '!', '@', //
      '#', '$', '%', '^', '&', '*', '(', //
      ')', '-', '+', '=', '~', '|' //
  ));

  /**
  * Escapes special characters in the input text.
  * @param text The text to escape.
  * @return The escaped text.
  */
  public static String escape(String text) {
    return escape(text, false);
  }

  /**
  * Escapes the input query text.
  * @param query The query text to escape.
  * @return The escaped query text.
  */
  public static String escapeQuery(String query) {
    return escape(query, true);
  }

  public static String escape(String text, boolean querying) {
    char[] chars = text.toCharArray();

    StringBuilder sb = new StringBuilder();
    for (char ch : chars) {
      if (ESCAPE_CHARS.contains(ch)
          || (querying && ch == ' ')) {
        sb.append("\\");
      }
      sb.append(ch);
    }
    return sb.toString();
  }

  public static String unescape(String text) {
    return text.replace("\\", "");
  }

  /**
  * Private constructor to prevent instantiation of this utility class.
  * @throws InstantiationError Always thrown to indicate that instantiation is not allowed.
  */
  private RediSearchUtil() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
