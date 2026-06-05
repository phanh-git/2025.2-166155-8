package com.importorder.app;

import com.importorder.controller.OverseasOrderController;
import com.importorder.controller.SaleRequestController;
import com.importorder.entity.SiteUser;
import com.importorder.entity.User;
import com.importorder.repository.SiteUserRepository;
import com.importorder.repository.UserRepository;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Ứng dụng hiện chỉ cần 2 luồng chính:
 * - SALES_DEPARTMENT gửi yêu cầu đặt hàng
 * - OVERSEAS_ORDER_DEPT nhận và xử lý yêu cầu
 */
public class AppContext {

    private final SaleRequestController saleRequestController = new SaleRequestController();
    private final OverseasOrderController overseasOrderController = new OverseasOrderController();
    private final UserRepository userRepository = new UserRepository();
    private final SiteUserRepository siteUserRepository = new SiteUserRepository();

    private User currentUser;

    public SaleRequestController getSaleRequestController() {
        return saleRequestController;
    }

    public OverseasOrderController getOverseasOrderController() {
        return overseasOrderController;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Optional<User> authenticate(String username, String password) throws SQLException {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return Optional.empty();

        User user = userOpt.get();
        if (!user.getPassword().equals(password)) return Optional.empty();
        return Optional.of(user);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        saleRequestController.setCurrentUser(user);
        overseasOrderController.setCurrentUser(user);
        overseasOrderController.setCurrentSiteCode(null);
        if (user != null && user.getRole() == User.Role.SITE_USER) {
            try {
                SiteUser siteUser = siteUserRepository.findByUserId(user.getId()).orElse(null);
                overseasOrderController.setCurrentSiteCode(siteUser != null ? siteUser.getSiteCode() : null);
            } catch (SQLException ex) {
                throw new IllegalStateException("Khong the tai thong tin site cua nguoi dung", ex);
            }
        }
    }

    public void logout() {
        currentUser = null;
        saleRequestController.setCurrentUser(null);
        overseasOrderController.setCurrentUser(null);
        overseasOrderController.setCurrentSiteCode(null);
    }
}
