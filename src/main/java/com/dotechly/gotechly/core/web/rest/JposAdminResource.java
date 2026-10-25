package com.dotechly.gotechly.core.web.rest;

import org.jpos.transaction.TransactionManager;
import org.jpos.util.NameRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Admin REST endpoints for jPOS TransactionManager monitoring and scaling.
 * <p>
 * Metrics are read directly from jPOS {@link TransactionManager} instances
 * discovered via {@link NameRegistrar}. Scaling is done by modifying jPOS
 * deploy XML files — Q2 detects the changes and hot-reloads automatically.
 */
@RestController
@RequestMapping("/api/v1/admin/jpos")
public class JposAdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(JposAdminResource.class);

    @Value("${gotechly.q2.deploy-dir:deploy}")
    private String deployDir;

    /** Cache: TM name → deploy XML file path. Invalidated on scaling operations. */
    private final Map<String, File> tmFileCache = new ConcurrentHashMap<>();

    /** Cache: TM name → int[]{sessions, maxSessions}. Invalidated on scaling operations. */
    private final Map<String, int[]> sessionConfigCache = new ConcurrentHashMap<>();

    // =========================================================================
    // Metrics
    // =========================================================================

    /**
     * GET /api/v1/admin/jpos/metrics
     * Returns runtime metrics for all jPOS TransactionManagers.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        LOG.debug("REST request to get jPOS TransactionManager metrics");

        List<Map<String, Object>> tmMetrics = new ArrayList<>();
        int totalConfiguredSessions = 0;
        int totalMaxSessions = 0;
        int totalActiveSessions = 0;
        int totalOutstanding = 0;
        long totalInTransit = 0;

        for (Map.Entry<String, TransactionManager> entry : discoverTransactionManagers().entrySet()) {
            String name = entry.getKey();
            TransactionManager tm = entry.getValue();

            Map<String, Object> metrics = collectTmMetrics(name, tm);
            tmMetrics.add(metrics);

            totalConfiguredSessions += (int) metrics.get("sessions");
            totalMaxSessions += (int) metrics.get("maxSessions");
            totalActiveSessions += (int) metrics.get("activeSessions");
            totalOutstanding += (int) metrics.get("outstandingTransactions");
            totalInTransit += (long) metrics.get("inTransit");
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTransactionManagers", tmMetrics.size());
        summary.put("totalConfiguredSessions", totalConfiguredSessions);
        summary.put("totalMaxSessions", totalMaxSessions);
        summary.put("totalActiveSessions", totalActiveSessions);
        summary.put("totalIdleSessions", totalConfiguredSessions - totalActiveSessions);
        summary.put("totalOutstandingTransactions", totalOutstanding);
        summary.put("totalInTransit", totalInTransit);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("transactionManagers", tmMetrics);
        response.put("summary", summary);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/admin/jpos/metrics/{tmName}
     * Returns runtime metrics for a specific TransactionManager.
     */
    @GetMapping("/metrics/{tmName}")
    public ResponseEntity<Map<String, Object>> getTmMetrics(@PathVariable String tmName) {
        LOG.debug("REST request to get jPOS metrics for TM '{}'", tmName);

        TransactionManager tm = lookupTm(tmName);
        if (tm == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(collectTmMetrics(tmName, tm));
    }

    // =========================================================================
    // Scaling — Sessions
    // =========================================================================

    /**
     * POST /api/v1/admin/jpos/scale/sessions
     * Updates sessions and/or max-sessions for a TransactionManager by modifying
     * its deploy XML. Q2 detects the change and hot-reloads the TM.
     * <p>
     * Body: {@code {"tmName": "mpesaTransactionManager_1", "sessions": 15, "maxSessions": 30}}
     */
    @PostMapping("/scale/sessions")
    public ResponseEntity<Map<String, Object>> scaleSessions(@RequestBody Map<String, Object> request) {
        String tmName = (String) request.get("tmName");
        Integer sessions = request.get("sessions") != null
                ? ((Number) request.get("sessions")).intValue() : null;
        Integer maxSessions = request.get("maxSessions") != null
                ? ((Number) request.get("maxSessions")).intValue() : null;

        if (tmName == null || tmName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "tmName is required"));
        }
        if (sessions == null && maxSessions == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessions or maxSessions required"));
        }
        if (sessions != null && sessions < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessions must be >= 1"));
        }
        if (maxSessions != null && maxSessions < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "maxSessions must be >= 1"));
        }
        if (sessions != null && maxSessions != null && sessions > maxSessions) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessions cannot exceed maxSessions"));
        }

        LOG.info("Scaling TM '{}': sessions={}, maxSessions={}", tmName, sessions, maxSessions);

        File xmlFile = findTmDeployFile(tmName);
        if (xmlFile == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Deploy XML not found for TM '" + tmName + "' in " + deployDir));
        }

        try {
            updateTmXml(xmlFile, sessions, maxSessions);
            sessionConfigCache.remove(tmName);
            LOG.info("Updated deploy XML '{}' for TM '{}'. Q2 will hot-reload.",
                    xmlFile.getName(), tmName);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tmName", tmName);
            result.put("xmlFile", xmlFile.getName());
            if (sessions != null) result.put("sessions", sessions);
            if (maxSessions != null) result.put("maxSessions", maxSessions);
            result.put("status", "XML updated — Q2 will hot-reload the TransactionManager");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOG.error("Failed to update deploy XML for TM '{}': {}", tmName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to update XML: " + e.getMessage()));
        }
    }

    // =========================================================================
    // Scaling — TM Count
    // =========================================================================

    /**
     * POST /api/v1/admin/jpos/scale/tm-count
     * Adds or removes TransactionManager instances by generating/deleting deploy XMLs
     * and updating the bean XML. Q2 detects changes and hot-reloads.
     * <p>
     * Body: {@code {"prefix": "mpesa", "count": 4}}
     * <p>
     * This expects the naming convention:
     * <ul>
     *   <li>TM XMLs: {@code {prefix}-tm-1.xml}, {@code {prefix}-tm-2.xml}, ...</li>
     *   <li>Bean XML: {@code {prefix}-bean.xml} with property {@code {prefix}TransactionManagersCount}</li>
     * </ul>
     */
    @PostMapping("/scale/tm-count")
    public ResponseEntity<Map<String, Object>> scaleTmCount(@RequestBody Map<String, Object> request) {
        String prefix = (String) request.get("prefix");
        Integer desiredCount = request.get("count") != null
                ? ((Number) request.get("count")).intValue() : null;

        if (prefix == null || prefix.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "prefix is required"));
        }
        if (desiredCount == null || desiredCount < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "count must be >= 1"));
        }

        LOG.info("Scaling TM count for prefix '{}' to {}", prefix, desiredCount);

        File dir = resolveDeployDir();
        if (dir == null) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Deploy directory not found: " + deployDir));
        }

        try {
            // Find existing TM XMLs matching pattern {prefix}-tm-{N}.xml
            File[] existingTmFiles = dir.listFiles(
                    (d, name) -> name.matches(prefix + "-tm-\\d+\\.xml"));
            int currentCount = existingTmFiles != null ? existingTmFiles.length : 0;

            if (desiredCount == currentCount) {
                return ResponseEntity.ok(Map.of(
                        "prefix", prefix,
                        "currentCount", currentCount,
                        "desiredCount", desiredCount,
                        "status", "No change needed"));
            }

            List<String> added = new ArrayList<>();
            List<String> removed = new ArrayList<>();

            if (desiredCount > currentCount) {
                // Scale UP: use the first TM XML as template
                File template = new File(dir, prefix + "-tm-1.xml");
                if (!template.exists()) {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", "Template file '" + template.getName() + "' not found"));
                }

                for (int i = currentCount + 1; i <= desiredCount; i++) {
                    File newFile = generateTmXml(template, prefix, i);
                    added.add(newFile.getName());
                    LOG.info("Generated new TM deploy XML: {}", newFile.getName());
                }
            } else {
                // Scale DOWN: remove highest-numbered TM XMLs
                // Sort descending by number to remove from the end
                Arrays.sort(existingTmFiles, (a, b) ->
                        extractTmNumber(b.getName(), prefix) - extractTmNumber(a.getName(), prefix));
                for (int i = 0; i < currentCount - desiredCount; i++) {
                    File toRemove = existingTmFiles[i];
                    Files.delete(toRemove.toPath());
                    removed.add(toRemove.getName());
                    LOG.info("Removed TM deploy XML: {}", toRemove.getName());
                }
            }

            // Update bean XML with new count
            File beanXml = new File(dir, prefix + "-bean.xml");
            if (beanXml.exists()) {
                updateBeanXmlCount(beanXml, prefix, desiredCount);
                LOG.info("Updated bean XML '{}' with count={}", beanXml.getName(), desiredCount);
            } else {
                LOG.warn("Bean XML '{}' not found — TM count property not updated", prefix + "-bean.xml");
            }

            // Invalidate caches — TM files and session configs may have changed
            tmFileCache.clear();
            sessionConfigCache.clear();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("prefix", prefix);
            result.put("previousCount", currentCount);
            result.put("newCount", desiredCount);
            if (!added.isEmpty()) result.put("added", added);
            if (!removed.isEmpty()) result.put("removed", removed);
            result.put("status", "Deploy XMLs updated — Q2 will hot-reload");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            LOG.error("Failed to scale TM count for prefix '{}': {}", prefix, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to scale TM count: " + e.getMessage()));
        }
    }

    // =========================================================================
    // Helpers — Metrics
    // =========================================================================

    private Map<String, Object> collectTmMetrics(String name, TransactionManager tm) {
        int activeSessions = tm.getActiveSessions();
        int outstanding = tm.getOutstandingTransactions();
        long inTransit = tm.getInTransit();

        // Read configured sessions and max-sessions from the TM's deploy XML
        // (TransactionManager MBean doesn't expose the initial 'sessions' config value)
        int[] xmlConfig = readConfiguredSessionsFromXml(name);
        int configuredSessions = xmlConfig[0];
        int maxSessions = xmlConfig[1];

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("name", name);
        metrics.put("sessions", configuredSessions);
        metrics.put("maxSessions", maxSessions);
        metrics.put("activeSessions", activeSessions);
        metrics.put("idleSessions", Math.max(0, configuredSessions - activeSessions));
        metrics.put("outstandingTransactions", outstanding);
        metrics.put("inTransit", inTransit);
        metrics.put("tpsAvg", tm.getTPSAvg());
        metrics.put("tpsPeak", tm.getTPSPeak());
        metrics.put("tps", tm.getTPSAsString());

        Date peakWhen = tm.getTPSPeakWhen();
        if (peakWhen != null) {
            metrics.put("tpsPeakWhen", peakWhen.toString());
        }

        return metrics;
    }

    /**
     * Reads the configured 'sessions' and 'max-sessions' values from the TM's deploy XML.
     * Results are cached — cache is invalidated on scaling operations.
     *
     * @return int[2]: [sessions, maxSessions]. -1 if not found.
     */
    private int[] readConfiguredSessionsFromXml(String tmName) {
        int[] cached = sessionConfigCache.get(tmName);
        if (cached != null) return cached;

        int sessions = -1;
        int maxSessions = -1;
        File xmlFile = findTmDeployFile(tmName);
        if (xmlFile == null) return new int[]{sessions, maxSessions};
        try {
            Document doc = parseXml(xmlFile);
            NodeList props = doc.getElementsByTagName("property");
            for (int i = 0; i < props.getLength(); i++) {
                Element prop = (Element) props.item(i);
                String propName = prop.getAttribute("name");
                if ("sessions".equals(propName)) {
                    sessions = Integer.parseInt(prop.getAttribute("value"));
                } else if ("max-sessions".equals(propName)) {
                    maxSessions = Integer.parseInt(prop.getAttribute("value"));
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not read sessions from '{}': {}", xmlFile.getName(), e.getMessage());
        }
        int[] result = new int[]{sessions, maxSessions};
        sessionConfigCache.put(tmName, result);
        return result;
    }

    private Map<String, TransactionManager> discoverTransactionManagers() {
        Map<String, TransactionManager> result = new TreeMap<>();
        try {
            Map<String, Object> registrar = NameRegistrar.getAsMap();
            for (Map.Entry<String, Object> entry : registrar.entrySet()) {
                if (entry.getValue() instanceof TransactionManager tm) {
                    result.put(entry.getKey(), tm);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to discover TransactionManagers from NameRegistrar: {}", e.getMessage(), e);
        }
        return result;
    }

    private TransactionManager lookupTm(String tmName) {
        try {
            Object obj = NameRegistrar.getIfExists(tmName);
            if (obj instanceof TransactionManager tm) {
                return tm;
            }
        } catch (Exception e) {
            LOG.warn("Failed to lookup TM '{}': {}", tmName, e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // Helpers — XML Modification
    // =========================================================================

    private File resolveDeployDir() {
        File dir = new File(deployDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), deployDir);
        }
        return dir.exists() && dir.isDirectory() ? dir : null;
    }

    /**
     * Finds the deploy XML file for a given TM name by scanning all XML files
     * in the deploy directory and matching the 'name' attribute on the root element.
     * Results are cached — cache is invalidated on scaling operations.
     */
    private File findTmDeployFile(String tmName) {
        File cached = tmFileCache.get(tmName);
        if (cached != null && cached.exists()) return cached;

        File dir = resolveDeployDir();
        if (dir == null) return null;

        File[] xmlFiles = dir.listFiles((d, name) -> name.endsWith(".xml"));
        if (xmlFiles == null) return null;

        for (File xmlFile : xmlFiles) {
            try {
                Document doc = parseXml(xmlFile);
                Element root = doc.getDocumentElement();
                String name = root.getAttribute("name");
                if (name != null && !name.isEmpty()) {
                    tmFileCache.put(name, xmlFile);
                }
                if (tmName.equals(name)) {
                    return xmlFile;
                }
            } catch (Exception e) {
                // Skip unparseable files
            }
        }
        return null;
    }

    private void updateTmXml(File xmlFile, Integer sessions, Integer maxSessions) throws Exception {
        Document doc = parseXml(xmlFile);
        NodeList props = doc.getElementsByTagName("property");

        for (int i = 0; i < props.getLength(); i++) {
            Element prop = (Element) props.item(i);
            String propName = prop.getAttribute("name");
            if ("sessions".equals(propName) && sessions != null) {
                prop.setAttribute("value", String.valueOf(sessions));
            } else if ("max-sessions".equals(propName) && maxSessions != null) {
                prop.setAttribute("value", String.valueOf(maxSessions));
            }
        }

        writeXml(doc, xmlFile);
    }

    /**
     * Generates a new TM deploy XML by copying the template and updating:
     * - name attribute: {prefix}TransactionManager_{n}
     * - queue property: {prefix}B2CQueue_{n}
     * - space property: tspace:{prefix}B2CSpace_{n}
     */
    private File generateTmXml(File template, String prefix, int number) throws Exception {
        Document doc = parseXml(template);
        Element root = doc.getDocumentElement();

        // Update TM name
        root.setAttribute("name", prefix + "TransactionManager_" + number);

        // Update queue and space properties
        NodeList props = doc.getElementsByTagName("property");
        for (int i = 0; i < props.getLength(); i++) {
            Element prop = (Element) props.item(i);
            String propName = prop.getAttribute("name");
            if ("queue".equals(propName)) {
                prop.setAttribute("value", prefix + "B2CQueue_" + number);
            } else if ("space".equals(propName)) {
                String oldVal = prop.getAttribute("value");
                // Preserve the tspace: prefix
                if (oldVal.startsWith("tspace:")) {
                    prop.setAttribute("value", "tspace:" + prefix + "B2CSpace_" + number);
                }
            }
        }

        File outputFile = new File(template.getParentFile(), prefix + "-tm-" + number + ".xml");
        writeXml(doc, outputFile);
        return outputFile;
    }

    /**
     * Updates the TransactionManagersCount property in the bean XML.
     */
    private void updateBeanXmlCount(File beanXml, String prefix, int count) throws Exception {
        Document doc = parseXml(beanXml);
        NodeList props = doc.getElementsByTagName("property");

        String countPropName = prefix + "TransactionManagersCount";
        for (int i = 0; i < props.getLength(); i++) {
            Element prop = (Element) props.item(i);
            if (countPropName.equals(prop.getAttribute("name"))) {
                prop.setAttribute("value", String.valueOf(count));
                break;
            }
        }

        writeXml(doc, beanXml);
    }

    private int extractTmNumber(String fileName, String prefix) {
        // Extract N from {prefix}-tm-{N}.xml
        String pattern = prefix + "-tm-(\\d+)\\.xml";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(fileName);
        return m.matches() ? Integer.parseInt(m.group(1)) : 0;
    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    private void writeXml(Document doc, File file) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        // Write to temp file first, then move atomically
        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        transformer.transform(new DOMSource(doc), new StreamResult(tempFile));
        Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
