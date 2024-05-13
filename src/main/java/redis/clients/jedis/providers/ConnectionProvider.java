package redis.clients.jedis.providers;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

import redis.clients.jedis.CommandArguments;

/**
* ConnectionProvider is an interface that provides methods to interact with database connections.
* This interface extends AutoCloseable, which provides automatic resource management for objects that are closed when they are no longer needed.
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
  * This method provides a default implementation for retrieving a map representation of the database connection.
  * The map contains a single entry where the key is the connection's string representation and the value is the connection object.
  * @return a map containing a single entry with the connection string as the key and the connection object as the value.
  */
  default Map<?, ?> getConnectionMap() {
    final Connection c = getConnection();
    return Collections.singletonMap(c.toString(), c);
  }
}
