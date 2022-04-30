package com.ryanheise.just_audio;

import android.content.Context;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;

public class CacheDataSourceFactory implements DataSource.Factory {
    private final Context context;
    private final DefaultDataSourceFactory defaultDatasourceFactory;
    private static SimpleCache sDownloadCache;
    private final ExoDatabaseProvider databaseProvider;

    private static final int MAX_AUDIO_CACHE_SIZE_IN_BYTES = 500 * 1024 * 1024;  // 500MB


    private SimpleCache getCache(Context context) {
        if (sDownloadCache != null) {
            return sDownloadCache;
        }

        LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(MAX_AUDIO_CACHE_SIZE_IN_BYTES);

//       sDownloadCache = new SimpleCache(new File(context.getCacheDir(), "media"), evictor);
        sDownloadCache = new SimpleCache(new File(context.getExternalCacheDir(), "media_audio"), evictor, databaseProvider);
        return sDownloadCache;
    }

    public void releaseCache() {
        sDownloadCache.release();
        sDownloadCache = null;
    }

    CacheDataSourceFactory(Context context) {
        super();
        this.context = context;
        String userAgent = Util.getUserAgent(context, "just_audio");
        DefaultBandwidthMeter bandwidthMeter =  DefaultBandwidthMeter.getSingletonInstance(context);
//        defaultDatasourceFactory = new DefaultDataSourceFactory(this.context,
//                bandwidthMeter,
//                new DefaultHttpDataSourceFactory(userAgent, null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true));


        defaultDatasourceFactory = new DefaultDataSourceFactory(this.context,
                bandwidthMeter,
                new DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).setUserAgent(userAgent)
                );

        databaseProvider = new ExoDatabaseProvider(context);
    }

    @Override
    public DataSource createDataSource() {
        return new CacheDataSource(getCache(this.context), defaultDatasourceFactory.createDataSource(),
                new FileDataSource(), new CacheDataSink(getCache(this.context), 2 * 1024 * 1024),
                CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null);
    }
}