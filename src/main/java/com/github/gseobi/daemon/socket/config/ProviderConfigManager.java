package com.github.gseobi.daemon.socket.config;

import com.github.gseobi.daemon.socket.common.Constant;
import com.github.gseobi.daemon.socket.config.model.ProviderConfig;
import com.github.gseobi.daemon.socket.crypto.constant.EncryptionType;
import com.github.gseobi.daemon.socket.crypto.constant.OutputFormat;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ProviderConfigManager {

    private static final Map<String, ProviderConfig> PROVIDER_SETTINGS = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_MODIFIED_MAP = new ConcurrentHashMap<>();

    public static void initAllConfigs() {
        try {
            File dir = new File(resolveConfDirPath());
            if (!dir.exists() || !dir.isDirectory()) {
                log.warn("[ProviderConfigManager] Config dir not found: {}", dir.getAbsolutePath());
                return;
            }

            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(Constant.CONF_FILE_EXTENSION));
            if (files == null || files.length == 0) {
                log.info("[ProviderConfigManager] No config files found.");
                return;
            }

            for (File file : files) {
                String providerCode = file.getName().substring(0, file.getName().lastIndexOf("."));
                getConfig(providerCode);
            }

            log.info("[ProviderConfigManager] Total {} provider config loaded.", files.length);
        } catch (Exception e) {
            log.error("[ProviderConfigManager] initAllConfigs exception", e);
        }
    }

    public static ProviderConfig getConfig(String providerCode) {
        String path = resolveConfDirPath() + File.separator + providerCode + Constant.CONF_FILE_EXTENSION;
        File file = new File(path);

        if (!file.exists()) {
            log.warn("[ProviderConfigManager] Config file not found: {}", path);
            return PROVIDER_SETTINGS.get(providerCode);
        }

        long currentModified = file.lastModified();
        Long lastModified = LAST_MODIFIED_MAP.get(path);

        if (lastModified == null || currentModified > lastModified) {
            synchronized (ProviderConfigManager.class) {
                long checked = LAST_MODIFIED_MAP.getOrDefault(path, 0L);
                if (currentModified > checked) {
                    log.info("[ProviderConfigManager] File changed. Reloading: {}", path);
                    reload(providerCode, file);
                    LAST_MODIFIED_MAP.put(path, currentModified);
                }
            }
        }

        return PROVIDER_SETTINGS.get(providerCode);
    }

    private static void reload(String providerCode, File file) {
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);

            ProviderConfig config = new ProviderConfig();
            config.setIp(props.getProperty("IP", ""));
            config.setPort(parseInt(props.getProperty("PORT", "0")));

            config.setEncType(parseEncType(props.getProperty("ENC_TYPE", "AES_128")));
            config.setEncKey(props.getProperty("ENC_KEY", ""));
            config.setEncIv(props.getProperty("ENC_IV", ""));
            config.setEncOut(parseEncOut(props.getProperty("ENC_OUT", "BASE64")));

            config.setEncryptOutbound(Boolean.parseBoolean(props.getProperty("ENC_OUTBOUND", "false")));

            PROVIDER_SETTINGS.put(providerCode, config);
            log.info("[ProviderConfigManager] Provider {} reload complete.", providerCode);

        } catch (Exception e) {
            log.error("[ProviderConfigManager] Error loading config (provider: {})", providerCode, e);
        }
    }

    private static String resolveConfDirPath() {
        String sys = System.getProperty(Constant.CONF_DIR_SYS_PROP);
        if (sys != null && !sys.isBlank()) return sys;

        String env = System.getenv(Constant.CONF_DIR_ENV);
        if (env != null && !env.isBlank()) return env;

        return Constant.DEFAULT_CONF_DIR;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static EncryptionType parseEncType(String s) {
        try { return EncryptionType.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return EncryptionType.AES_128; }
    }

    private static OutputFormat parseEncOut(String s) {
        try { return OutputFormat.valueOf(s.trim().toUpperCase()); }
        catch (Exception e) { return OutputFormat.BASE64; }
    }
}
