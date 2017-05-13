package cm.android.preference.ext;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class PrefsConfig {

    public static interface Factory {
        SharedPreferences getSharedPreferences(Context context, String name);
    }

    public static final class Builder {
        private String authority;
        private PrefsConfig.Factory factory;

        public Builder() {
            authority = "";
            factory = new Factory() {
                @Override
                public SharedPreferences getSharedPreferences(Context context, String name) {
                    return context.getSharedPreferences(name, Context.MODE_PRIVATE);
                }
            };
        }

        public Builder authority(String authority) {
            if (TextUtils.isEmpty(authority)) {
                throw new IllegalArgumentException("authority = " + authority);
            }
            this.authority = authority;
            return this;
        }

        public Builder factory(PrefsConfig.Factory factory) {
            if (factory != null) {
                this.factory = factory;
            }
            return this;
        }

        public PrefsConfig build() {
            return new PrefsConfig(this);
        }
    }


    private String authority;
    private PrefsConfig.Factory factory;

    private PrefsConfig(Builder builder) {
        authority = builder.authority;
        factory = builder.factory;
    }

    public String getAuthority() {
        return authority;
    }

    public PrefsConfig.Factory getFactory() {
        return factory;
    }
}
