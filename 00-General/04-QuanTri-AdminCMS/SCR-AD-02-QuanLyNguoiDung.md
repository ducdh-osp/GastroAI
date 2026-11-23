# SCR-AD-02 — Quản lý người dùng (bệnh nhân)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0046` |
| Actor | Chỉ Admin; các vai trò CMS khác và Bệnh nhân không có quyền quản lý tài khoản bệnh nhân |
| Mục tiêu nghiệp vụ | Cho Admin xử lý các tình huống cần can thiệp vào tài khoản bệnh nhân (vi phạm, yêu cầu hỗ trợ, nghi ngờ gian lận) |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao chỉ Admin

Khoá/mở tài khoản là quyết định **quản trị hệ thống**, không phải quyết định chuyên môn y tế — người biên tập nội dung y khoa giỏi không nhất thiết nên có quyền khoá tài khoản người dùng. Tách rõ ranh giới này tránh tình trạng cấp quyền "tiện thể" rồi dần dần vai trò nội dung có toàn quyền hệ thống ngoài ý muốn.

## 2. Thiết kế màn hình

Trang `/cms/users` có tìm theo tên/email, lọc trạng thái và phân trang. Bảng hiện tên, email, điện thoại, trạng thái và ngày tạo; không hiển thị hồ sơ y tế hay nhật ký sức khoẻ.

```text
┌───────────────────────────────────────────────────────────┐
│  Quản lý người dùng              [ 🔍 Tìm theo tên/email ]    │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Tên            │ Email            │ Trạng thái │ Thao tác │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ Nguyễn Văn A    │ a@gmail.com      │ ● Hoạt động│ [Khoá]   │ │
│ │ Trần Thị B      │ b@gmail.com      │ ⊘ Đã khoá  │ [Mở khoá]│ │
│ └─────────────────────────────────────────────────────────┘ │
│                                              ‹ 1 2 3 ... 20 › │
└───────────────────────────────────────────────────────────┘

— Khi bấm "Khoá" —
┌─────────────────────────────────┐
│  Khoá tài khoản này?                 │
│  Tên/email bệnh nhân được chọn      │
│  Không thể đăng nhập; thu hồi các   │
│  phiên đăng nhập hiện có.           │
│  Lý do (bắt buộc, tối đa 1000 ký tự):│
│  [___________________________]       │
│       [Huỷ]  [  Xác nhận khoá  ]     │
└─────────────────────────────────┘
```

Admin nhập lý do rồi xác nhận. Nếu API báo lỗi, modal giữ lý do để sửa hoặc thử lại; khi đang gửi, khoá nút gửi lặp và thao tác đóng modal. Bấm Huỷ trước khi gửi không thay đổi tài khoản. Mở khoá vẫn dùng hộp xác nhận, không yêu cầu lý do.

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Hạn chế thông tin hiển thị | Chỉ thông tin quản lý tài khoản nêu ở §2; không hiện hồ sơ y tế/nhật ký sức khoẻ | Phù hợp mục đích quản lý tài khoản |
| Bắt buộc ghi lý do khi khoá | FE và API đều kiểm tra lý do. API loại khoảng trắng đầu/cuối, từ chối lý do trống hoặc dài hơn 1.000 ký tự sau chuẩn hoá | Lý do được truyền vào `detail` của sự kiện `PATIENT_LOCK`, cùng đối tượng bệnh nhân và người thực hiện |
| Khoá không xoá dữ liệu | Giữ nhật ký/hồ sơ; chặn đăng nhập và tăng phiên bản token để vô hiệu JWT đã cấp | Mở khoá cho phép đăng nhập mới, không khôi phục JWT đã bị thu hồi |

API khoá: `POST /api/v1/cms/users/{patientId}/lock`, body JSON `{ "reason": "Lý do can thiệp" }`. Thiếu body hoặc lý do không hợp lệ trả 400 trước khi gọi thao tác khoá. API mở khoá giữ nguyên `POST /api/v1/cms/users/{patientId}/unlock`, không cần body. Client gọi API khoá phải gửi lý do theo hợp đồng này.

Luồng gọi ghi Audit Log sau khi thao tác khoá/mở khoá thành công. Bệnh nhân được lưu ở PostgreSQL, audit ở MySQL; cơ chế hiện tại bắt lỗi ghi audit để không làm hỏng thao tác chính đã hoàn tất. Vì vậy chưa có bảo đảm nguyên tử giữa hai database khi MySQL gặp lỗi. Yêu cầu ghi audit bền vững qua sự cố cần được xử lý riêng ở tầng hạ tầng audit.

## 4. Tham chiếu

- Phân quyền hệ thống (khác với quản lý tài khoản bệnh nhân): [SCR-AD-03-PhanQuyenRBAC.md](SCR-AD-03-PhanQuyenRBAC.md)
- Tra cứu nhật ký khoá/mở: [SCR-AD-06-NhatKyHoatDong.md](SCR-AD-06-NhatKyHoatDong.md).
- Đối chiếu code: `CmsUsersPage.tsx`, `cmsUsers.ts`, `LockPatientRequest.java`, `CmsUserAdminController.java`, `AdminPatientManagementService.java`, `AuditLogService.java`.
