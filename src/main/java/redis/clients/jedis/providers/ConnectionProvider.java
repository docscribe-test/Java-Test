package redis.clients.jedis.providers;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

import redis.clients.jedis.CommandArguments;

/**
* Interface for providing database connections.
*/
public interface ConnectionProvider extends AutoCloseable {

  /**
   * Retrieves a database connection.
   *
   * @return a Connection object representing a database connection.
   */
  Connection getConnection();

  /**
   * Retrieves a database connection with the specified command arguments.
   *
   * @param args the command arguments to be used when establishing the connection.
   * @return a Connection object representing a database connection.
   */
  Connection getConnection(CommandArguments args);

  /**
   * Returns a map containing a single entry with the connection string as the key and the connection object as the value.
   * This method is provided as a convenience for obtaining a map representation of the connection.
   *
   * @return a map containing a single entry with the connection string as the key and the connection object as the value.
   */
  /**
  * Retrieves a map representation of the current database connection.
  * @return a map with the connection string as the key and the connection object as the value.
  */
  default Map<?, ?> getConnectionMap() {
    final Connection c = getConnection();
    return Collections.singletonMap(c.toString(), c);
  }
}
