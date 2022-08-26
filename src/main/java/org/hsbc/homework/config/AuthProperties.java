package org.hsbc.homework.config;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * auth config reader
 *
 * @author BruceSu
 */
public class AuthProperties {

    private static Logger log = LogManager.getLogManager().getLogger("global");
    private static AuthProperties instance = new AuthProperties();

    /**
     * token expire time after authenticate
     */
    private int tokenExpireSeconds;
    /**
     * the token container threshold size value,while the size greater than this value,it will clear the expired tokens
     */
    private int tokenResizeTrigger;

    private AuthProperties() {
        try {
            load();
        } catch (IOException e) {
            log.log(Level.SEVERE, "AuthProperties load error.", e);
        }
    }

    private void load() throws IOException {
        ResourceBundle bundle = ResourceBundle.getBundle("auth");

        this.tokenExpireSeconds = Integer.parseInt(
            this.getBundleStringOrDefault(bundle, "token_expire_seconds", "7200"));
        this.tokenResizeTrigger = Integer.parseInt(
            this.getBundleStringOrDefault(bundle, "token_resize_trigger", "1024"));
    }

    private String getBundleStringOrDefault(ResourceBundle bundle, String key, String defaultVal) {
        return bundle.containsKey(key) ? bundle.getString(key) : defaultVal;
    }

    public static AuthProperties getInstance() {
        return instance;
    }

    public int getTokenExpireSeconds() {
        return tokenExpireSeconds;
    }

    public int getTokenResizeTrigger() {
        return tokenResizeTrigger;
    }
}
