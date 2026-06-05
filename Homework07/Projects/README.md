# He thong dat hang nhap khau

Project Java demo 6 use case:

1. Tao moi Yeu cau Nhap hang
2. Xem/Tong hop danh sach Yeu cau Nhap hang
3. Xu ly Yeu cau Nhap hang va lap PO
4. Xu ly don hang huy
5. Cau hinh danh muc hang ton kho/Product catalog
6. Xac nhan nhap kho thuc te

## Cau truc package

- `entity`: lop thuc the nghiep vu
- `boundary`: giao dien Swing
- `controller`: lop dieu khien use case
- `service`: xu ly nghiep vu
- `repository`: truy xuat du lieu in-memory
- `dto`: doi tuong truyen du lieu giua boundary va controller

## Chay bang terminal

1. Mo PowerShell va chuyen vao thu muc du an:

```powershell
cd .\Homework07\Projects
```

2. Xay dung va cho phep JavaFX:

```powershell
mvn clean package
mvn javafx:run
```

Neu `mvn` khong duoc tim thay, su dung duong dan day du:

```powershell
& "$env:USERPROFILE\tools\apache-maven-3.9.16\bin\mvn.cmd" clean package
& "$env:USERPROFILE\tools\apache-maven-3.9.16\bin\mvn.cmd" javafx:run
```

## Cau hinh PostgreSQL

Database cau hinh duoc luu trong `src/main/resources/application.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/importorder_db
db.user=postgres
db.password=postgres
```

Neu database chua ton tai, tao database bang lenh sau (can chay PowerShell duoi quyen Administrator neu can):

```powershell
$env:PGPASSWORD = 'postgres'
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -h localhost -c "CREATE DATABASE importorder_db;"
Remove-Item Env:\PGPASSWORD
```

Neu Postgres yeu cau mat khau khac, thay doi `postgres` thanh mat khau da cai dat trong boi `application.properties` hoac cap nhat `pg_hba.conf` de cho phep ket noi trust tu localhost.

## Chay bang Eclipse

Import project theo dang Maven project, chon class chinh:

```text
com.importorder.app.Main
```

Du lieu se duoc luu vao PostgreSQL theo cau hinh `application.properties`.
