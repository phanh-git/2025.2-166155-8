package com.importorder.boundary;

import com.importorder.entity.SaleRequest;
import com.importorder.entity.SiteOrder;
import com.importorder.entity.User;

final class UiTexts {

    private UiTexts() {
    }

    static String role(User.Role role) {
        if (role == null) return "Không xác định";
        return switch (role) {
            case ADMIN -> "Quản trị viên";
            case SALES_DEPARTMENT -> "Bộ phận bán hàng";
            case OVERSEAS_ORDER_DEPT -> "Bộ phận đặt hàng quốc tế";
            case SITE_USER -> "Quản lý site";
            case WAREHOUSE_MANAGER -> "Bộ phận quản lý kho";
        };
    }

    static String saleRequestStatus(SaleRequest.Status status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case RECEIVED -> "Đã tiếp nhận";
            case IN_PROGRESS -> "Đang xử lý";
            case SUCCESS -> "Đã hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    static String siteOrderStatus(SiteOrder.Status status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case ORDER_RECEIVED -> "Đã tạo đơn";
            case IN_TRANSIT -> "Đang vận chuyển";
            case DELIVERED -> "Đã giao tới kho";
            case SHORTAGE_REPORTED -> "Kho báo thiếu hàng";
            case WAREHOUSE_CONFIRMED -> "Kho đã xác nhận đủ hàng";
            case CANCELLED -> "Đã hủy";
        };
    }

    static String deliveryMeans(SiteOrder.DeliveryMeans means) {
        if (means == null) return "Không xác định";
        return switch (means) {
            case SHIP_DELIVERY -> "Đường biển";
            case AIR_DELIVERY -> "Đường hàng không";
        };
    }
}
