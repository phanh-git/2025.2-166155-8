package com.importorder.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả trả về sau khi Bộ phận đặt hàng quốc tế xử lý một SaleRequest.
 * Chứa danh sách đơn hàng tạo được và danh sách lỗi (nếu có mặt hàng không đủ hàng).
 */
public class ProcessResult {

    /** Danh sách SiteOrder đã tạo thành công */
    private List<SiteOrderDTO> createdOrders = new ArrayList<>();

    /** Danh sách lỗi: mặt hàng nào không thể đáp ứng đủ số lượng */
    private List<String> errors = new ArrayList<>();

    /** true nếu toàn bộ mặt hàng được xử lý thành công */
    private boolean fullyProcessed;

    // ─── Constructor ────────────────────────────────────────────────────────────

    public ProcessResult() {}

    // ─── Helper ─────────────────────────────────────────────────────────────────

    public void addOrder(SiteOrderDTO order) {
        this.createdOrders.add(order);
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // ─── Getters / Setters ──────────────────────────────────────────────────────

    public List<SiteOrderDTO> getCreatedOrders() { return createdOrders; }
    public void setCreatedOrders(List<SiteOrderDTO> createdOrders) {
        this.createdOrders = createdOrders;
    }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public boolean isFullyProcessed() { return fullyProcessed; }
    public void setFullyProcessed(boolean fullyProcessed) { this.fullyProcessed = fullyProcessed; }

    @Override
    public String toString() {
        return "ProcessResult{fullyProcessed=" + fullyProcessed
                + ", orders=" + createdOrders.size()
                + ", errors=" + errors + "}";
    }
}