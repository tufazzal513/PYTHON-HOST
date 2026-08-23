package com.python.localhost.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

data class Sample(val a: String, val b: Int)

class JsonStoreTest {

    @Test
    fun roundTripsDataClasses() {
        val f = File.createTempFile("jsonstore", ".json")
        val store = JsonStore()
        store.write(f, Sample("x", 5))
        val r = store.read(f, Sample::class.java)
        assertNotNull(r)
        assertEquals("x", r!!.a)
        assertEquals(5, r.b)
    }

    @Test
    fun returnsNullForMissingFile() {
        val store = JsonStore()
        val r = store.read(File("/nonexistent/path/xyz.json"), Sample::class.java)
        assertEquals(null, r)
    }
}
