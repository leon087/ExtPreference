package cm.android.preference.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.database.Cursor;

import java.io.Closeable;

public final class Util {

    private Util() {
    }

    public static Logger getLogger() {
        return LoggerFactory.getLogger("extpreference");
    }

    /**
     * Closes 'closeable', ignoring any checked exceptions. Does nothing if
     * 'closeable' is null.
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (RuntimeException rethrown) {
            throw rethrown;
        } catch (Exception e) {
            Util.getLogger().error("", e);
        }
    }

    public static void closeQuietly(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

}
