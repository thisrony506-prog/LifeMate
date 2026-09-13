package com.lifemate.reset

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class FreshStartFilesTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun clearsPersonalFilesButKeepsExplicitUpdateMetadata() {
        val root = temp.newFolder("private")
        File(root, "release_updates.xml").writeText("verified-metadata-fixture")
        File(root, "profile.xml").writeText("old private profile")
        File(root, "photos").mkdirs()
        File(root, "photos/photo.encrypted").writeText("old cipher")
        FreshStartReset.clearChildren(root, setOf("release_updates.xml"))
        assertEquals(listOf("release_updates.xml"), root.list()!!.toList())
        assertEquals("verified-metadata-fixture", File(root, "release_updates.xml").readText())
    }
    @Test fun deletionNeverFollowsLinksIntoSharedGallery() {
        val root = temp.newFolder("private")
        val outside = temp.newFolder("gallery")
        val photo = File(outside, "keep.jpg").apply { writeText("user gallery") }
        Files.createSymbolicLink(File(root, "link").toPath(), outside.toPath())
        FreshStartReset.clearChildren(root)
        assertTrue(photo.isFile)
        assertTrue(root.list()!!.isEmpty())
    }
}
