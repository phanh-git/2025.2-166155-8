# Tài liệu kiểm thử tự động cho `SaleRequestController.getRequestDetail(int requestId)`

## 1. Mô tả module được kiểm thử
- Lớp: `com.importorder.controller.SaleRequestController`
- Phương thức: `getRequestDetail(int requestId)`
- Chức năng: lấy chi tiết một `SaleRequest` theo `requestId`.
- Hành vi chính:
  - gọi `SaleRequestService.getRequestById(requestId)` để nhận `SaleRequestDTO`.
  - nếu `currentUser` có role `SALES_DEPARTMENT` và `dto.getCreatedBy()` khác `currentUser.getId()`, ném `IllegalStateException`.
  - nếu không, trả về DTO.

## 2. Kỹ thuật kiểm thử đã áp dụng

### 2.1 Kiểm thử hộp đen (Black-box testing)
- Dựa trên đầu vào và trạng thái bên ngoài của phương thức mà không xem sâu chi tiết cài đặt.
- Tập trung vào miền giá trị của `requestId` và trạng thái của `currentUser`.

#### 2.1.1 Phân hoạch tương đương (Equivalence Partitioning)
- `requestId` hợp lệ: tồn tại trong DB.
- `requestId` không hợp lệ: không tồn tại trong DB.
- `requestId` biên: 0 và số âm.

#### 2.1.2 Phân tích giá trị biên (Boundary Value Analysis)
- Các giá trị đặc biệt của `requestId`:
  - `0`
  - `1`
  - số rất lớn: `999999`
  - số âm: `-5`

#### 2.1.3 Lớp tương đương cho trạng thái `currentUser`
- `currentUser == null`
- `currentUser` có quyền `SALES_DEPARTMENT`
- `currentUser` có quyền khác (`ADMIN` / `OVERSEAS_ORDER_DEPT`)

### 2.2 Kiểm thử hộp trắng (White-box testing) với độ đo C1
- Dựa vào cấu trúc mã nguồn của `getRequestDetail`.
- Mục tiêu: phủ nhánh điều kiện của lệnh `if` trên `currentUser.getRole()` và `dto.getCreatedBy()`.
- Cụ thể:
  - nhánh `currentUser.getRole() == User.Role.SALES_DEPARTMENT && dto.getCreatedBy() != currentUser.getId()`
  - nhánh trả về DTO khi quyền đủ
  - nhánh ngoại lệ khi `requestId` không tồn tại
  - nhánh ngoại lệ khi `currentUser == null`

## 3. Các test case thiết kế

| Test case | Loại kiểm thử | Giá trị `requestId` | `currentUser` | Kết quả mong đợi |
|---|---|---|---|---|
| `getRequestDetail(1)` hợp lệ với `ADMIN` | Hộp đen + BVA | `1` | `ADMIN` | Trả về DTO |
| `getRequestDetail(999999)` không tồn tại | Hộp đen | `999999` | `ADMIN` | `IllegalArgumentException` |
| `getRequestDetail(0)` biên | Hộp đen + BVA | `0` | `ADMIN` | `IllegalArgumentException` |
| `getRequestDetail(-5)` âm | Hộp đen + BVA | `-5` | `ADMIN` | `IllegalArgumentException` |
| `getRequestDetail(42)` với `SALES_DEPARTMENT` và yêu cầu của người khác | Hộp trắng | `42` | `SALES_DEPARTMENT` | `IllegalStateException` |
| `getRequestDetail(33)` với `SALES_DEPARTMENT` sở hữu yêu cầu | Hộp trắng | `33` | `SALES_DEPARTMENT` | Trả về DTO |
| `getRequestDetail(5)` với `currentUser == null` | Hộp đen + Hộp trắng | `5` | `null` | `NullPointerException` |

## 4. Tên đầy đủ của Class kiểm thử tự động
- `com.importorder.controller.SaleRequestControllerTest`

## 5. Thư mục chứa kiểm thử tự động
- `Homework07/Projects/20235858`

## 6. Cách chạy kiểm thử
- Chạy lệnh Maven từ `Homework07/Projects`:
  ```bash
  mvn test
  ```

## 7. Ghi chú
- `SaleRequestController` hiện đã hỗ trợ injection `SaleRequestService` để có thể mock service khi kiểm thử.
- Các test case hiện tại không phụ thuộc trực tiếp vào cơ sở dữ liệu thực tế.
