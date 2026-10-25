package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.MUX;
import org.jpos.q2.QBeanSupport;
import org.jpos.util.NameRegistrar;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AppQBean extends QBeanSupport implements Configurable, Serializable {

    private Configuration configuration;
    private final org.slf4j.Logger log = LoggerFactory.getLogger(AppQBean.class);

    // ===== Transaction Managers =====
    private static volatile List<String> keFinTransactionManagerList = Collections.emptyList();
    private static final AtomicInteger keRoundRobinCounter = new AtomicInteger(0);

    private static volatile List<String> ugFinTransactionManagerList = Collections.emptyList();
    private static final AtomicInteger ugRoundRobinCounter = new AtomicInteger(0);

    private static volatile List<String> rwFinTransactionManagerList = Collections.emptyList();
    private static final AtomicInteger rwRoundRobinCounter = new AtomicInteger(0);

    private static volatile List<String> tzFinTransactionManagerList = Collections.emptyList();
    private static final AtomicInteger tzRoundRobinCounter = new AtomicInteger(0);

    private static volatile List<String> cdFinTransactionManagerList = Collections.emptyList();
    private static final AtomicInteger cdRoundRobinCounter = new AtomicInteger(0);

    private static volatile List<String> ssFinTransactionManagerList = Collections.emptyList();
    private static final AtomicInteger ssRoundRobinCounter = new AtomicInteger(0);


    private static volatile List<String> asyncFlowTransactionManagerList = Collections.emptyList();
    private static final AtomicInteger asyncFlowRoundRobinCounter = new AtomicInteger(0);

    // ===== MUX =====
    private static volatile List<String> keMuxList = Collections.emptyList();
    public static final Map<String, MUX> keMuxMap = new ConcurrentHashMap<>();
    private static final AtomicInteger keMuxCounter = new AtomicInteger(0);

    private static volatile List<String> ugMuxList = Collections.emptyList();
    public static final Map<String, MUX> ugMuxMap = new ConcurrentHashMap<>();
    private static final AtomicInteger ugMuxCounter = new AtomicInteger(0);

    private static volatile List<String> rwMuxList = Collections.emptyList();
    public static final Map<String, MUX> rwMuxMap = new ConcurrentHashMap<>();
    private static final AtomicInteger rwMuxCounter = new AtomicInteger(0);

    private static volatile List<String> tzMuxList = Collections.emptyList();
    public static final Map<String, MUX> tzMuxMap = new ConcurrentHashMap<>();
    private static final AtomicInteger tzMuxCounter = new AtomicInteger(0);

    private static volatile List<String> cdMuxList = Collections.emptyList();
    public static final Map<String, MUX> cdMuxMap = new ConcurrentHashMap<>();
    private static final AtomicInteger cdMuxCounter = new AtomicInteger(0);

    private static volatile List<String> ssMuxList = Collections.emptyList();
    public static final Map<String, MUX> ssMuxMap = new ConcurrentHashMap<>();
    private static final AtomicInteger ssMuxCounter = new AtomicInteger(0);

    // ================= ROUND ROBIN =================
    private static String next(List<String> list, AtomicInteger counter, String errorMsg) {
        if (list.isEmpty()) {
            throw new IllegalStateException(errorMsg);
        }
        int index = Math.floorMod(counter.getAndIncrement(), list.size());
        return list.get(index);
    }

    public static String nextKeFinTransactionManagerQueue() {
        return next(keFinTransactionManagerList, keRoundRobinCounter,
                "No KE Finacle transaction managers initialized");
    }

    public static String nextUgFinTransactionManagerQueue() {
        return next(ugFinTransactionManagerList, ugRoundRobinCounter,
                "No UG Finacle transaction managers initialized");
    }

    public static String nextRwFinTransactionManagerQueue() {
        return next(rwFinTransactionManagerList, rwRoundRobinCounter,
                "No RW Finacle transaction managers initialized");
    }

    public static String nextTzFinTransactionManagerQueue() {
        return next(tzFinTransactionManagerList, tzRoundRobinCounter,
                "No TZ Finacle transaction managers initialized");
    }

    public static String nextCdFinTransactionManagerQueue() {
        return next(cdFinTransactionManagerList, cdRoundRobinCounter,
                "No CD Finacle transaction managers initialized");
    }

    public static String nextSsFinTransactionManagerQueue() {
        return next(ssFinTransactionManagerList, ssRoundRobinCounter,
                "No SS Finacle transaction managers initialized");
    }
    public static String nextAsyncFlowTransactionManagerQueue() {
        return next(asyncFlowTransactionManagerList, asyncFlowRoundRobinCounter,
                "No asyncFlow transaction managers initialized");
    }
    public static String getNextMux(String countryCode) {
        switch (countryCode) {
            case "KE": return next(keMuxList, keMuxCounter, "No KE Mux initialized");
            case "UG": return next(ugMuxList, ugMuxCounter, "No UG Mux initialized");
            case "RW": return next(rwMuxList, rwMuxCounter, "No RW Mux initialized");
            case "TZ": return next(tzMuxList, tzMuxCounter, "No TZ Mux initialized");
            case "CD": return next(cdMuxList, cdMuxCounter, "No CD Mux initialized");
            case "SS": return next(ssMuxList, ssMuxCounter, "No SS Mux initialized");
            default: return null;
        }
    }

    public static Map<String, MUX> getMuxMap(String countryCode) {
        switch (countryCode) {
            case "KE": return keMuxMap;
            case "UG": return ugMuxMap;
            case "RW": return rwMuxMap;
            case "TZ": return tzMuxMap;
            case "CD": return cdMuxMap;
            case "SS": return ssMuxMap;
            default: return Collections.emptyMap();
        }
    }

    // ================= LIFECYCLE =================
    @Override
    public void setConfiguration(Configuration config) throws ConfigurationException {
        this.configuration = config;
    }

    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    protected void initService() throws ISOException {
        log.info("AppQBean Init service");
    }

    public void startService() {
        registerBean();
        initializeOnlineTransactionManagers();
        initializeMuxes();
    }

    public void stopService() {
        log.info("AppQBean Stopping AppQBean");
    }

    private void registerBean() {
        try {
            log.info("AppQBean Starting...");
            NameRegistrar.register("appQBean", this);
            log.info("appQBean registered");
        } catch (Exception e) {
            log.error("appQBean failed to register", e);
        }
    }

    // ================= INIT QUEUES =================
    private void initializeOnlineTransactionManagers() {
        log.info("Initializing Queues...");

        keFinTransactionManagerList = buildQueueList("keFinQueue_", "keFinTransactionManagerCount");
        ugFinTransactionManagerList = buildQueueList("ugFinQueue_", "ugFinTransactionManagerCount");
        rwFinTransactionManagerList = buildQueueList("rwFinQueue_", "rwFinTransactionManagerCount");
        tzFinTransactionManagerList = buildQueueList("tzFinQueue_", "tzFinTransactionManagerCount");
        cdFinTransactionManagerList = buildQueueList("cdFinQueue_", "cdFinTransactionManagerCount");
        ssFinTransactionManagerList = buildQueueList("ssFinQueue_", "ssFinTransactionManagerCount");
        asyncFlowTransactionManagerList = buildQueueList("asyncFlowQueue_", "asyncFlowTransactionManagerCount");

        log.info("KE Queues: {}", keFinTransactionManagerList);
        log.info("UG Queues: {}", ugFinTransactionManagerList);
        log.info("RW Queues: {}", rwFinTransactionManagerList);
        log.info("TZ Queues: {}", tzFinTransactionManagerList);
        log.info("CD Queues: {}", cdFinTransactionManagerList);
        log.info("SS Queues: {}", ssFinTransactionManagerList);
        log.info("asyncFlow Queues: {}", asyncFlowTransactionManagerList);
    }

    private List<String> buildQueueList(String prefix, String configKey) {
        try {
            String countStr = configuration.get(configKey);
            if (countStr == null) return Collections.emptyList();

            int count = Integer.parseInt(countStr);
            List<String> list = new ArrayList<>(count);

            for (int i = 1; i <= count; i++) {
                list.add(prefix + i);
            }
            return Collections.unmodifiableList(list);

        } catch (Exception e) {
            log.error("Error initializing {}", configKey, e);
            return Collections.emptyList();
        }
    }

    // ================= INIT MUX =================
    private void initializeMuxes() {
        log.info("Initializing MUXES...");

        initMux("ke-mux_", "kemuxCount", keMuxMap, list -> keMuxList = list);
        initMux("ug-mux_", "ugmuxCount", ugMuxMap, list -> ugMuxList = list);
        initMux("rw-mux_", "rwmuxCount", rwMuxMap, list -> rwMuxList = list);
        initMux("tz-mux_", "tzmuxCount", tzMuxMap, list -> tzMuxList = list);
        initMux("cd-mux_", "cdmuxCount", cdMuxMap, list -> cdMuxList = list);
        initMux("ss-mux_", "ssmuxCount", ssMuxMap, list -> ssMuxList = list);

        log.info("KE Mux: {}", keMuxList);
        log.info("UG Mux: {}", ugMuxList);
        log.info("RW Mux: {}", rwMuxList);
        log.info("TZ Mux: {}", tzMuxList);
        log.info("CD Mux: {}", cdMuxList);
        log.info("SS Mux: {}", ssMuxList);
    }

    private void initMux(String prefix,
                         String configKey,
                         Map<String, MUX> muxMap,
                         java.util.function.Consumer<List<String>> listSetter) {

        String countStr = configuration.get(configKey);
        if (countStr == null) return;

        int count = Integer.parseInt(countStr);
        List<String> tempList = new ArrayList<>(count);

        for (int i = 1; i <= count; i++) {
            try {
                String name = prefix + i;
                String key = "mux." + name;

                MUX mux = (MUX) NameRegistrar.get(key);
                muxMap.put(name, mux);
                tempList.add(name);

                log.info("{} FOUND: {} connected={}", key, mux, mux.isConnected());

            } catch (NameRegistrar.NotFoundException e) {
                log.error("{} NOT FOUND", prefix + i, e);
            }
        }

        listSetter.accept(Collections.unmodifiableList(tempList));
    }
}