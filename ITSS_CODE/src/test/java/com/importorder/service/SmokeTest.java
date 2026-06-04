package com.importorder.service;

import java.time.LocalDate;
import java.util.List;

import com.importorder.app.AppContext;
import com.importorder.dto.ActualReceiptItem;
import com.importorder.dto.ImportRequestDTO;
import com.importorder.dto.ProcessResult;
import com.importorder.dto.ProductInputDTO;

public class SmokeTest {
    public static void main(String[] args) {
        AppContext context = new AppContext();
        context.createRequestController.validateAndAddItem(
                new ProductInputDTO("P003", 30, "chai", LocalDate.now().plusDays(20)));
        ImportRequestDTO request = context.createRequestController.submitImportRequest();
        ProcessResult result = context.processRequestController.processRequest(request.id());
        if (result.orders().isEmpty()) {
            throw new AssertionError("Khong tao duoc don hang");
        }
        Long orderId = result.orders().get(0).id();
        context.confirmReceiptController.confirmReceipt(orderId, List.of(new ActualReceiptItem("P003", 30, "")));
        if (context.confirmReceiptController.loadWarehouseInventory().isEmpty()) {
            throw new AssertionError("Khong cap nhat ton kho noi bo");
        }
        System.out.println("Smoke test passed");
    }
}
