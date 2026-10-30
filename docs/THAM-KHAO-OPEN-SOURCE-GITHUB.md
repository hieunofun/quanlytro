# Nguồn GitHub tham khảo cho đề tài quản lý nhà trọ

Ngày kiểm tra: **26/10/2026**. Đánh giá cho đề tài của nhóm: hợp đồng, cư trú, hóa đơn minh bạch, điện nước, thu tiền/cọc, trả phòng và cảnh báo tiêu thụ.

**Nên đọc OpenKOS trước để xem toàn bộ luồng; dùng LeaseBook để đối chiếu sổ tiền/cọc; dùng TechFunWay/rental để tham khảo nhập điện nước và hóa đơn.** Đây là lựa chọn tham khảo sau khi đọc một số code/test, không phải xác nhận các dự án đã được kiểm toán hoặc đã chạy đúng toàn bộ nghiệp vụ Việt Nam.

Đã xem README, giấy phép, cấu trúc repository và các file nguồn/test liên kết dưới đây. **Chưa cài, chạy hoặc thực thi bộ test của các repo này.** Không lấy số sao hoặc lời tự giới thiệu “production-ready” làm bằng chứng nghiệp vụ.

## 1. Các lựa chọn đáng đọc

| Ưu tiên | Repository | Công nghệ | Giấy phép | Dùng tốt nhất cho |
| --- | --- | --- | --- | --- |
| 1 | [senatroxx/OpenKos](https://github.com/senatroxx/OpenKos) | Laravel, React/Inertia, PostgreSQL | [Apache-2.0](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/LICENSE) | Luồng vận hành nhà trọ, hợp đồng, điện nước, thanh toán và trả phòng |
| 2 | [jwh3times/LeaseBook](https://github.com/jwh3times/LeaseBook) | ASP.NET Core, React/TypeScript, PostgreSQL | [AGPL-3.0](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/LICENSE) | Tách tiền thu, công nợ, cọc, trả trước, hoàn và đảo bút toán |
| 3 | [TechFunWay/rental](https://github.com/TechFunWay/rental) | Go/Gin/GORM, Vue 3, SQLite | [MIT](https://github.com/TechFunWay/rental/blob/98e65d2c65c04a8e6bba321b6774ded72c5e812d/LICENSE) | Vận hành quy mô nhỏ, nhập chỉ số, tính phí, phiếu thu, giao diện di động |
| 4 | [MicroRealEstate — snapshot MIT năm 2025](https://github.com/microrealestate/microrealestate/tree/479d0ad703436071b64dfdf4e48321c121d8efb5) | Node.js/React, MongoDB, Docker | [MIT tại snapshot này](https://github.com/microrealestate/microrealestate/blob/479d0ad703436071b64dfdf4e48321c121d8efb5/LICENSE) | Hồ sơ cho thuê, mẫu hợp đồng, chứng từ và theo dõi thu tiền |

Giấy phép trong bảng là thông tin của chính repository ở phiên bản đã kiểm tra. Trước khi đưa mã nguồn vào đồ án, đọc giấy phép của đúng phiên bản và các phụ thuộc sử dụng.

## 2. OpenKOS — lựa chọn chính để tham khảo tổng thể

Phạm vi hướng tới boarding house/kos, gần loại hình nhà trọ hơn hệ thống đặt khách sạn. README mô tả quản lý nhiều cơ sở, phòng, nhiều người thuê, hóa đơn, bằng chứng thanh toán và yêu cầu sửa chữa. [Nguồn dự án](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/README.md).

Điểm đã thấy trong code/test:

- Công tơ có chuỗi chỉ số; kiểm tra kỳ đo chồng, đầu kỳ phải nối số cuối kỳ trước và chỉ số không giảm. Có test cho chuỗi này. [Xử lý điểm đo](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/app/Actions/Utility/RecordUtilityReading.php), [test](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/tests/Feature/UtilityReadingTest.php).
- Thanh toán có bước xác nhận, khóa bản ghi và phân bổ vào hóa đơn. Test có thanh toán một phần/đủ. [Xác nhận](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/app/Actions/Payments/VerifyPayment.php), [test phân bổ](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/tests/Feature/PaymentAllocationTest.php).
- Quyết toán có nháp, từng khoản khấu trừ kèm lý do và khóa sau khi chốt; test kiểm tra phân bổ vượt cọc và thay đổi bản đã chốt. [Xử lý quyết toán](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/app/Actions/Leases/SettleLeaseDeposit.php), [test quyết toán](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/tests/Feature/DepositSettlementTest.php).

**Điểm cần điều chỉnh theo bài của nhóm:** hàm xác nhận chặn tiền vượt hóa đơn; hàm quyết toán lấy số cọc gốc từ `lease.deposit_amount`. Vì vậy không dùng hai luồng này làm bằng chứng đã giải quyết tiền thu dư hoặc số cọc thực nhận từng lần. Bài hiện tại vẫn cần quỹ trả trước và lịch sử thu/hoàn riêng. Đây là nhận xét từ các file trên, chưa phải kết luận kiểm toán toàn bộ OpenKOS.

**Đề xuất:** dùng để so Use Case, biểu mẫu, chuyển trạng thái, nhập điện nước và danh sách ca kiểm thử. Không cần đổi backend Java sang PHP chỉ để học nghiệp vụ.

**Đã bắt đầu đối chiếu:** [Ma trận với mã hiện tại, 15 ca bổ sung và việc cần làm tiếp](DOI-CHIEU-OPENKOS-VA-KE-HOACH-TIEP-THEO.md). Ưu tiên kiểm chứng ranh giới điện nước, tiếp nhận khách mới và biểu mẫu quyết toán; chưa thay mô hình DB theo OpenKOS.

## 3. LeaseBook — nguồn tham khảo mạnh về tính minh bạch tài chính

Dự án đặt sổ kế toán kép làm trung tâm; tách tiền của chủ nhà, tiền cọc và tiền quản lý. README ghi rõ **pre-release, đang phát triển và chưa triển khai production**; luồng hợp đồng/sửa chữa đầy đủ còn trên lộ trình. [Trạng thái và kiến trúc](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/README.md).

Những phần đáng đọc cho bài của nhóm:

- Thu cọc là nghiệp vụ riêng, ghi nghĩa vụ tiền đang giữ; cấn cọc là nghiệp vụ khác. [CollectDeposit](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/src/LeaseBook.Modules.Accounting/Features/LedgerPosting/CollectDeposit.cs), [ApplyDeposit](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/src/LeaseBook.Modules.Accounting/Features/LedgerPosting/ApplyDeposit.cs).
- Quyết định thiết kế phân biệt tiền khách nộp vượt nợ với việc cấn quỹ vượt nợ: khoản thu dư có đường vào trả trước, cấn quỹ vượt khoản phải thu bị chặn. [ADR-011](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/docs/adr/ADR-011-deposit-prepayment-over-application-policy.md).
- Có mã kiểm thử hai khoản thu/cấn/hoàn cùng lúc và đảo trùng một giao dịch. [PostingConcurrencyTests](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/tests/LeaseBook.Tests.Accounting/PostingConcurrencyTests.cs), [ReversalServiceTests](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/tests/LeaseBook.Tests.Accounting/ReversalServiceTests.cs).

**Đề xuất:** học ranh giới nghiệp vụ và ca kiểm thử. Phần nhiều chủ nhà, tài khoản ủy thác và kế toán đầy đủ lớn hơn phạm vi đồ án; không cần sao chép toàn bộ cấu trúc hoặc các chính sách đặc thù thị trường của nó.

## 4. TechFunWay/rental — nguồn tham khảo sát việc ghi điện nước hằng tháng

Phần mềm hướng tới chủ cho thuê nhỏ, có công tơ điện/nước/gas, hóa đơn, theo dõi thu và in chứng từ; hỗ trợ Docker và giao diện di động. [Giới thiệu và cách chạy](https://github.com/TechFunWay/rental/blob/98e65d2c65c04a8e6bba321b6774ded72c5e812d/README.md).

Code có test ghi/cập nhật số công tơ, lấy chỉ số kỳ trước và tạo bản ghi công tơ khi lập hóa đơn. Có test cọc chỉ xuất hiện ở hóa đơn đầu, không lặp lại mỗi tháng và không tự bổ sung vào hóa đơn cũ. [Test công tơ](https://github.com/TechFunWay/rental/blob/98e65d2c65c04a8e6bba321b6774ded72c5e812d/server/rental/meters_test.go), [test cọc](https://github.com/TechFunWay/rental/blob/98e65d2c65c04a8e6bba321b6774ded72c5e812d/server/rental/deposit_test.go).

**Giới hạn:** trong `bills.go`, nhiều trường tiền và số đã thu dùng `float64`. Với mục tiêu hóa đơn minh bạch của nhóm, tiếp tục dùng `BigDecimal/numeric`. Việc thêm cọc vào hóa đơn đầu cũng không thay cho sổ thu/giữ/hoàn cọc. [Nguồn tính và thu hóa đơn](https://github.com/TechFunWay/rental/blob/98e65d2c65c04a8e6bba321b6774ded72c5e812d/server/rental/bills.go).

**Đề xuất:** tham khảo giao diện nhập liệu, quy trình chốt số và các ca không thu cọc lặp. Độ phù hợp với quỹ tiền khách và quyết toán nâng cao cần đánh giá thêm.

## 5. MicroRealEstate — phải phân biệt phiên bản và giấy phép

Snapshot `479d0ad703436071b64dfdf4e48321c121d8efb5` có README mô tả hồ sơ nhà/người thuê, mẫu hợp đồng, chứng từ và theo dõi tiền thuê, kèm MIT. Có thể dùng snapshot này để nghiên cứu trải nghiệm quản lý cho thuê. [README snapshot](https://github.com/microrealestate/microrealestate/blob/479d0ad703436071b64dfdf4e48321c121d8efb5/README.md), [LICENSE snapshot](https://github.com/microrealestate/microrealestate/blob/479d0ad703436071b64dfdf4e48321c121d8efb5/LICENSE).

**Không gọi nhánh hiện tại là MIT:** giấy phép hiện tại ghi các commit từ `a95d44d...` thuộc Sustainable Use License; có giới hạn mục đích sử dụng và quy mô. Tính năng thấy trên nhánh mới không mặc nhiên có trong snapshot MIT. [Thông báo chuyển giấy phép của tác giả](https://github.com/microrealestate/microrealestate/blob/main/LICENSE.md).

## 6. Hai nguồn bổ sung, không chọn làm nền chính

| Nguồn | Giá trị tham khảo | Lý do cần thận trọng |
| --- | --- | --- |
| [zite/property-management](https://github.com/zite/property-management) | Luồng báo trả, khấu trừ, cấn cọc, hoàn và gửi bảng quyết toán; có sổ kế toán kép | Mã MIT nhưng ứng dụng dựa trên tài khoản/runtime/database của Zite. README còn mô tả các tham chiếu dạng text. Không coi đây là gói tự host độc lập tương đương ứng dụng Java/PostgreSQL hiện tại. [Kiến trúc](https://github.com/zite/property-management/blob/71bb2c66b7f112aa1661e1d3b55648db020537d3/README.md) |
| [ngDuyHung/QuanLyTro-Backend](https://github.com/ngDuyHung/QuanLyTro-Backend) | Bối cảnh nhà trọ Việt Nam; có service hợp đồng, hóa đơn và phiếu tài chính | Chưa thấy LICENSE ở cây mã/API repository đã kiểm tra. Các file test trong thư mục tests chủ yếu là ExampleTest; chưa có căn cứ nhận là bộ kiểm chứng nghiệp vụ. Chỉ chọn để đọc đối chiếu, chưa chọn để tái sử dụng code. [LeaseService](https://github.com/ngDuyHung/QuanLyTro-Backend/blob/e696cd79503d28e34f67a158ba33a9a871b7980c/app/Services/LeaseService.php) |

Đã loại khỏi nhóm ưu tiên các mẫu chỉ lưu localStorage, repo chỉ đăng tin/tìm phòng và những repo có mô tả nhiều tính năng nhưng chưa có bằng chứng tương ứng trong phần code đã đọc. Repo fork không được tính như một lựa chọn độc lập nếu chưa có khác biệt đáng kể.

## 7. Dùng các nguồn này để kiểm tra lại đồ án

| Nội dung cần đối chiếu | Nguồn nên đọc | Điều cần giữ cho bài hiện tại |
| --- | --- | --- |
| Nhập chỉ số, kỳ đo, sửa số | OpenKOS + TechFunWay/rental | Nối đúng công tơ; giữ lịch sử; không sửa đè số đã phát hành |
| Cọc cam kết so với tiền đã nhận | LeaseBook | Chỉ thu thực tế mới tăng tiền đang giữ |
| Khách chuyển dư, trả nhiều lần | LeaseBook + test OpenKOS | Ghi đủ tiền vào; phân bổ nợ riêng; dư nằm ở trả trước |
| Trả phòng và khấu trừ | OpenKOS | Từng khoản có lý do; khoản nợ vượt cọc vẫn theo dõi được |
| Hoàn cọc đồng thời, gửi lại thao tác | LeaseBook | Không chi/cấn hai lần; có khóa và mã chống trùng |
| Mẫu hợp đồng/chứng từ | MicroRealEstate MIT | Tài liệu theo phiên bản, số liệu có nguồn |

**Riêng cảnh báo tiêu thụ bất thường:** trong phạm vi code/tài liệu đã xem, chưa xác minh được repo nào đáp ứng đầy đủ cách so mức dùng theo ngày, xử lý kỳ ngắn, khác nhóm người thuê và lưu nguồn baseline như yêu cầu của nhóm. Có ghi công tơ hoặc nhắc quá hạn thanh toán không có nghĩa đã có chức năng phát hiện tiêu thụ bất thường.

Các nguồn này nên giúp rà lại các quyết định nghiệp vụ; không phải lý do tự tăng số bảng, đổi công nghệ hoặc thay mô hình 49 bảng đã chọn. Nếu dựng thử, dùng dữ liệu và môi trường riêng; kết quả nghiên cứu hiện tại không được tính là kết quả chạy thử.
