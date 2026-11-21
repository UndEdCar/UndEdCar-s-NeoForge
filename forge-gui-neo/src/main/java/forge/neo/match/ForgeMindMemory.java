
package forge.neo.match;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import forge.localinstance.properties.ForgeConstants;

/**
 * Persistent storage for ForgeMind's learned gameplay observations.
 *
 * This data is kept separate from neo.properties, which stores
 * interface preferences.
 */
public final class ForgeMindMemory {
    private static final Logger LOGGER =
            Logger.getLogger(ForgeMindMemory.class.getName());

    private static final String FILE_NAME = "forgemind.properties";
    private static final String PROFILE_PREFIX = "profile.";
    private static final String PROFILE_ID_KEY = "profile.id";

    private static final ForgeMindMemory INSTANCE =
            new ForgeMindMemory();

    private final File file;
    private final Properties values = new Properties();

    private ForgeMindMemory() {
        file = new File(ForgeConstants.USER_PREFS_DIR, FILE_NAME);
        load();
    }

    public static ForgeMindMemory getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the persistent local learning profile ID.
     * Creates and saves one on the first call.
     */
    public synchronized String getOrCreateProfileId() {
        final String stored = values.getProperty(PROFILE_ID_KEY);

        if (stored != null) {
            try {
                UUID.fromString(stored);
                return stored;
            } catch (final IllegalArgumentException ex) {
                LOGGER.log(Level.WARNING,
                        "Invalid ForgeMind profile ID; creating a new one", ex);
            }
        }

        final String profileId = UUID.randomUUID().toString();
        values.setProperty(PROFILE_ID_KEY, profileId);
        save();
        return profileId;
    }

    public synchronized long getObservationCount(
            final String profileId,
            final String observationId) {

        final String key = key(profileId, observationId);
        final String stored = values.getProperty(key);

        if (stored == null) {
            return 0L;
        }

        try {
            return Long.parseLong(stored);
        } catch (final NumberFormatException ex) {
            LOGGER.log(Level.WARNING,
                    "Ignoring invalid ForgeMind observation count: " + key, ex);
            return 0L;
        }
    }

    public synchronized void recordObservation(
            final String profileId,
            final String observationId) {

        final String key = key(profileId, observationId);
        final long current =
                getObservationCount(profileId, observationId);

        values.setProperty(key, Long.toString(current + 1L));
    }

    public synchronized boolean save() {
        final File parent = file.getParentFile();

        if (parent != null && !parent.exists()
                && !parent.mkdirs() && !parent.isDirectory()) {
            LOGGER.warning(
                    "Could not create ForgeMind data directory: " + parent);
            return false;
        }

        try (FileOutputStream output = new FileOutputStream(file)) {
            values.store(output, "ForgeMind learned gameplay observations");
            return true;
        } catch (final IOException ex) {
            LOGGER.log(Level.WARNING,
                    "Could not save ForgeMind memory to " + file, ex);
            return false;
        }
    }

    private synchronized void load() {
        if (!file.isFile()) {
            return;
        }

        try (FileInputStream input = new FileInputStream(file)) {
            values.load(input);
        } catch (final IOException ex) {
            values.clear();
            LOGGER.log(Level.WARNING,
                    "Could not load ForgeMind memory from " + file, ex);
        }
    }

    private static String key(
            final String profileId,
            final String observationId) {

        return PROFILE_PREFIX
                + encode(requireId(profileId, "profileId"))
                + "."
                + encode(requireId(observationId, "observationId"));
    }

    private static String requireId(
            final String value,
            final String name) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    name + " must not be empty");
        }

        return value;
    }

    private static String encode(final String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
