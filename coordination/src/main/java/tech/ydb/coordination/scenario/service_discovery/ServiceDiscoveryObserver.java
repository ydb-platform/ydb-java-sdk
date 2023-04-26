package tech.ydb.coordination.scenario.service_discovery;

import java.util.List;

/**
 * @author Kirill Kurdyukov
 */
public interface ServiceDiscoveryObserver {

    void onNext(List<String> endpoints);
}
