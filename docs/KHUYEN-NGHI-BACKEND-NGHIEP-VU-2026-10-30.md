# Nguồn đối chiếu backend và nghiệp vụ quản lý nhà trọ

> **Cập nhật sau triển khai V13:** tài liệu này giữ nhận xét của bước nghiên cứu trước khi sửa. Hiện README/API/CSDL đã chuyển sang V13; `correctMeterLine` đã nối API và có kiểm thử HTTP. Lượt kiểm chứng đạt 123 test (61 ca ứng dụng/API V13, 8 migration, 54 hồi quy/đơn vị) và 20 ca SQL. Xem [biên bản triển khai và giới hạn còn lại](TRIEN-KHAI-BACKEND-V13.md), [kết quả Java](implementation-validation49.json), [kết quả SQL](database-validation.json). Không đọc các câu “chưa sửa/chưa chạy” bên dưới như trạng thái hiện hành.

Ngày kiểm tra lại: **30/10/2026**. Phạm vi: đọc repository của nhóm, mã nguồn/test của các dự án tham khảo và tài liệu chính thức của sản phẩm. Không chạy các ứng dụng tham khảo hoặc bộ kiểm thử trong lượt nghiên cứu này.

**Lựa chọn chính: OpenKOS để đối chiếu toàn bộ luồng nhà trọ. Giữ Spring Boot/PostgreSQL hiện tại; dùng thêm LeaseBook để kiểm tra luồng tiền và Resident để đối chiếu cách vận hành tại Việt Nam.** Đây là lựa chọn nguồn tham khảo, chưa phải xác nhận một backend đã đúng toàn bộ hoặc đủ điều kiện vận hành thực tế.

## 1. Phải xác định đúng bản của đồ án

- Repository được cung cấp: <https://github.com/hieunofun/quanlytro>.
- Workspace đang có nhiều thay đổi chưa commit. `README.md` local còn mô tả 17 bảng; `application.yml` lại mặc định profile `dev`, với nhóm `dev`/`prod` bật `model49`. Có V11/V12 và các service `Leases49`, `Meters49`, `Billing49`, `Finance49`, `Settlements49`.
- Vì vậy, mô tả trên GitHub, README local và bản triển khai 49 bảng không được coi là cùng một phiên bản. Trước buổi kiểm tra, cần chốt mã chạy, migration, API và tài liệu tương ứng.
- Đã có báo cáo nghiên cứu ngày 26/10 trong [THAM-KHAO-OPEN-SOURCE-GITHUB.md](THAM-KHAO-OPEN-SOURCE-GITHUB.md). Báo cáo này kiểm tra lại nguồn và bổ sung lựa chọn cùng stack, sản phẩm Việt Nam và nhận xét từ code hiện tại; không thay thế kết quả chạy thử.

## 2. So sánh các lựa chọn

