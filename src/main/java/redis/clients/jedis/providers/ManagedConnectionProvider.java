package redis.clients.jedis.providers;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;

/**
* This is a ManagedConnectionProvider class that implements the ConnectionProvider interface.
* It is responsible for managing connections.
*/
public class ManagedConnectionProvider implements ConnectionProvider {

  private Connection connection;

  /**
  * This method is used to set the connection.
  * @param connection The connection to be set.
  */
  public final void setConnection(Connection connection) {
    this.connection = connection;
  }

  @Override
  /**
  * This method is used to close the connection.
  */
  public void close() {
  }

  @Override
  /**
  * This method is used to get the connection.
  * @return The current connection.
  */
  public final Connection getConnection() {
    return connection;
  }

  @Override
  /**
  * This method is used to get the connection with specific arguments.
  * @param args The command arguments for the connection.
  * @return The current connection.
  */
  public final Connection getConnection(CommandArguments args) {
    return connection;
  }
}
