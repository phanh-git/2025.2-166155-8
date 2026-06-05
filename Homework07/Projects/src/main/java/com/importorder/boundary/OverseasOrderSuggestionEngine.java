package com.importorder.boundary;

import com.importorder.dto.SaleRequestDTO;
import com.importorder.dto.SaleRequestItemDTO;
import com.importorder.entity.Site;
import com.importorder.entity.SiteMerchandise;
import com.importorder.entity.SiteOrder;
import com.importorder.repository.SiteMerchandiseRepository;
import com.importorder.repository.SiteRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class OverseasOrderSuggestionEngine {

    private final SiteRepository siteRepository;
    private final SiteMerchandiseRepository siteMerchandiseRepository;

    OverseasOrderSuggestionEngine(SiteRepository siteRepository,
                                  SiteMerchandiseRepository siteMerchandiseRepository) {
        this.siteRepository = siteRepository;
        this.siteMerchandiseRepository = siteMerchandiseRepository;
    }

    SuggestionResult suggest(SaleRequestDTO request) throws SQLException {
        SuggestionResult result = new SuggestionResult();
        for (SaleRequestItemDTO item : request.getItems()) {
            ItemPlan itemPlan = buildItemPlan(item);
            result.itemPlans.put(item.getId(), itemPlan);
            result.allocations.addAll(itemPlan.suggestedAllocations);
            result.errors.addAll(itemPlan.errors);
        }
        return result;
    }

    ItemPlan buildItemPlan(SaleRequestItemDTO item) throws SQLException {
        ItemPlan plan = new ItemPlan(item);
        List<SiteMerchandise> available = siteMerchandiseRepository.findAvailableSitesForMerchandise(item.getMerchandiseCode());
        if (available.isEmpty()) {
            plan.errors.add("Không có site nào đang còn hàng cho mã " + item.getMerchandiseCode());
            return plan;
        }

        Map<String, Site> sites = new HashMap<>();
        for (Site site : siteRepository.findAll()) {
            sites.put(site.getCode(), site);
        }

        for (SiteMerchandise stock : available) {
            Site site = sites.get(stock.getSiteCode());
            if (site == null) {
                continue;
            }
            CandidateOption option = bestOptionForItem(site, stock, item.getDesiredDeliveryDate());
            if (option != null) {
                plan.candidates.add(option);
            }
        }

        plan.candidates.sort(
                Comparator.comparing((CandidateOption option) -> option.deliveryMeans == SiteOrder.DeliveryMeans.SHIP_DELIVERY ? 0 : 1)
                        .thenComparing(CandidateOption::availableQuantity, Comparator.reverseOrder())
                        .thenComparing(CandidateOption::arrivalDate)
        );

        int remaining = item.getQuantityOrdered();
        for (CandidateOption option : plan.candidates) {
            if (remaining <= 0) {
                break;
            }
            int allocated = Math.min(remaining, option.availableQuantity());
            if (allocated <= 0) {
                continue;
            }
            plan.suggestedAllocations.add(new SuggestedAllocation(
                    item.getId(),
                    item.getMerchandiseCode(),
                    safeItemName(item),
                    option.siteCode(),
                    option.siteName(),
                    option.deliveryMeans(),
                    option.arrivalDate(),
                    allocated,
                    item.getUnit()
            ));
            remaining -= allocated;
        }

        if (remaining > 0) {
            plan.errors.add("Không đủ số lượng cho mã " + item.getMerchandiseCode() + ". Còn thiếu " + remaining + " " + safeUnit(item));
        }

        return plan;
    }

    private CandidateOption bestOptionForItem(Site site,
                                              SiteMerchandise stock,
                                              LocalDate desiredDate) {
        LocalDate shipDate = LocalDate.now().plusDays(site.getDeliveryDaysByShip());
        if (!shipDate.isAfter(desiredDate)) {
            return new CandidateOption(
                    site.getCode(),
                    site.getName(),
                    SiteOrder.DeliveryMeans.SHIP_DELIVERY,
                    shipDate,
                    stock.getQuantity(),
                    stock.getUnit()
            );
        }

        LocalDate airDate = LocalDate.now().plusDays(site.getDeliveryDaysByAir());
        if (!airDate.isAfter(desiredDate)) {
            return new CandidateOption(
                    site.getCode(),
                    site.getName(),
                    SiteOrder.DeliveryMeans.AIR_DELIVERY,
                    airDate,
                    stock.getQuantity(),
                    stock.getUnit()
            );
        }
        return null;
    }

    private String safeItemName(SaleRequestItemDTO item) {
        return item.getMerchandiseName() == null || item.getMerchandiseName().isBlank()
                ? item.getMerchandiseCode()
                : item.getMerchandiseName();
    }

    private String safeUnit(SaleRequestItemDTO item) {
        return item.getUnit() == null || item.getUnit().isBlank() ? "đơn vị" : item.getUnit();
    }

    record CandidateOption(
            String siteCode,
            String siteName,
            SiteOrder.DeliveryMeans deliveryMeans,
            LocalDate arrivalDate,
            Integer availableQuantity,
            String unit
    ) {
    }

    record SuggestedAllocation(
            int saleRequestItemId,
            String merchandiseCode,
            String merchandiseName,
            String siteCode,
            String siteName,
            SiteOrder.DeliveryMeans deliveryMeans,
            LocalDate arrivalDate,
            int quantity,
            String unit
    ) {
    }

    static final class ItemPlan {
        private final SaleRequestItemDTO item;
        private final List<CandidateOption> candidates = new ArrayList<>();
        private final List<SuggestedAllocation> suggestedAllocations = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        ItemPlan(SaleRequestItemDTO item) {
            this.item = item;
        }

        public SaleRequestItemDTO item() {
            return item;
        }

        public List<CandidateOption> candidates() {
            return candidates;
        }

        public List<SuggestedAllocation> suggestedAllocations() {
            return suggestedAllocations;
        }

        public List<String> errors() {
            return errors;
        }
    }

    static final class SuggestionResult {
        private final Map<Integer, ItemPlan> itemPlans = new LinkedHashMap<>();
        private final List<SuggestedAllocation> allocations = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        public Map<Integer, ItemPlan> itemPlans() {
            return itemPlans;
        }

        public List<SuggestedAllocation> allocations() {
            return allocations;
        }

        public List<String> errors() {
            return errors;
        }
    }
}
