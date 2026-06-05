//package com.importorder.service;

import com.importorder.controller.SaleRequestController;
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

    // Black-box test cases

    @Test
    void tcBlack01_currentUserNull_shouldThrowNullPointerException() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(1, 100);
        when(saleRequestService.getRequestById(1)).thenReturn(dto);

        controller.setCurrentUser(null);

        assertThrows(NullPointerException.class, () -> controller.getRequestDetail(1));
    }

    @Test
    void tcBlack02_overseasOrderDeptCanViewAnyRequest() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(1, 100);
        when(saleRequestService.getRequestById(1)).thenReturn(dto);

        User user = new User(200, "overseas", "overseas", User.Role.OVERSEAS_ORDER_DEPT, LocalDateTime.now());
        controller.setCurrentUser(user);

        SaleRequestDTO result = controller.getRequestDetail(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void tcBlack03_adminCanViewAnyRequest() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(1, 100);
        when(saleRequestService.getRequestById(1)).thenReturn(dto);

        User user = new User(300, "admin", "admin", User.Role.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(user);

        SaleRequestDTO result = controller.getRequestDetail(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void tcBlack04_salesDepartmentOwnerCanViewOwnRequest() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(1, 100);
        when(saleRequestService.getRequestById(1)).thenReturn(dto);

        User user = new User(100, "sales", "sales", User.Role.SALES_DEPARTMENT, LocalDateTime.now());
        controller.setCurrentUser(user);

        SaleRequestDTO result = controller.getRequestDetail(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(100, result.getCreatedBy());
    }

    @Test
    void tcBlack05_salesDepartmentCannotViewOthersRequest() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(1, 100);
        when(saleRequestService.getRequestById(1)).thenReturn(dto);

        User user = new User(200, "sales2", "sales2", User.Role.SALES_DEPARTMENT, LocalDateTime.now());
        controller.setCurrentUser(user);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> controller.getRequestDetail(1));
        assertTrue(exception.getMessage().toLowerCase().contains("không có quyền"));
    }

    @Test
    void tcBlack06_requestNotFound_shouldThrowIllegalArgumentException() throws SQLException {
        when(saleRequestService.getRequestById(9999)).thenThrow(new IllegalArgumentException("Không tìm thấy sale request id=9999"));

        User user = new User(300, "admin", "admin", User.Role.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(user);

        assertThrows(IllegalArgumentException.class, () -> controller.getRequestDetail(9999));
    }

    @Test
    void tcBlack07_requestIdZero_shouldThrowIllegalArgumentException() throws SQLException {
        when(saleRequestService.getRequestById(0)).thenThrow(new IllegalArgumentException("Không tìm thấy sale request id=0"));

        User user = new User(300, "admin", "admin", User.Role.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(user);

        assertThrows(IllegalArgumentException.class, () -> controller.getRequestDetail(0));
    }

    @Test
    void tcBlack08_requestIdNegative_shouldThrowIllegalArgumentException() throws SQLException {
        when(saleRequestService.getRequestById(-1)).thenThrow(new IllegalArgumentException("Không tìm thấy sale request id=-1"));

        User user = new User(300, "admin", "admin", User.Role.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(user);

        assertThrows(IllegalArgumentException.class, () -> controller.getRequestDetail(-1));
    }

    @Test
    void tcBlack09_cancelRequestedRequestStillReturnsDtoForOwner() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(1, 100);
        dto.setCancelRequested(true);
        when(saleRequestService.getRequestById(1)).thenReturn(dto);

        User user = new User(100, "sales", "sales", User.Role.SALES_DEPARTMENT, LocalDateTime.now());
        controller.setCurrentUser(user);

        SaleRequestDTO result = controller.getRequestDetail(1);

        assertNotNull(result);
        assertTrue(result.isCancelRequested());
    }

    // White-box test cases

    @Test
    void tcWhite01_currentUserNull_branchB1() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(1, 100);
        when(saleRequestService.getRequestById(1)).thenReturn(dto);

        controller.setCurrentUser(null);

        assertThrows(NullPointerException.class, () -> controller.getRequestDetail(1));
    }

    @Test
    void tcWhite02_adminBranchB2B4B6() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(999, 200);
        when(saleRequestService.getRequestById(999)).thenReturn(dto);

        User user = new User(300, "admin", "admin", User.Role.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(user);

        SaleRequestDTO result = controller.getRequestDetail(999);

        assertNotNull(result);
        assertEquals(999, result.getId());
    }

    @Test
    void tcWhite03_overseasBranchB2B4B6() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(999, 200);
        when(saleRequestService.getRequestById(999)).thenReturn(dto);

        User user = new User(400, "overseas", "overseas", User.Role.OVERSEAS_ORDER_DEPT, LocalDateTime.now());
        controller.setCurrentUser(user);

        SaleRequestDTO result = controller.getRequestDetail(999);

        assertNotNull(result);
        assertEquals(999, result.getId());
    }

    @Test
    void tcWhite05_salesDepartmentOwnerBranchB3B4B6() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(1, 100);
        when(saleRequestService.getRequestById(1)).thenReturn(dto);

        User user = new User(100, "sales", "sales", User.Role.SALES_DEPARTMENT, LocalDateTime.now());
        controller.setCurrentUser(user);

        SaleRequestDTO result = controller.getRequestDetail(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void tcWhite06_salesDepartmentOtherBranchB3B4B5() throws SQLException {
        SaleRequestDTO dto = createSaleRequestDTO(1, 100);
        when(saleRequestService.getRequestById(1)).thenReturn(dto);

        User user = new User(200, "sales2", "sales2", User.Role.SALES_DEPARTMENT, LocalDateTime.now());
        controller.setCurrentUser(user);

        assertThrows(IllegalStateException.class, () -> controller.getRequestDetail(1));
    }

    @Test
    void tcWhite07_adminServiceThrowsIllegalArgumentExceptionBranchB2B4() throws SQLException {
        when(saleRequestService.getRequestById(9999)).thenThrow(new IllegalArgumentException("Không tìm thấy sale request id=9999"));

        User user = new User(300, "admin", "admin", User.Role.ADMIN, LocalDateTime.now());
        controller.setCurrentUser(user);

        assertThrows(IllegalArgumentException.class, () -> controller.getRequestDetail(9999));
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
