//package com.importorder.controller;

import com.importorder.dto.SaleRequestDTO;
import com.importorder.entity.User;
import com.importorder.service.SaleRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SaleRequestControllerTest {

    private SaleRequestService saleRequestService;
    private SaleRequestController controller;

    @BeforeEach
    void setUp() {
        saleRequestService = Mockito.mock(SaleRequestService.class);
        controller = new SaleRequestController(saleRequestService);
    }

    @Test
    void testGetRequestDetail_RequestIdOne_ValidAndAdminAllowed() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(1, 2);
        when(saleRequestService.getRequestById(1)).thenReturn(dto);

        User admin = new User(99, "admin", "admin", User.Role.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(admin);

        SaleRequestDTO result = controller.getRequestDetail(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(2, result.getCreatedBy());
    }

    @Test
    void testGetRequestDetail_LargeRequestIdNotFound_ThrowsIllegalArgumentException() throws SQLException {
        when(saleRequestService.getRequestById(999999)).thenThrow(new IllegalArgumentException("Không tìm thấy sale request id=999999"));

        User admin = new User(99, "admin", "admin", User.Role.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(admin);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.getRequestDetail(999999));

        assertTrue(exception.getMessage().contains("Không tìm thấy sale request id=999999"));
    }

    @Test
    void testGetRequestDetail_ZeroRequestId_ThrowsIllegalArgumentException() throws SQLException {
        when(saleRequestService.getRequestById(0)).thenThrow(new IllegalArgumentException("Không tìm thấy sale request id=0"));

        User admin = new User(99, "admin", "admin", User.Role.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(admin);

        assertThrows(IllegalArgumentException.class, () -> controller.getRequestDetail(0));
    }

    @Test
    void testGetRequestDetail_NegativeRequestId_ThrowsIllegalArgumentException() throws SQLException {
        when(saleRequestService.getRequestById(-5)).thenThrow(new IllegalArgumentException("Không tìm thấy sale request id=-5"));

        User admin = new User(99, "admin", "admin", User.Role.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(admin);

        assertThrows(IllegalArgumentException.class, () -> controller.getRequestDetail(-5));
    }

    @Test
    void testGetRequestDetail_SalesDepartmentCannotAccessOthersRequest_ThrowsIllegalStateException() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(42, 2);
        when(saleRequestService.getRequestById(42)).thenReturn(dto);

        User saleUser = new User(10, "sales", "sales", User.Role.SALES_DEPARTMENT, LocalDateTime.now());
        controller.setCurrentUser(saleUser);

        assertThrows(IllegalStateException.class, () -> controller.getRequestDetail(42));
    }

    @Test
    void testGetRequestDetail_SalesDepartmentOwnerCanAccessOwnRequest() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(33, 10);
        when(saleRequestService.getRequestById(33)).thenReturn(dto);

        User saleUser = new User(10, "sales", "sales", User.Role.SALES_DEPARTMENT, LocalDateTime.now());
        controller.setCurrentUser(saleUser);

        SaleRequestDTO result = controller.getRequestDetail(33);

        assertNotNull(result);
        assertEquals(33, result.getId());
        assertEquals(10, result.getCreatedBy());
    }

    @Test
    void testGetRequestDetail_CurrentUserNull_ThrowsNullPointerException() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(5, 5);
        when(saleRequestService.getRequestById(5)).thenReturn(dto);

        controller.setCurrentUser(null);

        assertThrows(NullPointerException.class, () -> controller.getRequestDetail(5));
    }

    private SaleRequestDTO createSaleRequestDTO(int id, int createdBy) {
        SaleRequestDTO dto = new SaleRequestDTO();
        dto.setId(id);
        dto.setCreatedBy(createdBy);
        dto.setCreatedByUsername("user" + createdBy);
        dto.setStatus(com.importorder.entity.SaleRequest.Status.RECEIVED);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }
}
