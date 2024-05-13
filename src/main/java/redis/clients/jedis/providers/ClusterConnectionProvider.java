package redis.clients.jedis.providers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.ClusterCommandArguments;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisException;

import static redis.clients.jedis.JedisCluster.INIT_NO_ERROR_PROPERTY;

/**
 * ClusterConnectionProvider class that implements ConnectionProvider interface.
 */
public class ClusterConnectionProvider implements ConnectionProvider {

  protected final JedisClusterInfoCache cache;

  /**
   * Constructor for ClusterConnectionProvider.
   * @param clusterNodes Set of cluster nodes.
   * @param clientConfig JedisClientConfig object.
   */
  public ClusterConnectionProvider(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig) {
    this.cache = new JedisClusterInfoCache(clientConfig, clusterNodes);
    initializeSlotsCache(clusterNodes, clientConfig);
  }

  public ClusterConnectionProvider(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig) {
    this.cache = new JedisClusterInfoCache(clientConfig, poolConfig, clusterNodes);
    initializeSlotsCache(clusterNodes, clientConfig);
  }

  public ClusterConnectionProvider(Set<HostAndPort> clusterNodes, JedisClientConfig clientConfig,
      GenericObjectPoolConfig<Connection> poolConfig, Duration topologyRefreshPeriod) {
    this.cache = new JedisClusterInfoCache(clientConfig, poolConfig, clusterNodes, topologyRefreshPeriod);
    initializeSlotsCache(clusterNodes, clientConfig);
  }

  /**
   * Initializes the cluster slots cache.
   * @param startNodes Set of starting nodes.
   * @param clientConfig JedisClientConfig object.
   */
  private void initializeSlotsCache(Set<HostAndPort> startNodes, JedisClientConfig clientConfig) {
    if (startNodes.isEmpty()) {
      throw new JedisClusterOperationException("No nodes to initialize cluster slots cache.");
    }

    ArrayList<HostAndPort> startNodeList = new ArrayList<>(startNodes);
    Collections.shuffle(startNodeList);

    JedisException firstException = null;
    for (HostAndPort hostAndPort : startNodeList) {
      try (Connection jedis = new Connection(hostAndPort, clientConfig)) {
        cache.discoverClusterNodesAndSlots(jedis);
        return;
      } catch (JedisException e) {
        if (firstException == null) {
          firstException = e;
        }
        // try next nodes
      }
    }

    if (System.getProperty(INIT_NO_ERROR_PROPERTY) != null) {
      return;
    }
    JedisClusterOperationException uninitializedException
        = new JedisClusterOperationException("Could not initialize cluster slots cache.");
    uninitializedException.addSuppressed(firstException);
    throw uninitializedException;
  }

  @Override
  public void close() {
    cache.close();
  }

  /**
   * Renews the slot cache.
   */
  public void renewSlotCache() {
    cache.renewClusterSlots(null);
  }

  /**
   * Renews the slot cache with a specific connection.
   * @param jedis Connection object.
   */
  public void renewSlotCache(Connection jedis) {
    cache.renewClusterSlots(jedis);
  }

  public Map<String, ConnectionPool> getNodes() {
    return cache.getNodes();
  }

  /**
   * Gets the node for a given slot.
   * @param slot Slot number.
   * @return HostAndPort object representing the node.
   */
  public HostAndPort getNode(int slot) {
    return slot >= 0 ? cache.getSlotNode(slot) : null;
  }

  /**
   * Gets a connection for the specified node.
   * @param node HostAndPort object representing the node.
   * @return Connection object.
   */
  public Connection getConnection(HostAndPort node) {
    return node != null ? cache.setupNodeIfNotExist(node).getResource() : getConnection();
  }

  @Override
  /**
   * Gets a connection based on command arguments.
   * @param args CommandArguments object.
   * @return Connection object.
   */
  public Connection getConnection(CommandArguments args) {
    final int slot = ((ClusterCommandArguments) args).getCommandHashSlot();
    return slot >= 0 ? getConnectionFromSlot(slot) : getConnection();
  }

  @Override
  /**
   * Gets a connection.
   * @return Connection object.
   */
  public Connection getConnection() {
    // In antirez's redis-rb-cluster implementation, getRandomConnection always
    // return valid connection (able to ping-pong) or exception if all
    // connections are invalid

    List<ConnectionPool> pools = cache.getShuffledNodesPool();

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

    JedisClusterOperationException noReachableNode = new JedisClusterOperationException("No reachable node in cluster.");
    if (suppressed != null) {
      noReachableNode.addSuppressed(suppressed);
    }
    throw noReachableNode;
  }

  /**
   * Gets a connection based on the specified slot.
   * @param slot Slot number.
   * @return Connection object.
   */
  public Connection getConnectionFromSlot(int slot) {
    ConnectionPool connectionPool = cache.getSlotPool(slot);
    if (connectionPool != null) {
      // It can't guaranteed to get valid connection because of node assignment
      return connectionPool.getResource();
    } else {
      // It's abnormal situation for cluster mode that we have just nothing for slot.
      // Try to rediscover state
      renewSlotCache();
      connectionPool = cache.getSlotPool(slot);
      if (connectionPool != null) {
        return connectionPool.getResource();
      } else {
        // no choice, fallback to new connection to random node
        return getConnection();
      }
    }
  }

  @Override
  /**
   * Gets an unmodifiable map of connections.
   * @return Map of String to ConnectionPool.
   */
  public Map<String, ConnectionPool> getConnectionMap() {
    return Collections.unmodifiableMap(getNodes());
  }
}
