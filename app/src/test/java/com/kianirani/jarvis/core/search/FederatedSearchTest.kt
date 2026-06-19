package com.kianirani.jarvis.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MX cross-device search acceptance: results from several nodes merge into one ranked list,
 * duplicate ids collapse to their best score, ordering is by score, and limit caps. Pure.
 */
class FederatedSearchTest {

    private fun r(id: String, score: Float, source: SearchSource = SearchSource.APPS) =
        SearchResult(id, id, source, "", score)

    @Test fun `results from multiple nodes merge and rank by score`() {
        val out = FederatedSearch.merge(
            listOf(
                NodeResults("phone", listOf(r("a", 0.5f), r("b", 0.9f))),
                NodeResults("desktop", listOf(r("c", 0.7f))),
            ),
        )
        assertEquals(listOf("b", "c", "a"), out.map { it.id })
    }

    @Test fun `a duplicate id collapses to its highest score`() {
        val out = FederatedSearch.merge(
            listOf(
                NodeResults("phone", listOf(r("doc", 0.4f))),
                NodeResults("desktop", listOf(r("doc", 0.95f))), // same id, stronger
            ),
        )
        assertEquals(1, out.size)
        assertEquals("doc", out.first().id)
        assertEquals(0.95f, out.first().score, 1e-6f)
    }

    @Test fun `ties break by id for stability`() {
        val out = FederatedSearch.merge(
            listOf(NodeResults("n", listOf(r("zeta", 0.8f), r("alpha", 0.8f)))),
        )
        assertEquals(listOf("alpha", "zeta"), out.map { it.id })
    }

    @Test fun `limit caps the unified list`() {
        val out = FederatedSearch.merge(
            listOf(NodeResults("n", listOf(r("a", 0.9f), r("b", 0.8f), r("c", 0.7f)))),
            limit = 2,
        )
        assertEquals(listOf("a", "b"), out.map { it.id })
    }

    @Test fun `empty input yields empty`() {
        assertTrue(FederatedSearch.merge(emptyList()).isEmpty())
    }
}
