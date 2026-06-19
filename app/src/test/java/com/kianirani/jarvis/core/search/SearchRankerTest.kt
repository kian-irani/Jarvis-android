package com.kianirani.jarvis.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SRCH / DS-L4 acceptance: one scoring function ranks heterogeneous sources comparably,
 * text relevance is graded, source weight breaks ties, blank query browses, limit caps.
 * Pure, no device.
 */
class SearchRankerTest {

    @Test fun `exact match scores higher than prefix scores higher than token match`() {
        assertEquals(1.0f, SearchRanker.textScore("maps", "Maps"))
        assertEquals(0.85f, SearchRanker.textScore("map", "Maps Pro"), 0.0001f)
        assertEquals(0.70f, SearchRanker.textScore("buy milk", "milk buy today"), 0.0001f)
    }

    @Test fun `partial token overlap scales and no match is zero`() {
        // 1 of 2 query tokens present -> 0.40 * 0.5 = 0.20
        assertEquals(0.20f, SearchRanker.textScore("buy bread", "buy a car"), 0.0001f)
        assertEquals(0f, SearchRanker.textScore("zzz", "Maps"))
    }

    @Test fun `subtitle-only match gives a small score`() {
        assertEquals(0.30f, SearchRanker.textScore("einstein", "Quote", "by einstein"), 0.0001f)
    }

    @Test fun `non-matching candidates are filtered out for a non-blank query`() {
        val out = SearchRanker.rank(
            "maps",
            listOf(
                SearchCandidate("1", "Maps", SearchSource.APPS),
                SearchCandidate("2", "Calculator", SearchSource.APPS),
            ),
        )
        assertEquals(listOf("1"), out.map { it.id })
    }

    @Test fun `equal text match is broken by source weight`() {
        val out = SearchRanker.rank(
            "call",
            listOf(
                SearchCandidate("web", "call", SearchSource.WEB),
                SearchCandidate("act", "call", SearchSource.ACTIONS),
                SearchCandidate("app", "call", SearchSource.APPS),
            ),
        )
        // all exact (text=1.0) -> ordered purely by source weight: ACTIONS>APPS>WEB
        assertEquals(listOf("act", "app", "web"), out.map { it.id })
    }

    @Test fun `relevanceBoost lifts a candidate above an equal-text peer`() {
        val out = SearchRanker.rank(
            "spotify",
            listOf(
                SearchCandidate("cold", "Spotify", SearchSource.APPS),
                SearchCandidate("hot", "Spotify", SearchSource.APPS, relevanceBoost = 0.5f),
            ),
        )
        assertEquals("hot", out.first().id)
    }

    @Test fun `blank query browses all candidates ordered by weight plus boost`() {
        val out = SearchRanker.rank(
            "  ",
            listOf(
                SearchCandidate("file", "notes.txt", SearchSource.FILES),
                SearchCandidate("app", "Camera", SearchSource.APPS),
            ),
        )
        assertEquals(listOf("app", "file"), out.map { it.id })
        assertEquals(2, out.size)
    }

    @Test fun `limit caps the result count`() {
        val cands = (1..5).map { SearchCandidate("$it", "Item $it", SearchSource.APPS) }
        assertEquals(2, SearchRanker.rank("item", cands, limit = 2).size)
        assertEquals(5, SearchRanker.rank("item", cands, limit = 0).size)
    }

    @Test fun `stable order for fully-equal scores keeps input order`() {
        val out = SearchRanker.rank(
            "x",
            listOf(
                SearchCandidate("a", "x", SearchSource.APPS),
                SearchCandidate("b", "x", SearchSource.APPS),
            ),
        )
        assertEquals(listOf("a", "b"), out.map { it.id })
        assertTrue(out.first().score > 0f)
    }
}
