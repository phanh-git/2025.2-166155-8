package com.importorder.boundary;

import com.importorder.dto.SaleRequestDTO;
import com.importorder.dto.SaleRequestItemDTO;
import com.importorder.entity.Site;
import com.importorder.entity.SiteMerchandise;
import com.importorder.entity.SiteOrder;
import com.importorder.repository.SiteMerchandiseRepository;
import com.importorder.repository.SiteRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class OverseasOrderSuggestionEngineTest {

    @Test
    void suggest_shouldReturnAllocationWhenShipCanMeetDeadline() throws SQLException {
        SiteRepository siteRepository = Mockito.mock(SiteRepository.class);
        SiteMerchandiseRepository siteMerchandiseRepository = Mockito.mock(SiteMerchandiseRepository.class);
        OverseasOrderSuggestionEngine engine = new OverseasOrderSuggestionEngine(siteRepository, siteMerchandiseRepository);

        Site site = new Site();
        site.setCode("S01");
        site.setName("Site 01");
        site.setDeliveryDaysByShip(3);
        site.setDeliveryDaysByAir(1);

        SiteMerchandise stock = new SiteMerchandise();
        stock.setSiteCode("S01");
        stock.setMerchandiseCode("P001");
        stock.setQuantity(10);
        stock.setUnit("chai");

        SaleRequestItemDTO item = new SaleRequestItemDTO();
        item.setId(11);
        item.setMerchandiseCode("P001");
        item.setMerchandiseName("San pham A");
        item.setQuantityOrdered(4);
        item.setUnit("chai");
        item.setDesiredDeliveryDate(LocalDate.now().plusDays(5));
        
        SaleRequestDTO request = new SaleRequestDTO();
        request.setItems(List.of(item));

        when(siteMerchandiseRepository.findAvailableSitesForMerchandise("P001")).thenReturn(List.of(stock));
        when(siteRepository.findAll()).thenReturn(List.of(site));

        OverseasOrderSuggestionEngine.SuggestionResult result = engine.suggest(request);

        assertEquals(1, result.allocations().size());
        assertTrue(result.errors().isEmpty());
        assertEquals(SiteOrder.DeliveryMeans.SHIP_DELIVERY, result.allocations().get(0).deliveryMeans());
        assertEquals(4, result.allocations().get(0).quantity());
    }

    @Test
    void buildItemPlan_shouldPreferAirWhenShipMissesDeadlineButAirMeetsDeadline() throws SQLException {
        SiteRepository siteRepository = Mockito.mock(SiteRepository.class);
        SiteMerchandiseRepository siteMerchandiseRepository = Mockito.mock(SiteMerchandiseRepository.class);
        OverseasOrderSuggestionEngine engine = new OverseasOrderSuggestionEngine(siteRepository, siteMerchandiseRepository);

        Site site = new Site();
        site.setCode("S02");
        site.setName("Site 02");
        site.setDeliveryDaysByShip(10);
        site.setDeliveryDaysByAir(2);

        SiteMerchandise stock = new SiteMerchandise();
        stock.setSiteCode("S02");
        stock.setMerchandiseCode("P002");
        stock.setQuantity(7);
        stock.setUnit("hop");

        SaleRequestItemDTO item = new SaleRequestItemDTO();
        item.setId(22);
        item.setMerchandiseCode("P002");
        item.setMerchandiseName("San pham B");
        item.setQuantityOrdered(5);
        item.setUnit("hop");
        item.setDesiredDeliveryDate(LocalDate.now().plusDays(3));

        when(siteMerchandiseRepository.findAvailableSitesForMerchandise("P002")).thenReturn(List.of(stock));
        when(siteRepository.findAll()).thenReturn(List.of(site));

        OverseasOrderSuggestionEngine.ItemPlan plan = engine.buildItemPlan(item);

        assertEquals(1, plan.candidates().size());
        assertEquals(SiteOrder.DeliveryMeans.AIR_DELIVERY, plan.candidates().get(0).deliveryMeans());
        assertTrue(plan.errors().isEmpty());
    }

    @Test
    void buildItemPlan_shouldReturnErrorWhenNoSiteHasStock() throws SQLException {
        SiteRepository siteRepository = Mockito.mock(SiteRepository.class);
        SiteMerchandiseRepository siteMerchandiseRepository = Mockito.mock(SiteMerchandiseRepository.class);
        OverseasOrderSuggestionEngine engine = new OverseasOrderSuggestionEngine(siteRepository, siteMerchandiseRepository);

        SaleRequestItemDTO item = new SaleRequestItemDTO();
        item.setId(33);
        item.setMerchandiseCode("P003");
        item.setQuantityOrdered(2);
        item.setUnit("cai");
        item.setDesiredDeliveryDate(LocalDate.now().plusDays(2));

        when(siteMerchandiseRepository.findAvailableSitesForMerchandise("P003")).thenReturn(List.of());

        OverseasOrderSuggestionEngine.ItemPlan plan = engine.buildItemPlan(item);

        assertTrue(plan.candidates().isEmpty());
        assertEquals(1, plan.errors().size());
        assertTrue(plan.errors().get(0).contains("P003"));
    }
}
