package com.importorder.service;

import com.importorder.dto.SiteOrderItemDTO;
import com.importorder.entity.SaleRequest;
import com.importorder.entity.SiteOrder;
import com.importorder.entity.SiteOrderItem;
import com.importorder.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfirmWarehouseReceiptTest {

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
    void confirmWarehouseReceipt_shouldThrowWhenOrderNotFound()
            throws SQLException {

        when(siteOrderRepo.findById(100))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.confirmWarehouseReceipt(
                        100,
                        LocalDate.now(),
                        List.of()
                )
        );
    }

    @Test
    void confirmWarehouseReceipt_shouldThrowWhenNoItems()
            throws SQLException {

        SiteOrder order = new SiteOrder();
        order.setId(1);
        order.setSaleRequestId(10);

        when(siteOrderRepo.findById(1))
                .thenReturn(Optional.of(order));

        when(siteOrderItemRepo.findBySiteOrderId(1))
                .thenReturn(List.of());

        assertThrows(
                IllegalStateException.class,
                () -> service.confirmWarehouseReceipt(
                        1,
                        LocalDate.now(),
                        List.of()
                )
        );
    }

    @Test
    void confirmWarehouseReceipt_shouldRejectNegativeQuantity()
            throws SQLException {

        SiteOrder order = buildOrder();

        SiteOrderItem item = buildItem(
                1,
                10,
                "P001"
        );

        SiteOrderItemDTO dto = new SiteOrderItemDTO();
        dto.setId(1);
        dto.setReceivedQuantity(-1);

        when(siteOrderRepo.findById(1))
                .thenReturn(Optional.of(order));

        when(siteOrderItemRepo.findBySiteOrderId(1))
                .thenReturn(List.of(item));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.confirmWarehouseReceipt(
                        1,
                        LocalDate.now(),
                        List.of(dto)
                )
        );
    }

    @Test
    void confirmWarehouseReceipt_shouldRejectQuantityGreaterThanOrdered()
            throws SQLException {

        SiteOrder order = buildOrder();

        SiteOrderItem item = buildItem(
                1,
                10,
                "P001"
        );

        SiteOrderItemDTO dto = new SiteOrderItemDTO();
        dto.setId(1);
        dto.setReceivedQuantity(20);

        when(siteOrderRepo.findById(1))
                .thenReturn(Optional.of(order));

        when(siteOrderItemRepo.findBySiteOrderId(1))
                .thenReturn(List.of(item));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.confirmWarehouseReceipt(
                        1,
                        LocalDate.now(),
                        List.of(dto)
                )
        );
    }

    private SiteOrder buildOrder() {

        SiteOrder order = new SiteOrder();

        order.setId(1);
        order.setSaleRequestId(10);

        return order;
    }

    private SiteOrderItem buildItem(
            int id,
            int quantity,
            String merchandiseCode
    ) {

        SiteOrderItem item = new SiteOrderItem();

        item.setId(id);
        item.setQuantity(quantity);
        item.setMerchandiseCode(merchandiseCode);
        item.setUnit("box");

        return item;
    }
}