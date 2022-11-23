package org.pygisserver.render.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @Description: 根据行列号，查看索引文件
 * @author: 刘洋 
 * @date: 2022-03-23
 */
public class BundlxCache {
    public static class CacheKey {
        public final int zoom;

        public final int row;

        public final int col;
        
        public final String layerName;

        public CacheKey(int zoom, int row, int col,String layerName) {
            this.zoom = zoom;
            this.row = row;
            this.col = col;
            this.layerName = layerName;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (zoom != cacheKey.zoom) return false;
            if (row != cacheKey.row) return false;
            if (col != cacheKey.col) return false;
            return layerName.equals(cacheKey.layerName);
        }

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + col;
			result = prime * result + ((layerName == null) ? 0 : layerName.hashCode());
			result = prime * result + row;
			result = prime * result + zoom;
			return result;
		}
    }

    public static class CacheEntry {
        public CacheEntry(String pathToBundleFile, long offset, int size) {
            this.pathToBundleFile = pathToBundleFile;
            this.offset = offset;
            this.size = size;
        }

        public String pathToBundleFile;

        public long offset;

        public int size;
    }

    private Cache<CacheKey, CacheEntry> indexCache;

    /**
     * 缓存构造类
     * @param maxSize 缓存大小，如果大于这个大小，新增缓存将会删除最近不使用的
     */
    public BundlxCache(int maxSize) {
        indexCache = CacheBuilder.newBuilder().maximumSize(maxSize).build();
    }

    public synchronized void cleanUp(String layerName){
        indexCache.asMap().entrySet().removeIf(next -> next.getKey().layerName.equals(layerName));
    }

    /**
     * 获取缓存内容
     * @param key
     * @return
     */
    public synchronized CacheEntry get(CacheKey key) {
        return indexCache.getIfPresent(key);
    }

    /**
     * 新增缓存目录
     * @param key 缓存key
     * @param entry 缓存内容
     */
    public synchronized void put(CacheKey key, CacheEntry entry) {
        indexCache.put(key, entry);
    }
}
