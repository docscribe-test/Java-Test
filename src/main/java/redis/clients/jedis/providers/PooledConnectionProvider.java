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
* This is a PooledConnectionProvider class that implements ConnectionProvider interface.
* It provides pooled connections to Redis.
*/
public class PooledConnectionProvider implements ConnectionProvider {

  private final Pool<Connection> pool;
  private Object connectionMapKey = "";

  /**
  * Constructor for PooledConnectionProvider.
  * @param hostAndPort The host and port to connect.
  */
  public PooledConnectionProvider(HostAndPort hostAndPort) {
    this(new ConnectionFactory(hostAndPort));
    this.connectionMapKey = hostAndPort;
  }

  /**
  * Constructor for PooledConnectionProvider.
  * @param hostAndPort The host and port to connect.
  * @param clientConfig The client configuration for Jedis.
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
  * Constructor for PooledConnectionProvider.
  * @param factory The factory to create Connection objects.
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
  * Private constructor for PooledConnectionProvider.
  * @param pool The pool of Connection objects.
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
  * @return Pool of Connection objects.
  */
  public final Pool<Connection> getPool() {
    return pool;
  }

  @Override
  /**
  * This method retrieves a connection from the pool.
  * @return Connection from the pool.
  */
  public Connection getConnection() {
    return pool.getResource();
  }

  @Override
  /**
  * This method retrieves a connection from the pool.
  * @param args The command arguments.
  * @return Connection from the pool.
  */
  public Connection getConnection(CommandArguments args) {
    return pool.getResource();
  }

  @Override
  /**
  * This method returns a Map of connections.
  * @return Map containing connection key and its associated connection pool.
  */
  public Map<?, Pool<Connection>> getConnectionMap() {
    return Collections.singletonMap(connectionMapKey, pool);
  }
}
