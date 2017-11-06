package io.shardingjdbc.orchestration.reg.base;

import io.shardingjdbc.orchestration.internal.util.IpUtils;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;
import lombok.val;

import java.lang.management.ManagementFactory;

/**
 * Shard JDBC Instance
 *
 * @author junxiong
 */
@Value
@Wither
@AllArgsConstructor
public class ShardInstance {
    /**
     * Delimiter of instance id
     */
    private static final String DELIMITER = "@-@";

    /**
     * sharding-jdbc instance identity
     */
    String id;
    /**
     * instance state, either ENABLED or DISABLED
     */
    ShardState state;

    /**
     * local sharding jdbc instance
     *
     * @return Sharding jdbc instance
     */
    public static ShardInstance localInstance() {
        val javaVmName = ManagementFactory.getRuntimeMXBean().getName();
        val id = IpUtils.getIp() + DELIMITER + javaVmName.split("@")[0];
        return new ShardInstance(id, ShardState.ENABLED);
    }
}