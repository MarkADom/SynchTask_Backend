package com.synchtask.task.application.service

import com.synchtask.board.domain.entity.Board
import com.synchtask.shared.exception.ResourceNotFoundException
import com.synchtask.shared.exception.UnauthorizedAccessException
import com.synchtask.task.domain.entity.Task
import com.synchtask.task.domain.entity.TaskAttachment
import com.synchtask.task.domain.entity.TaskPriority
import com.synchtask.task.domain.entity.TaskStatus
import com.synchtask.task.domain.repository.TaskAttachmentRepository
import com.synchtask.task.domain.repository.TaskRepository
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.entity.UserRole
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TaskAttachmentServiceTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var taskAttachmentRepository: TaskAttachmentRepository
    private lateinit var service: TaskAttachmentService

    private val owner = User(
        id = 1L,
        name = "Owner",
        email = "owner@test.com",
        passwordHash = "hash",
        role = UserRole.USER
    )

    private val board = Board(
        id = 1L,
        name = "Board",
        owner = owner
    )

    private val task = Task(
        id = 10L,
        title = "Task",
        description = "Desc",
        owner = owner,
        board = board,
        status = TaskStatus.TODO,
        priority = TaskPriority.MID
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()

        taskRepository = mockk()
        taskAttachmentRepository = mockk()
        service = TaskAttachmentService(taskRepository, taskAttachmentRepository)
    }

    @Test
    fun `should upload file when user is owner`() {
        val file = mockk<MultipartFile>()
        every { file.originalFilename } returns "file.txt"

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskAttachmentRepository.save(any()) } answers { firstArg() }

        val result = service.uploadFile(task.id!!, file, owner.email)

        assertEquals("file.txt", result.fileName)
        assertTrue(result.fileUrl.contains("cdn.synchtask.app"))

        verify(exactly = 1) { taskAttachmentRepository.save(any()) }
    }

    @Test
    fun `should throw when uploading file to non existing task`() {
        val file = mockk<MultipartFile>()
        every { taskRepository.findById(999L) } returns Optional.empty()

        assertFailsWith<ResourceNotFoundException> {
            service.uploadFile(999L, file, owner.email)
        }
    }

    @Test
    fun `should throw when uploading file by non owner`() {
        val file = mockk<MultipartFile>()
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        assertFailsWith<UnauthorizedAccessException> {
            service.uploadFile(task.id!!, file, "other@test.com")
        }

        verify(exactly = 0) { taskAttachmentRepository.save(any()) }
    }

    @Test
    fun `should list attachments when user is owner`() {
        val attachment = TaskAttachment(
            id = 1L,
            task = task,
            fileName = "doc.pdf",
            fileUrl = "url"
        )

        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskAttachmentRepository.findAllByTask(task) } returns listOf(attachment)

        val result = service.listAttachments(task.id!!, owner.email)

        assertEquals(1, result.size)
        assertEquals("doc.pdf", result.first().fileName)
    }

    @Test
    fun `should throw when listing attachments by non owner`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        assertFailsWith<UnauthorizedAccessException> {
            service.listAttachments(task.id!!, "other@test.com")
        }
    }

    @Test
    fun `should delete attachment when owner`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskAttachmentRepository.deleteByTaskAndId(task, 5L) } returns 1

        val result = service.deleteAttachment(task.id!!, 5L, owner.email)

        assertTrue(result)
        verify { taskAttachmentRepository.deleteByTaskAndId(task, 5L) }
    }

    @Test
    fun `should return false when attachment not found`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)
        every { taskAttachmentRepository.deleteByTaskAndId(task, 5L) } returns 0

        val result = service.deleteAttachment(task.id!!, 5L, owner.email)

        assertTrue(!result)
    }

    @Test
    fun `should throw when deleting attachment by non owner`() {
        every { taskRepository.findById(task.id!!) } returns Optional.of(task)

        assertFailsWith<UnauthorizedAccessException> {
            service.deleteAttachment(task.id!!, 5L, "other@test.com")
        }

        verify(exactly = 0) { taskAttachmentRepository.deleteByTaskAndId(any(), any()) }
    }
}
