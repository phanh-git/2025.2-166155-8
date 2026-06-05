package com.importorder.boundary;

import com.importorder.app.AppContext;
import com.importorder.entity.SiteUser;
import com.importorder.entity.User;
import com.importorder.repository.SiteRepository;
import com.importorder.repository.SiteUserRepository;
import com.importorder.repository.UserRepository;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AdminDashboard extends DashboardBase {

    private final UserRepository userRepository = new UserRepository();
    private final SiteRepository siteRepository = new SiteRepository();
    private final SiteUserRepository siteUserRepository = new SiteUserRepository();

    public AdminDashboard(Stage stage, AppContext context) {
        super(stage, context);
    }

    @Override
    protected VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(250);
        VBox brand = brandPane("Hệ thống đặt hàng nhập khẩu", "Quản trị hệ thống");
        VBox nav = new VBox(6);
        nav.setPadding(new Insets(18, 14, 18, 14));
        nav.getChildren().add(navButton("Quản trị người dùng", this::buildUserManagementPage));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(brand, new Separator(), nav, spacer, sidebarUserBox());
        return sidebar;
    }

    @Override
    protected void setInitialPage() {
        setPage(buildUserManagementPage());
    }

    private Node buildUserManagementPage() {
        VBox page = pageShell("Quản trị người dùng", "Tạo và cập nhật tài khoản cho từng vai trò trong hệ thống.");

        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<User, String> idCol = new TableColumn<>("Mã người dùng");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getId())));
        TableColumn<User, String> usernameCol = new TableColumn<>("Tên đăng nhập");
        usernameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getUsername()));
        TableColumn<User, String> roleCol = new TableColumn<>("Vai trò");
        roleCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(UiTexts.role(data.getValue().getRole())));
        table.getColumns().addAll(idCol, usernameCol, roleCol);

        VBox detailBox = new VBox(10);
        detailBox.getStyleClass().add("card");
        detailBox.setPadding(new Insets(18));

        Runnable loadUsers = () -> {
            try {
                List<User> users = userRepository.findAll();
                table.setItems(FXCollections.observableArrayList(users));
                if (!users.isEmpty()) {
                    table.getSelectionModel().selectFirst();
                    renderUserDetail(detailBox, users.get(0));
                } else {
                    detailBox.getChildren().setAll(paragraph("Chưa có tài khoản nào."));
                }
            } catch (SQLException ex) {
                detailBox.getChildren().setAll(paragraph("Không tải được danh sách người dùng: " + ex.getMessage()));
            }
        };

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                renderUserDetail(detailBox, selected);
            }
        });

        Button createButton = new Button("Tạo tài khoản mới");
        createButton.getStyleClass().add("btn-primary");
        createButton.setOnAction(e -> {
            UserFormDialog dialog = new UserFormDialog(stage, siteRepository);
            dialog.showDialog(null, null).ifPresent(result -> {
                try {
                    User user = result.getUser();
                    int id = userRepository.insert(user);
                    user.setId(id);
                    if (result.getSiteUser() != null) {
                        result.getSiteUser().setUserId(id);
                        siteUserRepository.upsert(result.getSiteUser());
                    }
                    loadUsers.run();
                } catch (Exception ex) {
                    showError("Không thể tạo tài khoản", ex.getMessage());
                }
            });
        });

        Button editButton = new Button("Cập nhật tài khoản đang chọn");
        editButton.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            try {
                SiteUser existingSiteUser = siteUserRepository.findByUserId(selected.getId()).orElse(null);
                UserFormDialog dialog = new UserFormDialog(stage, siteRepository);
                dialog.showDialog(selected, existingSiteUser).ifPresent(result -> {
                    try {
                        userRepository.update(result.getUser());
                        if (result.getSiteUser() != null) {
                            result.getSiteUser().setUserId(result.getUser().getId());
                            siteUserRepository.upsert(result.getSiteUser());
                        } else {
                            siteUserRepository.deleteByUserId(result.getUser().getId());
                        }
                        loadUsers.run();
                    } catch (Exception ex) {
                        showError("Không thể cập nhật tài khoản", ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                showError("Không thể tải thông tin tài khoản", ex.getMessage());
            }
        });

        page.getChildren().addAll(createButton, editButton, table, sectionTitle("Chi tiết tài khoản"), detailBox);
        loadUsers.run();
        return scroll(page);
    }

    private void renderUserDetail(VBox detailBox, User user) {
        detailBox.getChildren().clear();
        detailBox.getChildren().addAll(
                paragraph("Mã người dùng: " + user.getId()),
                paragraph("Tên đăng nhập: " + user.getUsername()),
                paragraph("Vai trò: " + UiTexts.role(user.getRole()))
        );
        try {
            SiteUser siteUser = siteUserRepository.findByUserId(user.getId()).orElse(null);
            if (siteUser != null) {
                detailBox.getChildren().add(paragraph("Site được gán: " + siteUser.getSiteCode()));
            }
        } catch (SQLException ex) {
            detailBox.getChildren().add(paragraph("Không tải được thông tin site của tài khoản."));
        }
    }
}
