package com.synchtask.board.application.service

import com.synchtask.activity.application.service.ActivityService
import com.synchtask.board.application.dto.BoardCollaboratorUpdateDTO
import com.synchtask.board.application.dto.BoardCreateDTO
import com.synchtask.board.domain.entity.Board
import com.synchtask.board.domain.entity.BoardMember
import com.synchtask.board.domain.repository.BoardMemberRepository
import com.synchtask.board.domain.repository.BoardRepository
import com.synchtask.shared.domain.membership.MembershipRole
import com.synchtask.user.domain.entity.User
import com.synchtask.user.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals

class BoardServiceTest {
    private lateinit var boardRepository: BoardRepository
    private lateinit var boardMemberRepository: BoardMemberRepository
    private lateinit var userRepository: UserRepository
    private lateinit var activityService: ActivityService
    private lateinit var service: BoardService

    private val owner = User(
        id = 1L,
        name = "Owner",
        email = "owner@test.com",
        passwordHash = "hash"
    )
    private val collaborator = User(
        id = 2L,
        name = "Collab",
        email = "collab@test.com",
        passwordHash = "hash"
    )

    @BeforeEach
    fun setup() {
        boardRepository = mockk(relaxed = true)
        boardMemberRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        activityService = mockk(relaxed = true)

        service = BoardService(boardRepository, boardMemberRepository, userRepository, activityService)
    }

    @Test
    fun `createBoard creates owner membership`() {
        val dto = BoardCreateDTO(
            name = "Board",
            color = "#000",
            description = "desc"
        )
        val savedBoard = Board(
            id = 10L,
            name = "Board",
            color = "#000",
            description = "desc",
            owner = owner
        )

        every { boardRepository.save(any()) } returns savedBoard

        val membershipSlot = slot<BoardMember>()
        every { boardMemberRepository.save(capture(membershipSlot)) } returns mockk(relaxed = true)

        service.createBoard(dto, owner)

        verify(exactly = 1) { boardMemberRepository.save(any()) }
        assertEquals(savedBoard.id, membershipSlot.captured.board.id)
        assertEquals(owner.id, membershipSlot.captured.user.id)
        assertEquals(MembershipRole.OWNER, membershipSlot.captured.role)
    }

    @Test
    fun `updateCollaborators replaces only membership records`() {
        val board = Board(
            id = 11L,
            name = "Board",
            owner = owner
        )
        val dto = BoardCollaboratorUpdateDTO(userIds = listOf(collaborator.id!!))

        val ownerMember = BoardMember(
            board = board,
            user = owner,
            role = MembershipRole.OWNER
        )
        val oldCollaboratorMember = BoardMember(
            board = board,
            user = collaborator,
            role = MembershipRole.COLLABORATOR
        )

        every { boardRepository.findById(board.id!!) } returns Optional.of(board)
        every { boardMemberRepository.findByBoardIdAndUserId(board.id!!, owner.id!!) } returns ownerMember
        every { userRepository.findAllById(dto.userIds) } returns listOf(collaborator)
        every { boardMemberRepository.findAllByBoardId(board.id!!) } returns listOf(ownerMember, oldCollaboratorMember)
        every { boardRepository.save(board) } returns board

        service.updateCollaborators(board.id!!, dto, owner)

        verify(exactly = 1) {
            boardMemberRepository.deleteAll(
                match<List<BoardMember>> { members ->
                    members.size == 1 && members.first().role == MembershipRole.COLLABORATOR
                }
            )
        }
        verify(exactly = 1) {
            boardMemberRepository.saveAll(
                match<List<BoardMember>> { members ->
                    members.size == 1 && members.first().role == MembershipRole.COLLABORATOR
                }
            )
        }
    }

    @Test
    fun `deleteBoard records snapshot for notifications`() {
        val board = Board(
            id = 13L,
            name = "Board",
            owner = owner
        )
        board.members.add(
            BoardMember(
                board = board,
                user = collaborator,
                role = MembershipRole.COLLABORATOR
            )
        )

        every { boardRepository.findById(board.id!!) } returns Optional.of(board)
        every { boardMemberRepository.findByBoardIdAndUserId(board.id!!, owner.id!!) } returns
            BoardMember(
                board = board,
                user = owner,
                role = MembershipRole.OWNER
            )

        service.deleteBoard(board.id!!, owner)

        verify(exactly = 1) { boardRepository.delete(board) }
        verify(exactly = 1) {
            activityService.record(
                actor = owner,
                type = com.synchtask.activity.domain.model.ActivityType.BOARD_DELETED,
                referenceId = board.id,
                description = any(),
                contextSnapshot = withArg { snapshot ->
                    assertEquals(owner.email, snapshot?.ownerEmail)
                    assertEquals(setOf(collaborator.email), snapshot?.collaboratorEmails)
                }
            )
        }
    }

    @Test
    fun `getSimpleBoardsForUser returns empty when actor has null id`() {
        val anonymous = User(
            id = null,
            name = "Anon",
            email = "anon@test.com",
            passwordHash = "hash"
        )

        val boards = service.getSimpleBoardsForUser(anonymous)

        assertEquals(emptyList(), boards)
    }


    @Test
    fun `getBoardAccessibleByUser checks access through BoardMemberRepository`() {
        val board = Board(
            id = 12L,
            name = "Board",
            owner = owner
        )
        every { boardRepository.findById(board.id!!) } returns Optional.of(board)
        every { boardMemberRepository.existsByBoardIdAndUserId(board.id!!, owner.id!!) } returns true

        service.getBoardAccessibleByUser(board.id!!, owner)

        verify(exactly = 1) { boardMemberRepository.existsByBoardIdAndUserId(board.id!!, owner.id!!) }
    }
}
