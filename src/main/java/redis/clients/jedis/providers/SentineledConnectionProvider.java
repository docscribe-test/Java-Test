package redis.clients.jedis.providers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.IOUtils;

/**
* This class provides a sentinel connection.
*/
/**
* This class provides a sentinel connection.
*/
public class SentineledConnectionProvider implements ConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SentineledConnectionProvider.class);

  protected static final long DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS = 5000;

  private volatile HostAndPort currentMaster;

  private volatile ConnectionPool pool;

  private final String masterName;

  private final JedisClientConfig masterClientConfig;

  private final GenericObjectPoolConfig<Connection> masterPoolConfig;

  protected final Collection<SentinelListener> sentinelListeners = new ArrayList<>();

  private final JedisClientConfig sentinelClientConfig;

  private final long subscribeRetryWaitTimeMillis;

  private final Object initPoolLock = new Object();

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, /*poolConfig*/ null, sentinels, sentinelClientConfig);
  }

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig) {
    this(masterName, masterClientConfig, poolConfig, sentinels, sentinelClientConfig,
        DEFAULT_SUBSCRIBE_RETRY_WAIT_TIME_MILLIS);
  }

  public SentineledConnectionProvider(String masterName, final JedisClientConfig masterClientConfig,
      final GenericObjectPoolConfig<Connection> poolConfig,
      Set<HostAndPort> sentinels, final JedisClientConfig sentinelClientConfig,
      final long subscribeRetryWaitTimeMillis) {

    this.masterName = masterName;
    this.masterClientConfig = masterClientConfig;
    this.masterPoolConfig = poolConfig;

    this.sentinelClientConfig = sentinelClientConfig;
    this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;

    HostAndPort master = initSentinels(sentinels);
    initMaster(master);
  }

  @Override
  /**
  * Retrieves a connection from the pool.
  * @return a connection from the pool.
  */
  /**
  * Retrieves a connection from the pool.
  * @return a connection from the pool.
  */
  public Connection getConnection() {
    return pool.getResource();
  }

  @Override
  /**
  * Retrieves a connection from the pool.
  * @param args the command arguments.
  * @return a connection from the pool.
  */
  /**
  * Retrieves a connection from the pool.
  * @param args the command arguments.
  * @return a connection from the pool.
  */
  public Connection getConnection(CommandArguments args) {
    return pool.getResource();
  }

  @Override
  /**
  * Closes the connection provider, shutting down all sentinel listeners and closing the pool.
  */
  /**
  * Closes the connection provider, shutting down all sentinel listeners and closing the pool.
  */
  public void close() {
    sentinelListeners.forEach(SentinelListener::shutdown);

    pool.close();
  }

  /**
  * Retrieves the current master.
  * @return the current master.
  */
  /**
  * Retrieves the current master.
  * @return the current master.
  */
  public HostAndPort getCurrentMaster() {
    return currentMaster;
  }

  /**
  * Initializes the master.
  * @param master the master to be initialized.
  */
  /**
  * Initializes the master.
  * @param master the master to be initialized.
  */
  private void initMaster(HostAndPort master) {
    synchronized (initPoolLock) {
      if (!master.equals(currentMaster)) {
        currentMaster = master;

        ConnectionPool newPool = masterPoolConfig != null
            ? new ConnectionPool(currentMaster, masterClientConfig, masterPoolConfig)
            : new ConnectionPool(currentMaster, masterClientConfig);

        ConnectionPool existingPool = pool;
        pool = newPool;
        LOG.info("Created connection pool to master at {}.", master);

        if (existingPool != null) {
          // although we clear the pool, we still have to check the returned object in getResource,
          // this call only clears idle instances, not borrowed instances
          // existingPool.clear(); // necessary??
          existingPool.close();
        }
      }
    }
  }

  /**
  * Initializes the sentinels.
  * @param sentinels the set of sentinels.
  * @return the master.
  */
  /**
  * Initializes the sentinels.
  * @param sentinels the set of sentinels.
  * @return the master.
  */
  private HostAndPort initSentinels(Set<HostAndPort> sentinels) {

    HostAndPort master = null;
    boolean sentinelAvailable = false;

    LOG.debug("Trying to find master from available sentinels...");

    for (HostAndPort sentinel : sentinels) {

      LOG.debug("Connecting to Sentinel {}...", sentinel);

      try (Jedis jedis = new Jedis(sentinel, sentinelClientConfig)) {

        List<String> masterAddr = jedis.sentinelGetMasterAddrByName(masterName);

        // connected to sentinel...
        sentinelAvailable = true;

        if (masterAddr == null || masterAddr.size() != 2) {
          LOG.warn("Sentinel {} is not monitoring master {}.", sentinel, masterName);
          continue;
        }

        master = toHostAndPort(masterAddr);
        LOG.debug("Redis master reported at {}.", master);
        break;
      } catch (JedisException e) {
        // resolves #1036, it should handle JedisException there's another chance
        // of raising JedisDataException
        LOG.warn("Could not get master address from {}.", sentinel, e);
      }
    }

    if (master == null) {
      if (sentinelAvailable) {
        // can connect to sentinel, but master name seems to not monitored
        throw new JedisException(
            "Can connect to sentinel, but " + masterName + " seems to be not monitored.");
      } else {
        throw new JedisConnectionException(
            "All sentinels down, cannot determine where " + masterName + " is running.");
      }
    }

    LOG.info("Redis master running at {}. Starting sentinel listeners...", master);

    for (HostAndPort sentinel : sentinels) {

      SentinelListener listener = new SentinelListener(sentinel);
      // whether SentinelListener threads are alive or not, process can be stopped
      listener.setDaemon(true);
      sentinelListeners.add(listener);
      listener.start();
    }

    return master;
  }

  /**
   * Must be of size 2.
   * @param masterAddr master address
   */
  /**
  * Converts a list containing a host and port to a HostAndPort object.
  * @param masterAddr a list containing a host and port.
  * @return a HostAndPort object.
  */
  /**
  * Converts a list containing a host and port to a HostAndPort object.
  * @param masterAddr a list containing a host and port.
  * @return a HostAndPort object.
  */
  private static HostAndPort toHostAndPort(List<String> masterAddr) {
    return toHostAndPort(masterAddr.get(0), masterAddr.get(1));
  }

  /**
  * Converts a host string and a port string to a HostAndPort object.
  * @param hostStr the host string.
  * @param portStr the port string.
  * @return a HostAndPort object.
  */
  /**
  * Converts a host string and a port string to a HostAndPort object.
  * @param hostStr the host string.
  * @param portStr the port string.
  * @return a HostAndPort object.
  */
  private static HostAndPort toHostAndPort(String hostStr, String portStr) {
    return new HostAndPort(hostStr, Integer.parseInt(portStr));
  }

  /**
  * Thread that listens for sentinel updates.
  */
  /**
  * Thread that listens for sentinel updates.
  */
  /**
  * Thread that listens for sentinel updates.
  */
  protected class SentinelListener extends Thread {

    protected final HostAndPort node;
    protected volatile Jedis sentinelJedis;
    protected AtomicBoolean running = new AtomicBoolean(false);

    /**
    * Constructs a new SentinelListener.
    * @param node the node to listen to.
    */
    /**
    * Constructs a new SentinelListener with the specified node.
    * @param node the node to listen to.
    */
    /**
    * Constructs a new SentinelListener with the specified node.
    * @param node the node to listen to.
    */
    public SentinelListener(HostAndPort node) {
      super(String.format("%s-SentinelListener-[%s]", masterName, node.toString()));
      this.node = node;
    }

    @Override
    /**
    * The main execution method for the SentinelListener thread.
    */
    /**
    * The main execution method for the SentinelListener thread.
    */
    /**
    * The main execution method for the SentinelListener thread.
    */
    public void run() {

      running.set(true);

      while (running.get()) {

        try {
          // double check that it is not being shutdown
          if (!running.get()) {
            break;
          }

          sentinelJedis = new Jedis(node, sentinelClientConfig);

          // code for active refresh
          List<String> masterAddr = sentinelJedis.sentinelGetMasterAddrByName(masterName);
          if (masterAddr == null || masterAddr.size() != 2) {
            LOG.warn("Can not get master {} address. Sentinel: {}.", masterName, node);
          } else {
            initMaster(toHostAndPort(masterAddr));
          }

          sentinelJedis.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
              LOG.debug("Sentinel {} published: {}.", node, message);

              String[] switchMasterMsg = message.split(" ");

              if (switchMasterMsg.length > 3) {

                if (masterName.equals(switchMasterMsg[0])) {
                  initMaster(toHostAndPort(switchMasterMsg[3], switchMasterMsg[4]));
                } else {
                  LOG.debug(
                    "Ignoring message on +switch-master for master {}. Our master is {}.",
                    switchMasterMsg[0], masterName);
                }

              } else {
                LOG.error("Invalid message received on sentinel {} on channel +switch-master: {}.",
                    node, message);
              }
            }
          }, "+switch-master");

        } catch (JedisException e) {

          if (running.get()) {
            LOG.error("Lost connection to sentinel {}. Sleeping {}ms and retrying.", node,
                subscribeRetryWaitTimeMillis, e);
            try {
              Thread.sleep(subscribeRetryWaitTimeMillis);
            } catch (InterruptedException se) {
              LOG.error("Sleep interrupted.", se);
            }
          } else {
            LOG.debug("Unsubscribing from sentinel {}.", node);
          }
        } finally {
          IOUtils.closeQuietly(sentinelJedis);
        }
      }
    }

    // must not throw exception
    /**
    * Shuts down the SentinelListener thread.
    */
    /**
    * Shuts down the SentinelListener thread.
    */
    /**
    * Shuts down the SentinelListener thread.
    */
    public void shutdown() {
      try {
        LOG.debug("Shutting down listener on {}.", node);
        running.set(false);
        // This isn't good, the Jedis object is not thread safe
        if (sentinelJedis != null) {
          sentinelJedis.close();
        }
      } catch (RuntimeException e) {
        LOG.error("Error while shutting down.", e);
      }
    }
  }
}
