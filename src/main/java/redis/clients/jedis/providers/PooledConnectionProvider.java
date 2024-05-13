package redis.clients.jedis.providers;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionFactory;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.util.Pool;

/**
* This class provides pooled connections.
* It implements the ConnectionProvider interface.
*/
public class PooledConnectionProvider implements ConnectionProvider {

  private final Pool<Connection> pool;
  private Object connectionMapKey = "";

  /**
  * This constructor creates a pooled connection provider with a host and port.
  * @param hostAndPort The host and port to use.
  */
  public PooledConnectionProvider(HostAndPort hostAndPort) {
    this(new ConnectionFactory(hostAndPort));
    this.connectionMapKey = hostAndPort;
  }

  /**
  * This constructor creates a pooled connection provider with a host, port and client configuration.
  * @param hostAndPort The host and port to use.
  * @param clientConfig The client configuration to use.
  */
  public PooledConnectionProvider(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
    this(new ConnectionPool(hostAndPort, clientConfig));
    this.connectionMapKey = hostAndPort;
  }

  public PooledConnectionProvider(HostAndPort hostAndPort, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionFactory(hostAndPort, clientConfig), poolConfig);
    this.connectionMapKey = hostAndPort;
  }

  /**
  * This constructor creates a pooled connection provider with a connection factory.
  * @param factory The connection factory to use.
  */
  public PooledConnectionProvider(PooledObjectFactory<Connection> factory) {
    this(new ConnectionPool(factory));
    this.connectionMapKey = factory;
  }

  public PooledConnectionProvider(PooledObjectFactory<Connection> factory,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(new ConnectionPool(factory, poolConfig));
    this.connectionMapKey = factory;
  }

  /**
  * This private constructor creates a pooled connection provider with a connection pool.
  * @param pool The connection pool to use.
  */
  private PooledConnectionProvider(Pool<Connection> pool) {
    this.pool = pool;
  }

  @Override
  /**
  * This method closes the connection pool.
  */
  public void close() {
    pool.close();
  }

  /**
  * This method returns the connection pool.
  * @return The connection pool.
  */
  public final Pool<Connection> getPool() {
    return pool;
  }

  @Override
  /**
  * This method gets a connection from the pool.
  * @return A connection from the pool.
  */
  public Connection getConnection() {
    return pool.getResource();
  }

  @Override
  /**
  * This method gets a connection from the pool with specific command arguments.
  * @param args The command arguments.
  * @return A connection from the pool.
  */
  public Connection getConnection(CommandArguments args) {
    return pool.getResource();
  }

  @Override
  /**
  * This method returns a map of connections.
  * @return A map of connections.
  */
  public Map<?, Pool<Connection>> getConnectionMap() {
    return Collections.singletonMap(connectionMapKey, pool);
  }
}
