package com.importorder.boundary;

import com.importorder.app.AppContext;
import com.importorder.entity.User;
import javafx.stage.Stage;

public class MainDashboard {

    private final Stage stage;
    private final AppContext context;

    public MainDashboard(Stage stage, AppContext context) {
        this.stage = stage;
        this.context = context;
    }

    public void show() {
        User user = context.getCurrentUser();
        if (user == null) {
            new LoginScreen(stage, context).show();
            return;
        }

        switch (user.getRole()) {
            case SALES_DEPARTMENT -> new SalesDashboard(stage, context).show();
            case OVERSEAS_ORDER_DEPT -> new OverseasDashboard(stage, context).show();
            case SITE_USER -> new SiteDashboard(stage, context).show();
            case WAREHOUSE_MANAGER -> new WarehouseDashboard(stage, context).show();
            case ADMIN -> new AdminDashboard(stage, context).show();
        }
    }
}
