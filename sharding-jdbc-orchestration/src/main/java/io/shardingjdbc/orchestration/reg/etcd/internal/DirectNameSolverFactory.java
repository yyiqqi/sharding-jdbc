package io.shardingjdbc.orchestration.reg.etcd.internal;

import com.google.common.base.Strings;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;
import io.shardingjdbc.orchestration.reg.exception.RegException;
import lombok.val;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

/**
 * @author junxiong
 */
public class DirectNameSolverFactory extends NameResolver.Factory {
    private static final Pattern SCHEMAS = Pattern.compile("^(http|https)");

    private NameResolver.Listener listener;
    private ExecutorService executor;
    private boolean shutdown;
    private boolean resolving;
    private List<String> endpoints;
    private String scheme;

    private DirectNameSolverFactory(String scheme, List<String> endpoints) {
        this.endpoints = endpoints;
        this.scheme = scheme;
    }

    /**
     * new direct name solver factory
     *
     * @param scheme    scheme
     * @param endpoints endpoints
     * @return name solver factory
     */
    static DirectNameSolverFactory newFactory(String scheme, List<String> endpoints) {
        return new DirectNameSolverFactory(scheme, endpoints);
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        if (scheme.equals(targetUri.getPath())) {
            return new NameResolver() {
                @Override
                public String getServiceAuthority() {
                    return scheme;
                }

                @Override
                public void start(Listener listener) {
                    if (!shutdown) {
                        resolving = true;
                        for (String endpoint : endpoints) {
                            try {
                                val uri = new URI(endpoint);
                                val schema = uri.getScheme();
                                val host = uri.getHost();
                                val port = uri.getPort();
                                val path = uri.getPath();
                                if (SCHEMAS.matcher(schema).matches()
                                        && !Strings.isNullOrEmpty(path)) {
                                    val group = new EquivalentAddressGroup(new InetSocketAddress(uri.getHost(), uri.getPort()));
                                    listener.onAddresses(Collections.singletonList(group), Attributes.EMPTY);
                                }
                            } catch (URISyntaxException e) {
                                throw new RegException("Illegal endpoint, %s", e.getMessage());
                            }

                        }
                    }
                }

                @Override
                public void shutdown() {
                    if (shutdown) {
                        return;
                    }
                    shutdown = true;
                    executor = SharedResourceHolder.release(GrpcUtil.SHARED_CHANNEL_EXECUTOR, executor);
                }
            };
        } else {
            return null;
        }
    }

    @Override
    public String getDefaultScheme() {
        return null;
    }
}