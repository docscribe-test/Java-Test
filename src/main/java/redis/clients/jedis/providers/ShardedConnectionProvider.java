package redis.clients.jedis.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.ShardedCommandArguments;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Hashing;

/**
* This is a deprecated class that provides sharded connections.
*/
@Deprecated
public class ShardedConnectionProvider implements ConnectionProvider {

  private final TreeMap<Long, HostAndPort> nodes = new TreeMap<>();
  private final Map<String, ConnectionPool> resources = new HashMap<>();
  private final JedisClientConfig clientConfig;
  private final GenericObjectPoolConfig<Connection> poolConfig;
  private final Hashing algo;

  /**
  * Constructor for creating a ShardedConnectionProvider with a list of shards.
  * @param shards List of shards.
  */
  public ShardedConnectionProvider(List<HostAndPort> shards) {
    this(shards, DefaultJedisClientConfig.builder().build());
  }

  /**
  * Constructor for creating a ShardedConnectionProvider with a list of shards and a client configuration.
  * @param shards List of shards.
  * @param clientConfig Configuration for the Jedis client.
  */
  public ShardedConnectionProvider(List<HostAndPort> shards, JedisClientConfig clientConfig) {
    this(shards, clientConfig, new GenericObjectPoolConfig<Connection>());
  }

  public ShardedConnectionProvider(List<HostAndPort> shards, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this(shards, clientConfig, poolConfig, Hashing.MURMUR_HASH);
  }

  public ShardedConnectionProvider(List<HostAndPort> shards, JedisClientConfig clientConfig,
      Hashing algo) {
    this(shards, clientConfig, null, algo);
  }

  public ShardedConnectionProvider(List<HostAndPort> shards, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig, Hashing algo) {
    this.clientConfig = clientConfig;
    this.poolConfig = poolConfig;
    this.algo = algo;
    initialize(shards);
  }

  /**
  * Initialize the ShardedConnectionProvider with a list of shards.
  * @param shards List of shards.
  */
  private void initialize(List<HostAndPort> shards) {
    for (int i = 0; i < shards.size(); i++) {
      HostAndPort shard = shards.get(i);
      for (int n = 0; n < 160; n++) {
        Long hash = this.algo.hash("SHARD-" + i + "-NODE-" + n);
        nodes.put(hash, shard);
        setupNodeIfNotExist(shard);
      }
    }
  }

  /**
  * Setup a node if it does not exist.
  * @param node Node to setup.
  * @return The existing or new connection pool.
  */
  /**
   * Setup a node if it does not exist.
   * @param node Node to setup.
   * @return The existing or new connection pool.
   */
  private ConnectionPool setupNodeIfNotExist(final HostAndPort node) {
    String nodeKey = node.toString();
    ConnectionPool existingPool = resources.get(nodeKey);
    if (existingPool != null) return existingPool;

    ConnectionPool nodePool = poolConfig == null ? new ConnectionPool(node, clientConfig)
        : new ConnectionPool(node, clientConfig, poolConfig);
    resources.put(nodeKey, nodePool);
    return nodePool;
  }

  /**
  * Get the hashing algorithm.
  * @return The hashing algorithm.
  */
  public Hashing getHashingAlgo() {
    return algo;
  }

  /**
  * Reset the ShardedConnectionProvider by clearing resources and nodes.
  */
  /**
   * Reset the ShardedConnectionProvider by clearing resources and nodes.
   */
  private void reset() {
    for (ConnectionPool pool : resources.values()) {
      try {
        if (pool != null) {
          pool.destroy();
        }
      } catch (RuntimeException e) {
        // pass
      }
    }
    resources.clear();
    nodes.clear();
  }

  @Override
  /**
  * Close the ShardedConnectionProvider by resetting it.
  */
  public void close() {
    reset();
  }

  /**
  * Get a node based on a hash.
  * @param hash Hash of the node.
  * @return The node corresponding to the hash.
  */
  public HostAndPort getNode(Long hash) {
    return hash != null ? getNodeFromHash(hash) : null;
  }

  /**
  * Get a connection to a node.
  * @param node Node to get connection to.
  * @return Connection to the node.
  */
  public Connection getConnection(HostAndPort node) {
    return node != null ? setupNodeIfNotExist(node).getResource() : getConnection();
  }

  @Override
  /**
  * Get a connection using command arguments.
  * @param args Command arguments.
  * @return Connection based on the command arguments.
  */
  public Connection getConnection(CommandArguments args) {
    final Long hash = ((ShardedCommandArguments) args).getKeyHash();
    return hash != null ? getConnection(getNodeFromHash(hash)) : getConnection();
  }

  /**
  * Get a shuffled list of node pools.
  * @return Shuffled list of node pools.
  */
  private List<ConnectionPool> getShuffledNodesPool() {
    List<ConnectionPool> pools = new ArrayList<>(resources.values());
    Collections.shuffle(pools);
    return pools;
  }

  @Override
  /**
  * Get a connection from the shuffled nodes pool.
  * @return Connection from the shuffled nodes pool.
  * @throws JedisException If no reachable shard is found.
  */
  public Connection getConnection() {
    List<ConnectionPool> pools = getShuffledNodesPool();

    JedisException suppressed = null;
    for (ConnectionPool pool : pools) {
      Connection jedis = null;
      try {
        jedis = pool.getResource();
        if (jedis == null) {
          continue;
        }

        jedis.ping();
        return jedis;

      } catch (JedisException ex) {
        if (suppressed == null) { // remembering first suppressed exception
          suppressed = ex;
        }
        if (jedis != null) {
          jedis.close();
        }
      }
    }

    JedisException noReachableNode = new JedisException("No reachable shard.");
    if (suppressed != null) {
      noReachableNode.addSuppressed(suppressed);
    }
    throw noReachableNode;
  }

  /**
  * Get a node from a hash.
  * @param hash Hash of the node.
  * @return Node corresponding to the hash.
  */
  /**
   * Get a node from a hash.
   * @param hash Hash of the node.
   * @return Node corresponding to the hash.
   */
  private HostAndPort getNodeFromHash(Long hash) {
    SortedMap<Long, HostAndPort> tail = nodes.tailMap(hash);
    if (tail.isEmpty()) {
      return nodes.get(nodes.firstKey());
    }
    return tail.get(tail.firstKey());
  }

  @Override
  /**
  * Get a map of connection pools.
  * @return Map of connection pools.
  */
  public Map<String, ConnectionPool> getConnectionMap() {
    return Collections.unmodifiableMap(resources);
  }
}
