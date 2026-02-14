package com.synchtask.task.presentation.controller

import com.synchtask.task.application.dto.TaskAttachmentDTO
import com.synchtask.task.application.dto.TaskLinkDTO
import com.synchtask.task.application.service.TaskAttachmentService
import com.synchtask.task.application.service.TaskLinkService
import com.synchtask.user.application.service.UserService
import com.synchtask.user.domain.entity.User
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.springframework.security.core.userdetails.User as SpringUser

class TaskResourceControllerTest {
    private lateinit var linkService: TaskLinkService
    private lateinit var attachmentService: TaskAttachmentService
    private lateinit var userService: UserService
    private lateinit var controller: TaskResourceController

    private lateinit var userDetails: UserDetails
    private lateinit var actor: User

    @BeforeEach
    fun setup() {
        linkService = mockk(relaxed = true)
        attachmentService = mockk(relaxed = true)
        userService = mockk(relaxed = true)
        controller = TaskResourceController(linkService, attachmentService, userService)

        userDetails =
            SpringUser(
                "user@synchtask.com",
                "hash",
                emptyList()
            )
        actor = User(id = 1L, name = "User", email = "user@synchtask.com", passwordHash = "hash")

        every { userService.getUserByEmail(userDetails.username) } returns actor
    }

    @Test
    fun `should add link successfully`() {
        val dto =
            TaskLinkDTO(
                id = 1L,
                title = "GitHub",
                url = "https://github.com",
                createdAt = LocalDateTime.now()
            )

        every {
            linkService.addLink(10L, "GitHub", "https://github.com", actor)
        } returns dto

        val response =
            controller.addLink(
                taskId = 10L,
                title = "GitHub",
                url = "https://github.com",
                user = userDetails
            )

        assertEquals(dto.id, response.body!!.id)
        assertEquals(dto.title, response.body!!.title)
        assertEquals(dto.url, response.body!!.url)

        verify(exactly = 1) {
            linkService.addLink(10L, "GitHub", "https://github.com", actor)
        }
    }

    @Test
    fun `should list links`() {
        val links =
            listOf(
                TaskLinkDTO(1L, "Doc", "https://docs", LocalDateTime.now()),
                TaskLinkDTO(2L, "Repo", "https://repo", LocalDateTime.now())
            )

        every { linkService.listLinks(10L, actor) } returns links

        val response = controller.listLinks(10L, userDetails)

        assertEquals(2, response.body!!.size)
        assertEquals("Doc", response.body!![0].title)

        verify(exactly = 1) { linkService.listLinks(10L, actor) }
    }

    @Test
    fun `should delete link successfully`() {
        every {
            linkService.removeLink(10L, 5L, actor)
        } just Runs

        val response =
            controller.deleteLink(
                taskId = 10L,
                linkId = 5L,
                user = userDetails
            )

        assertEquals(204, response.statusCode.value())

        verify(exactly = 1) {
            linkService.removeLink(10L, 5L, actor)
        }
    }

    @Test
    fun `should upload attachment`() {
        val file = mockk<MultipartFile>()

        val dto =
            TaskAttachmentDTO(
                id = 1L,
                fileName = "file.pdf",
                fileUrl = "https://cdn.synchtask.com/file.pdf",
                uploadedAt = LocalDateTime.now()
            )

        every {
            attachmentService.uploadFile(10L, file, actor)
        } returns dto

        val response =
            controller.uploadFile(
                taskId = 10L,
                file = file,
                user = userDetails
            )

        assertNotNull(response.body)
        assertEquals("file.pdf", response.body!!.fileName)
        assertEquals("https://cdn.synchtask.com/file.pdf", response.body!!.fileUrl)

        verify(exactly = 1) {
            attachmentService.uploadFile(10L, file, actor)
        }
    }

    @Test
    fun `should list attachments`() {
        val attachments =
            listOf(
                TaskAttachmentDTO(
                    1L,
                    "a.txt",
                    "https://cdn.synchtask.com/a.txt",
                    LocalDateTime.now()
                ),
                TaskAttachmentDTO(
                    2L,
                    "b.txt",
                    "https://cdn.synchtask.com/b.txt",
                    LocalDateTime.now()
                )
            )

        every {
            attachmentService.listAttachments(10L, actor)
        } returns attachments

        val response = controller.listAttachments(10L, userDetails)

        assertEquals(2, response.body!!.size)
        assertEquals("a.txt", response.body!![0].fileName)

        verify(exactly = 1) {
            attachmentService.listAttachments(10L, actor)
        }
    }

    @Test
    fun `should delete attachment successfully`() {
        every {
            attachmentService.deleteAttachment(10L, 3L, actor)
        } just Runs

        val response =
            controller.deleteAttachment(
                taskId = 10L,
                attachmentId = 3L,
                user = userDetails
            )

        assertEquals(204, response.statusCode.value())

        verify(exactly = 1) {
            attachmentService.deleteAttachment(10L, 3L, actor)
        }
    }
}
