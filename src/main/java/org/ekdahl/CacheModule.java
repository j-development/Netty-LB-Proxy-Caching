package org.ekdahl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

public class CacheModule {
	//Cache from Guava
	@Getter
	private static final Cache<String, Object> cache = CacheBuilder.newBuilder()
			.expireAfterWrite(1, TimeUnit.HOURS) // Expire after 1 hour
				.maximumSize(1000) // Maximum size of 1000 entries
				.build();

}
