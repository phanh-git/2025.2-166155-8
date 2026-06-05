package com.importorder.service;

import com.importorder.dto.ProcessResult;
import com.importorder.dto.SiteOrderDTO;
import com.importorder.entity.SaleRequest;
import com.importorder.entity.SaleRequestItem;
import com.importorder.entity.Site;
import com.importorder.entity.SiteMerchandise;
import com.importorder.entity.SiteOrder;
import com.importorder.entity.SiteOrderItem;
import com.importorder.repository.MerchandiseRepository;
import com.importorder.repository.SaleRequestItemRepository;
import com.importorder.repository.SaleRequestRepository;
import com.importorder.repository.SiteMerchandiseRepository;
import com.importorder.repository.SiteOrderItemRepository;
import com.importorder.repository.SiteOrderRepository;
import com.importorder.repository.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class OverseasOrderServiceTest {

    private SaleRequestRepository saleRequestRepo;
    private SaleRequestItemRepository saleRequestItemRepo;
    private SiteRepository siteRepo;
    private SiteMerchandiseRepository siteMerchandiseRepo;
    private SiteOrderRepository siteOrderRepo;
    private SiteOrderItemRepository siteOrderItemRepo;
    private MerchandiseRepository merchandiseRepo;
    private OverseasOrderService service;

    @BeforeEach
    void setUp() {
        saleRequestRepo = mock(SaleRequestRepository.class);
        saleRequestItemRepo = mock(SaleRequestItemRepository.class);
        siteRepo = mock(SiteRepository.class);
        siteMerchandiseRepo = mock(SiteMerchandiseRepository.class);
        siteOrderRepo = mock(SiteOrderRepository.class);
        siteOrderItemRepo = mock(SiteOrderItemRepository.class);
        merchandiseRepo = mock(MerchandiseRepository.class);
        service = new OverseasOrderService(
                saleRequestRepo,
                saleRequestItemRepo,
                siteRepo,
                siteMerchandiseRepo,
                siteOrderRepo,
                siteOrderItemRepo,
                merchandiseRepo
        );
    }

    @Test
    void processSaleRequest_shouldThrowWhenRequestNotFound() throws SQLException {
        when(saleRequestRepo.findById(99)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.processSaleRequest(99, 7));

        assertTrue(ex.getMessage().contains("Không tìm thấy"));
        verify(saleRequestRepo, never()).updateStatus(anyInt(), any());
    }

    @Test
    void processSaleRequest_shouldThrowWhenStatusIsNotReceived() throws SQLException {
        SaleRequest request = new SaleRequest();
        request.setId(10);
        request.setStatus(SaleRequest.Status.IN_PROGRESS);
        when(saleRequestRepo.findById(10)).thenReturn(Optional.of(request));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.processSaleRequest(10, 7));

        assertTrue(ex.getMessage().contains("không thể xử lý lại"));
        verify(saleRequestRepo, never()).updateStatus(eq(10), any());
    }
    @Test
    void processSaleRequest_shouldCreateOrderForShipDeliveryAndMarkInProgress() throws SQLException {
        SaleRequest request = new SaleRequest();
        request.setId(1);
        request.setStatus(SaleRequest.Status.RECEIVED);
        request.setCreatedAt(LocalDateTime.now());
        when(saleRequestRepo.findById(1)).thenReturn(Optional.of(request));

        SaleRequestItem item = new SaleRequestItem();
        item.setId(11);
        item.setSaleRequestId(1);
        item.setMerchandiseCode("P001");
        item.setQuantityOrdered(4);
        item.setUnit("chai");
        item.setDesiredDeliveryDate(LocalDate.now().plusDays(5));
        when(saleRequestItemRepo.findBySaleRequestId(1)).thenReturn(List.of(item));

        Site site = new Site();
        site.setCode("S01");
        site.setName("Site 01");
        site.setDeliveryDaysByShip(3);
        site.setDeliveryDaysByAir(10);
        when(siteRepo.findAll()).thenReturn(List.of(site));
        when(siteMerchandiseRepo.findAvailableSitesForMerchandise("P001")).thenReturn(List.of(stock("S01", "P001", 10)));
        when(merchandiseRepo.findByCode("P001")).thenReturn(Optional.of(mockMerchandise("P001", "San pham A")));
        when(siteOrderRepo.insertWithItems(any(SiteOrder.class), anyList())).thenReturn(100);

        ProcessResult result = service.processSaleRequest(1, 7);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(1, result.getCreatedOrders().size());
        SiteOrderDTO dto = result.getCreatedOrders().get(0);
        assertEquals("S01", dto.getSiteCode());
        assertEquals(SiteOrder.DeliveryMeans.SHIP_DELIVERY, dto.getDeliveryMeans());
        assertEquals(SiteOrder.Status.ORDER_RECEIVED, dto.getStatus());
        assertTrue(result.isFullyProcessed());

        verify(saleRequestRepo, times(2)).updateStatus(1, SaleRequest.Status.IN_PROGRESS);
    }

    @Test
    void processSaleRequest_shouldRecordErrorWhenNoSiteHasStock() throws SQLException {
        SaleRequest request = new SaleRequest();
        request.setId(2);
        request.setStatus(SaleRequest.Status.RECEIVED);
        when(saleRequestRepo.findById(2)).thenReturn(Optional.of(request));

        SaleRequestItem item = new SaleRequestItem();
        item.setId(12);
        item.setSaleRequestId(2);
        item.setMerchandiseCode("P002");
        item.setQuantityOrdered(4);
        item.setUnit("hop");
        item.setDesiredDeliveryDate(LocalDate.now().plusDays(5));
        when(saleRequestItemRepo.findBySaleRequestId(2)).thenReturn(List.of(item));

        when(siteRepo.findAll()).thenReturn(List.of());
        when(siteMerchandiseRepo.findAvailableSitesForMerchandise("P002")).thenReturn(List.of());
        when(merchandiseRepo.findByCode("P002")).thenReturn(Optional.of(mockMerchandise("P002", "San pham B")));

        ProcessResult result = service.processSaleRequest(2, 7);

        assertTrue(result.getCreatedOrders().isEmpty());
        assertEquals(1, result.getErrors().size());
        assertFalse(result.isFullyProcessed());
        verify(siteOrderRepo, never()).insertWithItems(any(), anyList());
    }

    private SiteMerchandise stock(String siteCode, String merchandiseCode, int quantity) {
        SiteMerchandise sm = new SiteMerchandise();
        sm.setSiteCode(siteCode);
        sm.setMerchandiseCode(merchandiseCode);
        sm.setQuantity(quantity);
        sm.setUnit("unit");
        return sm;
    }

    private com.importorder.entity.Merchandise mockMerchandise(String code, String name) {
        com.importorder.entity.Merchandise merchandise = new com.importorder.entity.Merchandise();
        merchandise.setCode(code);
        merchandise.setName(name);
        return merchandise;
    }
}
