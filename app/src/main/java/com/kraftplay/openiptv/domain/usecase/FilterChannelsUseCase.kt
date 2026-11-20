package com.kraftplay.openiptv.domain.usecase

import com.kraftplay.openiptv.model.IptvChannel

class FilterChannelsUseCase {

    operator fun invoke(
        allChannels: List<IptvChannel>,
        activeCategory: String,
        searchQuery: String,
        recentChannels: List<IptvChannel>,
        hiddenChannels: Set<String>,
        customChannelOrder: List<String>
    ): List<IptvChannel> {
        val baseList = (if (activeCategory == "Recent") recentChannels else allChannels)
            .filterNot { hiddenChannels.contains(it.url) }

        val query = searchQuery.trim()
        val filtered = if (query.isEmpty() && activeCategory == "All") {
            baseList
        } else {
            baseList.filter { channel ->
                val matchesCategory = when (activeCategory) {
                    "All" -> true
                    "Favorites" -> channel.isFavorite
                    "Recent" -> true
                    else -> (channel.category ?: "") == activeCategory
                }
                val matchesSearch = query.isEmpty() || channel.name.contains(query, ignoreCase = true)
                matchesCategory && matchesSearch
            }
        }

        if (customChannelOrder.isEmpty()) {
            return filtered.sortedWith(
                compareByDescending<IptvChannel> { it.isPinned }
                    .thenByDescending { it.isFavorite }
            )
        }

        val orderMap = HashMap<String, Int>(customChannelOrder.size)
        customChannelOrder.forEachIndexed { i, url -> orderMap[url] = i }

        return filtered.sortedWith(
            compareByDescending<IptvChannel> { it.isPinned }
                .thenByDescending { it.isFavorite }
                .thenBy { orderMap[it.url] ?: Int.MAX_VALUE }
        )
    }
}
