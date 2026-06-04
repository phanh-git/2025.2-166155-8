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

```powershell
javac -d out (Get-ChildItem -Recurse src\main\java\*.java).FullName
java -cp out com.importorder.app.Main
```

## Chay bang Eclipse

Import project theo dang Java Project, chon class chinh:

```text
com.importorder.app.Main
```

Du lieu hien tai luu in-memory, moi lan chay se reset ve du lieu mau.
