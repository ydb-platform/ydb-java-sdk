package tech.ydb.coordination.scenario.configuration;

/**
 * @author Kirill Kurdyukov
 */
public interface ConfigurationSubscriberObserver {

    void onNext(byte[] configurationData);
}
