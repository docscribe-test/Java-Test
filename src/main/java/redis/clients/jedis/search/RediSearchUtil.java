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


  /**
   * Converts a Map containing keys of type String and values of type Object to a Map with keys and values of type String.
   * If stringEscape is false, the values are directly converted to String. If stringEscape is true, special characters are escaped in the values.
   * @param input The input Map to be converted.
   * @param stringEscape Flag indicating whether to escape special characters in the values.
   * @return A Map with keys and values of type String.
   */
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
   * Converts an array of float values to a byte array.
   * The float values are converted to bytes with little-endian order.
   * @param input The input array of float values.
   * @return A byte array representing the input float values.
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
   * Special characters include: , . < > { } [ ] " ' : ; ! @ # $ % ^ & * ( ) - + = ~ |
   * @param text The input text to escape special characters.
   * @return The text with escaped special characters.
   */
  public static String escape(String text) {
    return escape(text, false);
  }

  /**
   * Escapes special characters in the input query text.
   * Special characters include: , . < > { } [ ] " ' : ; ! @ # $ % ^ & * ( ) - + = ~ |
   * Spaces are also escaped in query text.
   * @param query The input query text to escape special characters.
   * @return The query text with escaped special characters.
   */
  public static String escapeQuery(String query) {
    return escape(query, true);
  }

  /**
   * Escapes special characters in the input text.
   * Special characters include: , . < > { } [ ] " ' : ; ! @ # $ % ^ & * ( ) - + = ~ |
   * If querying is true, spaces are also escaped in the text.
   * @param text The input text to escape special characters.
   * @param querying Flag indicating whether to escape spaces in addition to special characters.
   * @return The text with escaped special characters.
   */
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
   * Private constructor to prevent instantiation of the RediSearchUtil class.
   * Throws an InstantiationError with a message indicating not to instantiate this class.
   */
  private RediSearchUtil() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
