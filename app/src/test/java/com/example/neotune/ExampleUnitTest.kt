package com.example.neotune

import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testYouTubeMusicSearch() = runBlocking {
        val result = YouTubeMusicApi.search("Imagine Dragons")
        println(result)
        // Optionally, add assertions here
    }
}