| Nguồn | Công nghệ/phạm vi | Phần đáng học | Giới hạn ảnh hưởng đến lựa chọn |
| --- | --- | --- | --- |
| [senatroxx/OpenKos](https://github.com/senatroxx/OpenKos) | Laravel, React/Inertia, PostgreSQL; boarding house/kos | Phòng, người thuê, hợp đồng, điện nước, xác nhận thanh toán, sửa chữa, quyết toán cọc | README ở snapshot đã đọc ghi MVP/v0.1 Alpha, chưa được hoàn thiện cho production. Phù hợp nhất về đề tài, chưa đủ căn cứ coi là chuẩn vận hành |
| [jwh3times/LeaseBook](https://github.com/jwh3times/LeaseBook) | ASP.NET Core, React/TypeScript, PostgreSQL | Sổ tiền, cọc, trả trước, phân bổ, hoàn và đảo giao dịch; kiểm thử cạnh tranh | README ghi pre-release và chưa triển khai production. Phạm vi kế toán ủy thác rộng hơn đồ án; chọn làm nguồn đối chiếu tài chính |
| [TechFunWay/rental](https://github.com/TechFunWay/rental) | Go/Gin/GORM, Vue 3, SQLite; chủ cho thuê nhỏ | Nhập số công tơ, lập hóa đơn, phiếu thu, không thu lặp cọc ở các tháng sau | Code hóa đơn dùng `float64`; cách ghi cọc trong hóa đơn đầu chưa thay thế được sổ giữ/hoàn cọc |
| [KLBMinhLong/StudentDormitoryManagement](https://github.com/KLBMinhLong/StudentDormitoryManagement) | Java 21, Spring Boot, PostgreSQL; ký túc xá | Cách tổ chức controller/service/repository/DTO; tài liệu hợp đồng, điện nước, thanh toán PayOS | Nghiệp vụ theo giường/sinh viên khác nhà trọ theo hợp đồng. Cây file đã kiểm tra chỉ thấy test khởi động và BuildingService, chưa thấy bộ test tài chính tương ứng; chưa thấy LICENSE |
| [MicroRealEstate](https://github.com/microrealestate/microrealestate) | Hồ sơ cho thuê, hợp đồng, tiền thuê, biên nhận | Mẫu tài liệu và trải nghiệm quản lý hồ sơ | Nhánh hiện tại dùng Sustainable Use License; snapshot cũ kết thúc ở `479d0ad…` mới thuộc MIT. Không gộp tính năng nhánh mới với giấy phép bản cũ |
| [Resident](https://resident.vn/) | Sản phẩm quản lý nhà trọ tại Việt Nam | Chốt điện nước → hóa đơn → nhận tiền/gạch nợ; công nợ, tiền dư, sự cố, phân quyền | Nguồn tham khảo quy trình công khai; chưa truy cập hay kiểm toán backend của nhà cung cấp |

Giấy phép đã kiểm tra: OpenKOS Apache-2.0; LeaseBook AGPL-3.0; TechFunWay/rental MIT. Xem giấy phép của đúng phiên bản trước khi đưa code vào đồ án. Với repo chưa thấy LICENSE, chỉ kết luận là mã đang công khai, chưa suy ra quyền tái sử dụng.

## 3. Vì sao chọn OpenKOS, và phần nào không nên sao chép

OpenKOS hướng tới boarding house/kos, gần đề tài hơn hệ thống đặt phòng khách sạn. [README tại snapshot aeb3544](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/README.md) cũng ghi rõ trạng thái Alpha.

Đã đọc các bằng chứng cụ thể:

- [UtilityReadingTest](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/tests/Feature/UtilityReadingTest.php): nối chuỗi chỉ số; chặn chỉ số giảm/trùng; bảo vệ chỉ số có kỳ sau phụ thuộc.
- [UtilityBillingTest](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/tests/Feature/UtilityBillingTest.php): ranh giới thuê, snapshot giá, tránh tính lại, điều chỉnh chênh lệch liên kết khoản gốc.
- [DepositSettlementTest](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/tests/Feature/DepositSettlementTest.php): khấu trừ có chi tiết, đối chiếu đủ số cọc và bảo vệ bản đã chốt.

Hai khác biệt phải giữ trong đầu khi đối chiếu:

1. [VerifyPayment](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/app/Actions/Payments/VerifyPayment.php) từ chối xác nhận nếu tổng khoản thu vượt hóa đơn. Với đồ án có khách chuyển dư, cần ghi nhận đủ tiền thực nhận rồi phân bổ phần dư vào trả trước.
2. [SettleLeaseDeposit](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/app/Actions/Leases/SettleLeaseDeposit.php) khởi tạo số cọc từ `lease.deposit_amount`. Chỉ file này không chứng minh đã đối chiếu được cọc thực nhận từng lần. Bài của nhóm phải phân biệt cọc cam kết với số tiền thực đang giữ.

LeaseBook bổ sung đúng phần còn cần đối chiếu: [ADR-011](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/docs/adr/ADR-011-deposit-prepayment-over-application-policy.md) phân biệt tiền thu dư với cấn quỹ vượt nợ; [PostingConcurrencyTests](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/tests/LeaseBook.Tests.Accounting/PostingConcurrencyTests.cs) kiểm tra hai thao tác cùng tranh số dư/khoản nợ. [README](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/README.md) công bố trạng thái phát triển, không phải bằng chứng hệ thống đã được triển khai thực tế.

Nguồn còn lại: [test cọc TechFunWay/rental](https://github.com/TechFunWay/rental/blob/98e65d2c65c04a8e6bba321b6774ded72c5e812d/server/rental/deposit_test.go), [mã tính hóa đơn](https://github.com/TechFunWay/rental/blob/98e65d2c65c04a8e6bba321b6774ded72c5e812d/server/rental/bills.go), [README dự án Spring Boot ký túc xá](https://github.com/KLBMinhLong/StudentDormitoryManagement/blob/main/README.md), [thông báo giấy phép MicroRealEstate](https://github.com/microrealestate/microrealestate/blob/main/LICENSE.md).

## 4. Nhận xét từ mã hiện tại của nhóm

Đây là nhận xét tĩnh, không phải kết quả chạy test:

| Phần đã thấy | Bằng chứng local | Ý nghĩa |
| --- | --- | --- |
| Tách phiếu thu, quỹ và phân bổ | [Finance49](../src/main/java/com/hostel/model49/Finance49.java), [Model49Tests](../src/test/java/com/hostel/Model49Tests.java) | Có nền cho tiền dư, cọc thực nhận và hoàn; nên kiểm chứng tiếp thay vì thay cả backend |
| Khóa bản ghi và mã chống lặp | `Finance49.receive/entry`, các test thanh toán đồng thời trong `Model49Tests` | Đã có cơ chế bảo vệ số dư; sự tồn tại của test chưa chứng minh tất cả đường gọi API đều đúng |
| Tách việc nhận/trả thực tế khỏi ngày hợp đồng | [Leases49](../src/main/java/com/hostel/model49/Leases49.java), [OpenKosWorkflowTests](../src/test/java/com/hostel/OpenKosWorkflowTests.java) | Đã có test cho hết hạn giấy nhưng chưa trả phòng, điện nước trước nhận/sau trả, kỳ đo chồng |
| Duyệt quyết toán và chi tiền là hai bước | [Settlements49](../src/main/java/com/hostel/model49/Settlements49.java) và `Finance49.refund` | Có cơ sở thể hiện đúng số cần hoàn và số đã chi |
| Sửa tiền điện đã phát hành chưa thấy nối thành luồng chuyên biệt | Có `Finance49.correctMeterLine`; tìm trong `src`, `frontend/src` và tài liệu API chưa thấy caller của hàm | Ưu tiên kiểm tra qua HTTP. Vẫn có API điều chỉnh chung; chưa kết luận toàn hệ thống không thể điều chỉnh |
| Tài liệu đang lệch phiên bản | README 17 bảng, profile mặc định model49; các tài liệu lịch sử cùng tồn tại | Phải chốt bộ API/ERD/kịch bản test khớp mã sẽ trình bày |

Tài liệu ngày 26/10 ghi một số ca còn đề xuất; hiện `OpenKosWorkflowTests` đã có mã cho một phần các ca đó. Không lặp lại nhận xét cũ rằng tất cả ca này chưa được viết. Lượt này chưa chạy lại nên không công bố số test đạt.

## 5. Bộ tình huống cần chứng minh trước buổi kiểm tra

| Tình huống | Kết quả nghiệp vụ cần đạt |
| --- | --- |
| Cọc cam kết 2 triệu, mới thu 1,5 triệu | Sổ tiền giữ 1,5 triệu; còn thiếu 500 nghìn; giao phòng thiếu cọc có căn cứ và quyền phù hợp |
| Hóa đơn 3.620.000, nhận 4.000.000, gửi lại cùng mã giao dịch | Tiền thực nhận chỉ tăng một lần; nợ về 0; trả trước còn 380.000 |
| Hai nhân viên cùng phân bổ/cấn/hoàn một số dư | Không chi hoặc cấn hai lần; số dư và khoản nợ không bị âm do tranh chấp cập nhật |
| Người cũ trả, người mới nhận giữa tháng | Tách tiền phòng theo chính sách đã chốt; điện nước có điểm bàn giao, không tính trùng hoặc đẩy phần trống cho khách |
| Giá đổi hoặc số người thực ở đổi trong kỳ | Áp dụng đúng phiên bản giá và ngày hiệu lực; dịch vụ đầu người có căn cứ ngày-người |
| Đã phát hành và thu đủ, phát hiện tính thừa 10 kWh | Giữ chứng từ gốc; điều chỉnh theo giá nguồn; giải phóng phần phân bổ dư về đúng quỹ, chưa tự coi là đã hoàn tiền |
| Thiệt hại vượt cọc hoặc còn tranh chấp | Theo dõi nợ vượt cọc; khoản chưa xác minh không tự bị cấn; từng khoản có căn cứ |
| Trả phòng nhưng chưa chốt được điện nước cuối | Có chính sách tạm giữ, lý do và ngày đối soát; không biến số ước tính thành số đo thực |
| STAFF đổi ID phòng/hợp đồng/quỹ/ảnh ngoài phạm vi | Bị chặn tại backend, không chỉ ẩn nút ở frontend |
| Hợp đồng hết hạn nhưng khách chưa bàn giao | Không tự giao phòng đang bị chiếm hữu cho khách mới |

Với cảnh báo tiêu thụ bất thường, trong các nguồn đã đọc chưa xác minh được dự án đáp ứng toàn bộ cách chuẩn hóa theo ngày, thiếu lịch sử, đổi người thuê và truy vết baseline của bài. Phần này cần tiêu chí và ca kiểm thử riêng.

## 6. Cách dùng kết quả nghiên cứu

1. Chốt bản backend đem kiểm tra và sửa tài liệu lệch phiên bản.
2. Dùng OpenKOS đối chiếu trạng thái, điều kiện trước/sau và ca lỗi của từng Use Case; dùng LeaseBook đối chiếu thu/cấn/hoàn/đảo tiền.
3. Chạy các tình huống trên qua API và PostgreSQL thử riêng, ghi số tiền/chứng từ trước và sau. Chỉ sửa service hoặc DB khi xác định được ca không đạt.
4. Đối chiếu thao tác và chứng từ với [quy trình Resident công khai](https://resident.vn/). Thông tin nhà cung cấp mô tả không thay cho kiểm chứng backend. Để xác nhận sát hoạt động thực tế, cần đối chiếu tiếp với quy trình và chứng từ đã ẩn dữ liệu cá nhân của một chủ trọ cụ thể.

Không có căn cứ từ lượt đọc này để đổi Java sang PHP/.NET, tăng số bảng, hoặc coi một repo công khai là chuẩn mặc định cho mọi nhà trọ.
