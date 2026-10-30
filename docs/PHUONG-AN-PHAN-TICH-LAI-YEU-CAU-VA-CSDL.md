# Phương án phân tích lại yêu cầu và cơ sở dữ liệu — đã chọn triển khai

Ngày lập: **19/10/2026**. Dự án: **Hệ thống quản lý nhà trọ với hóa đơn minh bạch và cảnh báo tiêu thụ điện nước bất thường**.

Rà lại theo vận hành thực tế: **19/10/2026**. Chủ nhiệm đồng ý triển khai ngày **26/10/2026**. Kết quả và giới hạn kiểm chứng được ghi riêng tại [Triển khai mô hình 49 bảng](TRIEN-KHAI-MO-HINH-49-BANG.md). Nội dung phân tích bên dưới giữ bối cảnh khảo sát ban đầu; danh sách tình huống không tự đồng nghĩa đã nghiệm thu toàn bộ.

**Bản CSDL đã chọn:** [49 bảng và các liên kết](PHUONG-AN-CSDL-49-BANG.md). Chứng từ tháng/bồi thường/điều chỉnh dùng chung đầu–chi tiết; tiền nhận/chi dùng cash_transactions và sổ phân bổ/quỹ; ảnh dùng hệ tệp chung. Danh mục thực thể ứng viên ở mục 7 là phân tích nghiệp vụ, không phải danh sách bảng vật lý được cộng lại.

**Trạng thái: ĐÃ ĐỒNG Ý TRIỂN KHAI.** Đã thay đổi ERD local, DBML, SQL, migration và backend trong mã nguồn. Chưa chuyển database đang dùng, chưa cập nhật slide hoặc công bố lại sơ đồ trực tuyến. Đây không phải ghi nhận giảng viên đã duyệt mô hình.

Nhóm **gehihi**: Nguyễn Doãn Huy, Phan Văn Hiếu, Nguyễn Trần Tuấn Khôi, Nie Phương Anh, Trần Đức Nguyên, Nguyễn Quang Trung. Chưa có dữ liệu thực tế về MSSV, phần việc, minh chứng và tỷ lệ đóng góp; không điền thay. Mục phân công “nhóm 5 người” trong V1 là kế hoạch cũ, không dùng để tự phân công cho sáu thành viên hiện nay.

## 1. Kết luận để chốt trước
**Mô hình 17 bảng có các ràng buộc và luồng thu tiền đáng giữ, nhưng chưa bao phủ kế hoạch V1. Vấn đề nằm ở phạm vi và nghiệp vụ, không chỉ ở cách diễn đạt slide.** Chưa có căn cứ kết luận toàn bộ V1 đã được thầy duyệt; cũng không có căn cứ gán cho thầy một phương án DB cụ thể từ nhận xét “Suy nghĩ lại, phân tích đúng và đủ”.

Các thiếu hụt ảnh hưởng trực tiếp đến tính đúng:

1. Cọc thỏa thuận đang được dùng như cọc đã nhận. Thiếu chứng từ nhận từng phần, hoàn thực tế và đối soát số dư cọc.
2. Thiệt hại chỉ là tổng số trong quyết toán. Nếu lớn hơn cọc, phần vượt chưa có cơ chế thu và theo dõi như một khoản phải thu độc lập.
3. Sự cố buộc gắn hợp đồng; API tạo yêu cầu buộc hợp đồng ACTIVE. Chưa biểu diễn đủ sửa phòng trống, thiết bị chung và bảo trì định kỳ.
4. Gia hạn sửa trực tiếp `end_date` nhưng bị trigger chặn sau khi phát hành hóa đơn. Cần lịch sử gia hạn và quy tắc chỉ tác động tương lai.
5. Chỉ số được nối trong từng hợp đồng, chưa nối vật lý giữa hai khách dùng cùng công tơ. So lượng dùng kỳ lẻ với trung bình cả tháng có thể đánh giá sai.
6. Dịch vụ có giá trong khu đều được đưa vào hóa đơn; chưa có dữ liệu đăng ký/ngừng, số xe hoặc số lượng dùng riêng của hợp đồng.
7. Nhiều yêu cầu V1 bị rút khỏi bản hiện hành: tiện nghi có cấu trúc, ảnh phòng, lịch sử giá, nhật ký nghiệp vụ, thông báo, vi phạm, phân công theo phòng, ngưỡng theo phòng và khôi phục mật khẩu.

**Hướng khuyến nghị:** lấy V1 làm danh mục đối chiếu, bổ sung các dữ liệu cần thiết cho những tình huống đã nêu, giữ các nguyên tắc hóa đơn theo hợp đồng, bất biến sau phát hành và chống thu vượt nợ. Không đặt trước số bảng. Việc thu hẹp một yêu cầu V1 phải được ghi rõ và được anh đồng ý.

### 1.1. Danh sách quyết định cần anh xác nhận

Các lựa chọn dưới đây đều **chưa được duyệt**. Có thể trả lời theo mã, ví dụ `D01 đồng ý; D03 chọn B; D15 để sau`.

| Mã | Nội dung cần chốt | Phương án khuyến nghị / lựa chọn |
| --- | --- | --- |
| D01 | Phạm vi theo V1 | Giữ các mục V1 thuộc giai đoạn Java/Web trong mục 3. Nếu giảm mục nào, ghi cụ thể; không mặc định gọi là ngoài MVP. |
| D02 | Quyền người thuê | Phân biệt bên ký/chịu trách nhiệm, người liên hệ và người thực ở. Người ở có tài khoản có thể báo/theo dõi sự cố của mình và xem thông tin phòng được phép; quyền tài chính/hợp đồng cần được cấp rõ, không mặc nhiên cấp cho cả phòng. |
| D03 | STAFF tiếp nhận khách mới | Cho phép tiếp nhận trong ngữ cảnh một phòng được giao, lập hợp đồng nháp và tạo hồ sơ trong cùng quy trình; không mở tra cứu toàn bộ hồ sơ các khu khác. ADMIN quản lý tài khoản đăng nhập. |
| D04 | Tiện nghi và tài sản | A: danh mục tiện nghi + số lượng theo phòng, bàn giao theo hạng mục, đồ cần phân biệt có mã nội bộ. B: quản lý từng tài sản đầy đủ. Khuyến nghị A; serial, mua sắm và khấu hao để sau. |
| D05 | Cọc thiếu | Cho phép nhận nhiều lần; hiển thị cam kết, thực nhận, thiếu và số dư riêng. ADMIN hoặc người được ủy quyền cho nhận phòng khi chưa đủ theo hạn mức/lý do; không tự giả lập tiền đã nhận. |
| D06 | Bồi thường | Lập chi tiết có căn cứ, ADMIN duyệt khoản người thuê phải chịu; chi phí sửa thực tế là dữ kiện riêng. Phần chưa trả phải thu tiếp được. |
| D07 | Cấn cọc và hoàn tiền | Chỉ cấn khoản đã đủ điều kiện, theo phân bổ được xác nhận; bồi thường chưa thống nhất không mặc nhiên được ưu tiên. Hoàn chỉ ghi đã hoàn khi thực chi. Chuyển cọc có phê duyệt theo D21, không tự chuyển vì cùng phòng/người ở. |
| D08 | Đổi liên hệ / đổi bên thuê | Đổi người liên hệ không đổi bên chịu trách nhiệm và không bắt quyết toán. Nếu thay bên ký/chịu trách nhiệm, cần thỏa thuận có hiệu lực rõ; khuyến nghị hợp đồng mới cho trách nhiệm mới. Nợ/cọc trước mốc không tự chuyển. |
| D09 | Gia hạn và đổi giá | Cho gia hạn bằng phụ lục, có thể kèm giá/cọc mới từ ngày hiệu lực tương lai. Giữ lịch sử điều khoản/giá; không bắt ký hợp đồng mới chỉ vì đổi giá. Bản đầu ưu tiên hiệu lực đầu kỳ; đổi giữa kỳ cần chia các đoạn giá được thỏa thuận, không lấy giá cuối áp cho cả tháng. |
| D10 | Dịch vụ và cách chia phí | Có đăng ký/ngừng/số lượng theo hiệu lực. Từng dịch vụ chọn tính cả kỳ hoặc chia ngày theo thỏa thuận; gửi xe có thể là vé tháng hoặc xe-ngày. Phí đầu người dựa khoảng đăng ký cư trú áp dụng, không đếm ngẫu nhiên tại ngày lập hoặc theo việc có ngủ ở phòng mỗi ngày. |
| D11 | Công tơ và kỳ lẻ | Tách điểm đo vật lý/khoảng chịu trách nhiệm; dùng chung mốc bàn giao khi đổi khách. Hỗ trợ thay công tơ thủ công: chốt cũ, mở mới, cộng từng khoảng cùng công tơ. Không tự tính khi thiếu mốc; reset/quay vòng cần xác minh riêng. |
| D12 | Chu kỳ đo và cảnh báo | Kỳ hóa đơn tháng dương lịch, cửa sổ đo theo ngày chốt thực tế của khu, không bắt mọi nơi chốt cuối tháng. Cảnh báo theo lượng/ngày và lịch sử so sánh phù hợp; đổi nhóm người thuê/số người phải được nhận diện. Thiếu baseline không chặn hóa đơn chỉ vì thiếu baseline. |
| D13 | Sai hóa đơn đã phát hành | Giữ bản gốc; thêm chứng từ điều chỉnh có liên kết và phê duyệt nếu muốn xử lý đầy đủ. Khuyến nghị đưa vào phạm vi vì có thể phát hiện lỗi sau phát hành; không tạo hóa đơn tháng thứ hai để né UNIQUE. |
| D14 | Ủy quyền và xử lý khẩn | Quyền duyệt/cấn/hoàn tùy phân công và hạn mức do ADMIN thiết lập; không bắt chủ nhà duyệt mọi việc nhỏ. Sự cố khẩn được tiếp nhận/điều phối ngay trong phạm vi, phần chi phí/trách nhiệm được bổ sung và duyệt sau theo chính sách, có lịch sử. |
| D15 | Phí chậm trả | V1 có nêu tính năng tùy chọn. Khuyến nghị để sau trong lần làm lại này và ghi là một ngoại lệ phạm vi cần anh duyệt. Nếu giữ phải chốt công thức, ngày tính, mức trần, miễn giảm và không tính phí chồng lên phí. |
| D16 | Tầng | Giữ `floor_number` nếu chỉ dùng phân loại/lọc. Chỉ thêm thực thể tầng nếu cần tên, trạng thái hoặc nghiệp vụ độc lập của tầng. |
| D17 | Khôi phục mật khẩu | Giai đoạn đầu dùng mã một lần do ADMIN cấp sau xác minh; lưu mã băm, hết hạn, số lần thử và thời điểm dùng. Email tự phục vụ là lựa chọn bổ sung nếu có kênh email được xác minh. |
| D18 | Giới hạn vận hành | Một chủ nhà, ghi nhận thu/chi thủ công, một backend; chưa tích hợp ngân hàng thật, AI/IoT/mobile. Thuê nguyên phòng, không bán từng giường. Công tơ điện riêng/phòng; nước đo riêng hoặc tính theo đầu người đã cấu hình. Chia công tơ tổng nhiều phòng là giới hạn riêng, không tự chia đều. |
| D19 | Nhận tiền và phân bổ | Ghi đúng tiền thực nhận, kể cả trả trước, trả thừa, nộp hộ hoặc chưa rõ hợp đồng; sau đó mới phân bổ. Phân bổ không vượt tiền nguồn/nợ đích. Tiền chưa phân bổ khác cọc bảo đảm; một phiếu thu có thể trả nhiều khoản. |
| D20 | Báo trả và ở quá hạn | Tách ngày thông báo, dự kiến trả, bàn giao thực tế và kết thúc tính tiền theo thỏa thuận. Khách còn chiếm phòng sau hạn không làm phòng tự trống; báo xung đột khách kế tiếp và xử lý quyền ở/giá bằng phê duyệt, không tự gia hạn pháp lý. |
| D21 | Chuyển phòng và chuyển cọc | Chốt phòng cũ/nhận phòng mới ở mốc rõ, giữ nợ và chỉ số theo từng nơi. Nếu chuyển cọc cùng bên có quyền, dùng chứng từ chuyển hai đầu trong một transaction; không tạo thu/hoàn tiền giả và không chuyển khoản đang tranh chấp. |
| D22 | Giữ chỗ, hủy và ưu đãi | Nếu chủ nhà hứa giữ phòng hoặc nhận tiền trước khi ký, cần phiếu giữ chỗ có hạn, điều kiện hoàn/khấu trừ và trạng thái. Giữ chỗ khác hợp đồng/cọc bảo đảm. Miễn/giảm phí phải có căn cứ, thời gian, người duyệt; không xóa dòng thu hoặc sửa giá cũ. |
| D23 | Ngoại lệ và tranh chấp | Theo dõi chưa rõ/chờ xác minh/đang tranh chấp/tạm giữ có căn cứ. Không coi mất liên lạc là đã trả phòng hoặc tự xóa nợ. Nợ khó thu chỉ xử lý bằng quyết định riêng; bảo lưu dữ liệu và tiền chưa hoàn. |
| D24 | Lịch thu và kỳ từng khoản | Hỗ trợ tiền phòng tháng T thu trước, điện/nước theo kỳ đo đã chốt T−1; mỗi dòng có khoảng dịch vụ riêng. Giữ một hóa đơn tháng chuẩn/hợp đồng/kỳ, có chứng từ điều chỉnh/phát sinh/quyết toán riêng có nguồn, không ép mọi khoản cùng cửa sổ hoặc phát hành số đo chưa có. |

Các lựa chọn D01–D24 đều chưa được duyệt. D19–D24 và việc sửa D02/D07–D14/D18 xuất phát từ lần rà soát vận hành thực tế, không được gán thành yêu cầu sẵn có của V1/thầy. Mốc giá/cách thu/cách xử lý tiền giữ phải được ghi trong chính sách và thỏa thuận áp dụng.

### 1.2. Kết quả rà soát sát thực tế

**Bản trước chưa đủ để chốt triển khai thực tế.** Hướng quản lý cọc thực nhận, bồi thường riêng, bất biến chứng từ và kiểm soát đồng thời là phù hợp về logic; nhưng một số lựa chọn đang ép người dùng theo mô hình dữ liệu:

1. Bắt lập hợp đồng mới khi chỉ đổi liên hệ hoặc đổi giá gia hạn là quá chặt. Cần tách vai trò và lưu thay đổi có hiệu lực.
2. Chặn số thu lớn hơn nợ là đúng với **phân bổ**, nhưng không thể làm biến mất tiền khách thực sự đã chuyển thừa/trả trước.
3. Một khoản chuyển tiền có thể trả nhiều hóa đơn, tiền phòng có thể thu trước điện/nước; cần phiếu thu nguồn và phân bổ, kỳ từng dòng.
4. Ngày hết hạn, ngày rời chỗ ở, ngày giao lại chìa khóa và ngày hết nghĩa vụ trả tiền có thể khác nhau. Không tự giải phóng phòng chỉ vì hết hạn hoặc có thông báo trả.
5. Chưa có luồng đủ rõ cho chuyển phòng, thay công tơ, thiếu số đo, đổi kế hoạch trả, hủy sau nhận tiền và vắng tạm thời.
6. Chỉ người đại diện được báo sửa chữa hoặc mọi khoản đều chờ ADMIN sẽ gây nghẽn vận hành; quyền cần gắn cư trú/phạm vi và ủy quyền thực tế.
7. Cảnh báo thống kê không phải kết luận lỗi người thuê. Baseline của nhóm khách trước, thay số người, lịch đo lệch và công tơ hỏng phải được phân biệt.
8. Ưu tiên trừ bồi thường trước mọi khoản, mặc định cho rằng đại diện luôn ở, hoặc coi thiếu baseline là thiếu dữ liệu tính tiền đều không thể áp dụng cứng.

Các điểm này đã được sửa trong phương án dưới đây. Đây là rà soát theo nguồn nghiệp vụ công khai và mô phỏng tình huống, **chưa có phỏng vấn chủ trọ/đối soát sao kê, hợp đồng và hồ sơ bàn giao thật của một cơ sở cụ thể**. Không khẳng định bao phủ mọi tình huống ngoài thực tế.

## 2. Nguồn và mức độ kiểm chứng

### 2.1. Tài liệu đã đối chiếu

| Mã nguồn | Tài liệu / cách sử dụng |
| --- | --- |
| V1 | [Kế hoạch dự án gốc](source/v1.docx). Đã đọc toàn bộ nội dung văn bản trong `word/document.xml`; dùng làm danh mục yêu cầu, không coi là biên bản thầy phê duyệt. |
| DB | [SQL hiện hành](quanlytro.sql), [view và trigger](database-rules.sql), [DBML](quanlytro.dbml), [chuẩn hóa](NORMALIZATION.md). Đọc cấu trúc và quy tắc, không chạy SQL. |
| CODE | Các Controller/Service, `Access`, `SessionRegistry`, `Audit` và phần liên quan của [Flyway V10](../src/main/resources/db/migration/V10__normalize_to_approved_model.sql). Dùng xác minh hành vi đang viết trong code. |
| DOC | [API](API.md), [backend 17 bảng](BACKEND-17-TABLES.md), [nội dung thuyết trình](NOI-DUNG-THUYET-TRINH-BUOC-1-2.md), [rà soát slide](SLIDE-REVIEW.md), [đối chiếu V1 cũ](V1-COVERAGE.md). Tài liệu V1-COVERAGE tự ghi là lịch sử prototype, không dùng các chữ “Có” ở đó để kết luận V10 đã có. |
| SLIDE | Đã trích xuất tiêu đề/văn bản từ PowerPoint và PDF trong repo, xác nhận mỗi bản 22 trang; đối chiếu kỹ các trang quyền, quyết toán, quan hệ, chuẩn hóa, ví dụ tiền, phân công và sửa chữa. Có đọc các trang tương ứng của `E:\thiet-ke-csdl-va-phan-tich-nghiep-vu 1.pdf`. Không thực hiện một đợt nghiệm thu hình thức slide mới. |
| TEST-CŨ | [backend-validation.json](backend-validation.json), [database-validation.json](database-validation.json). Đây là hồ sơ kiểm thử bản hiện hành. Không chạy lại test ứng dụng và không gán kết quả này cho thiết kế đề xuất. |
| YC | Nội dung bối cảnh người dùng cung cấp; yêu cầu phân tích/chờ chốt và lần rà tiếp theo hướng thực tế. Các tình huống bổ sung không tự được gán thành yêu cầu của thầy. |
| NGUỒN-VẬN-HÀNH | Hướng dẫn do các nhà cung cấp phần mềm cho thuê công bố và tài liệu giá điện từ cơ quan/đơn vị điện lực; danh mục liên kết, giới hạn áp dụng ở mục 11.1. Dùng chứng minh tình huống có quy trình xử lý, không dùng làm khảo sát tần suất hay áp luật nước ngoài vào Việt Nam. |

Ký hiệu `V1 §5.2/P132` nghĩa là mục 5.2, đoạn XML thứ 132 trong phần thân tài liệu, tính cả đoạn trống; đây không phải số trang Word. Số đoạn giúp tìm lại chính xác câu gốc khi bố cục Word thay đổi.

Repository đang ở commit `ee554235cd517236cf34b6b6840820e86a2bd2a2`. Đếm tĩnh từ SQL/DBML xác nhận **17 bảng, 138 cột, 17 PK, 28 FK, 55 CHECK, 2 EXCLUDE, 6 view**. Chưa kết nối DB để xác nhận trạng thái runtime hiện tại, chưa truy cập lại ERD trực tuyến. Không suy từ các số đếm này rằng yêu cầu đã đủ.

### 2.2. Phát hiện có căn cứ trong code

| Mã | Điều đã kiểm chứng | Hệ quả nghiệp vụ |
| --- | --- | --- |
| E01 | `guard_checkout` gán `deposit_snapshot := c.deposit_amount`; [ContractService](../src/main/java/com/hostel/service/ContractService.java) cũng tính số cấn từ `deposit_amount`. | Cọc ghi trong hợp đồng bị dùng như tiền thực đã giữ; không biểu diễn được nhận thiếu. |
| E02 | `payment_transactions.invoice_id` bắt buộc; `checkout_records` chỉ có tổng `damage_amount`. | Chưa có đích ghi giao dịch thu bồi thường vượt cọc; một con số quyết toán âm không thay thế sổ phải thu. |
| E03 | [MaintenanceController](../src/main/java/com/hostel/controller/MaintenanceController.java) yêu cầu `contractId` và ACTIVE; DB cũng bắt buộc FK hợp đồng. | Không tạo sự cố trực tiếp cho phòng trống/thiết bị chung. ADMIN cũng bị hạn chế bởi cấu trúc này. |
| E04 | `guard_contract` cấm đổi `end_date` khi đã có hóa đơn ISSUED; endpoint gia hạn lại UPDATE trường đó. | Gia hạn thực tế sau thời gian đã thu tiền bị chặn, không chỉ thiếu câu giải thích trên slide. |
| E05 | [PricingService](../src/main/java/com/hostel/service/PricingService.java) lấy mọi dịch vụ có giá hiệu lực; [InvoiceService](../src/main/java/com/hostel/service/InvoiceService.java) lập dòng cho tất cả kết quả. | `is_mandatory=false` không có nghĩa là dịch vụ tự chọn đã được đăng ký đúng từng hợp đồng. |
| E06 | [MeterService](../src/main/java/com/hostel/service/MeterService.java) tìm chỉ số trước trong cùng `contract_id`; lần đầu nhận `oldIndex` nhập tay. | Chưa buộc chỉ số đầu khách mới bằng mốc trả của khách cũ trên cùng công tơ. |
| E07 | Baseline cộng lượng theo phòng/tháng của N tháng trước, rồi so trực tiếp với lượng bản ghi hợp đồng hiện tại. | Khi tháng có hai khách, lịch sử là tổng cả phòng còn kỳ đang xét có thể chỉ là vài ngày. Chưa chuẩn hóa theo thời lượng. |
| E08 | `MeterService.edit` nhận `reason` nhưng không lưu lý do; UPDATE thay `recorded_by`. [Audit](../src/main/java/com/hostel/service/Audit.java) chỉ log actor/action/resource/id, bỏ qua đối số trước/sau. | Không có lịch sử sửa trước/sau đáp ứng V1; người ghi ban đầu cũng không còn tách biệt rõ với người sửa. |
| E09 | [RetiredPrototypeController](../src/main/java/com/hostel/controller/RetiredPrototypeController.java) trả 410 cho ảnh phòng, giá phòng, phân công phòng, thông báo, vi phạm, bảo trì định kỳ, nhật ký và reset mật khẩu. | Những yêu cầu này từng có ở prototype nhưng không còn là chức năng của bản hiện hành. Archive không thay thế luồng đang dùng. |
| E10 | STAFF không được POST hồ sơ; `tenantScope` lại chỉ nhận người đã gắn với hợp đồng trong khu được giao. | Ngay cả hồ sơ khách mới do ADMIN tạo nhưng chưa có hợp đồng trong khu cũng không qua điều kiện này để STAFF lập hợp đồng. Cần một luồng tiếp nhận có phạm vi rõ. |
| E11 | `v_invoice_lines` chia mọi dòng không đo đếm cho số ngày tháng; chỉ có một dòng phòng và một dòng/dịch vụ. | Chưa biểu diễn riêng khoản thu một lần hoặc dịch vụ số lượng xe mà vẫn giữ đúng công thức. Không được nhét bồi thường vào dòng phí tháng. |
| E12 | `PUT /contracts/{id}/notes` sửa trực tiếp `terms`; chưa có bản hợp đồng đã ký bất biến trong mô hình. | Khóa hóa đơn không tự bảo toàn nội dung hợp đồng/PDF gốc. Cần phiên bản ký hoặc phụ lục. |
| E13 | V10 chuyển `rooms.amenities` cũ sang `rooms.description`; API phòng hiện không cung cấp chức năng chỉnh danh mục tiện nghi. `internal_note` được lưu và ẩn với TENANT nhưng chưa thấy endpoint hiện hành cập nhật nội dung này. | Không nói “tiện nghi hoàn toàn không còn dữ liệu”: có thể còn văn bản cũ, nhưng chưa có danh mục/số lượng/tra cứu và luồng sửa tương ứng. Ghi chú nội bộ mới đáp ứng một phần. |
| E14 | [ReportController](../src/main/java/com/hostel/controller/ReportController.java) có dashboard, công nợ, thu tháng/quý và lấp đầy; sự cố đang tổng hợp theo kỳ/trạng thái. | Không đánh đồng thiếu thông báo hết hạn với thiếu hoàn toàn danh sách sắp hết hạn. Báo cáo sự cố còn thiếu phân loại theo loại như V1. |

Các mục E01–E14 là kết luận từ đọc tài liệu/code. Tình huống lỗi thực tế tương ứng chưa được chạy lại trong lần phân tích này.

## 3. Phạm vi có truy nguồn

### 3.1. Những yêu cầu phải có nếu giữ kế hoạch Java/Web của V1

“Phải có theo V1” ở đây là cơ sở đối chiếu kế hoạch của nhóm, không phải khẳng định thầy đã phê duyệt. Trạng thái “Có nền tảng” chỉ xác nhận cấu trúc/code liên quan, không phải nghiệm thu đầy đủ.

| Mã | Yêu cầu và nguồn | Hiện trạng 17 bảng | Hướng xử lý đề xuất |
| --- | --- | --- | --- |
| R01 | Đăng nhập/xuất, phân quyền backend, đổi mật khẩu, JWT hết hạn/cấp lại, khóa sai nhiều lần — §5.1/P104–112 | Có nền tảng; bộ nhớ một tiến trình cho phiên và khóa tạm | Giữ; ghi rõ giới hạn khởi động lại/một instance. |
| R02 | Quên/đặt lại mật khẩu qua email hoặc mã — §5.1/P110 | Route cũ 410 | Bổ sung mã một lần, xác minh và lịch sử dùng; D17. |
| R03 | Quản lý nhân viên, phân công nhiều khu hoặc phòng, lưu thao tác — §4.2/P85–89 | Chỉ khu; thiếu lịch sử nghiệp vụ có trước/sau | Bổ sung phạm vi phòng và thời gian phân công, audit. |
| R04 | Khu/tầng/phòng, giá, diện tích, sức chứa, trạng thái, chặn thuê chồng/bảo trì — §5.2/P115–129 | Có nền tảng; tầng là số | Giữ ràng buộc, quyết định tầng ở D16; kiểm tra sức chứa cả khi sửa phòng. |
| R05 | Danh sách tiện nghi từng phòng — §5.2/P132 | Có thể có mô tả chuyển từ prototype, chưa có dữ liệu danh mục | Danh mục và liên kết phòng; mức theo dõi từng đồ ở D04. |
| R06 | Nhiều ảnh thực tế của phòng — §5.2/P133 | Không có bảng/route đang dùng | Ảnh phòng riêng, thứ tự hiển thị, quyền xem. |
| R07 | Ghi chú nội bộ không hiện cho người thuê — §5.2/P134 | Có cột và ẩn khi đọc; chưa có luồng cập nhật hiện hành rõ | Bổ sung thao tác cập nhật đúng quyền và audit. |
| R08 | Lịch sử giá thuê, không đổi giá hợp đồng cũ — §5.2/P135 | Giá hợp đồng riêng đã có; thiếu lịch sử giá niêm yết | Lịch sử giá phòng theo hiệu lực; không dùng làm nguồn tính lại hợp đồng đã ký. |
| R09 | Hồ sơ, đại diện, người ở cùng, lịch sử cư trú — §5.3/P139–147 | Có nền tảng; STAFF tiếp nhận mới bị vướng E10 | Chốt quyền D02/D03; giữ khoảng cư trú và tránh đếm đại diện hai lần. |
| R10 | Hợp đồng, nhận/quyết toán cọc, gia hạn, kết thúc/trả phòng — §3/P58; §5.4/P151–167 | Có nền tảng; cọc và gia hạn thiếu như E01/E04 | Sổ cọc thực tế, phụ lục gia hạn, tách kết thúc ở khỏi hết nợ. |
| R11 | Nhắc hợp đồng sắp hết hạn — §5.4/P170 | Có danh sách dashboard; chưa có thông báo tự động | Thông báo có người nhận, mốc nhắc và chống gửi trùng. |
| R12 | Điều khoản tùy chỉnh, PDF hợp đồng — §5.4/P171–172 | Có; điều khoản còn sửa trực tiếp | Giữ bản đã ký và phụ lục; xem E12. |
| R13 | Đánh dấu/xử lý vi phạm có lý do — §5.4/P173 | 410 | Hồ sơ vi phạm, trạng thái xử lý và sự kiện lịch sử; không tự biến vi phạm thành tiền phạt. |
| R14 | Chỉ số, giá dịch vụ theo thời gian, validate mới ≥ cũ — §6/P177–197 | Có nền tảng trong từng hợp đồng | Nối chuỗi vật lý cùng công tơ và lưu cửa sổ đo. |
| R15 | Lịch sử sửa chỉ số trước/sau/người/thời gian — §6/P198 | Chưa đáp ứng E08 | Lịch sử bền vững, lý do bắt buộc và ràng buộc khóa dữ liệu đã dùng. |
| R16 | Ngưỡng theo khu hoặc phòng — §6/P199 | Chỉ khu | Phòng ghi đè ngưỡng khu; lưu ngưỡng/phương pháp đã áp dụng tại lần đánh giá. |
| R17 | Internet/rác/dịch vụ/gửi xe; phí đầu người và tỷ lệ ngày — §6/P187–201 | Có ngày/ngày-người; thiếu đăng ký và số lượng xe | Giữ cách tính theo ngày; đăng ký tùy chọn là bổ sung ở B04, không nhận vơ V1 đã mô tả đủ. |
| R18 | Hóa đơn hợp đồng/kỳ, chặn trùng, giữ nguyên khi phát hành, chi tiết công thức — §7/P205–215; §8/P233–245 | Có nền tảng | Giữ; bổ sung căn cứ thời gian/kiểu tính cho các loại dòng mới. |
| R19 | PDF hóa đơn, nhắc nợ, phương thức thanh toán — §7/P218–221 | PDF/phương thức có; nhắc nợ thiếu | Bổ sung nhắc nợ in-app và chống lặp. |
| R20 | Phí chậm trả tùy chọn bật/tắt theo khu — §7/P220 | Chưa có trong mô hình hiện hành | Chờ D15; nếu để sau phải ghi nhận giảm phạm vi V1. |
| R21 | Lịch sử chỉnh hóa đơn trước phát hành — §7/P222 | Xóa/lập lại nháp, chưa có lịch sử đủ nội dung | Lưu trước/sau hoặc sự kiện xóa/lập lại có cùng khóa nghiệp vụ hợp đồng/kỳ. |
| R22 | Quy định kỳ, ngày chốt, ngày phát hành thống nhất theo khu — §7/P223 | Tháng lịch, có ngày cấu hình | Chốt D12; không nói ngày ghi trong tháng tự chứng minh đã đo đủ tháng. |
| R23 | In-app, loại thông báo, người nhận, đã đọc/thời gian — §7.1/P225–229 | 410 | Sự kiện thông báo + người nhận; email thông báo được V1 để giai đoạn sau. |
| R24 | Cảnh báo thống kê đơn giản, kiểm tra trước phát hành — §9/P247–260 | Có; chưa phù hợp kỳ lẻ E07 | Chuẩn hóa theo thời lượng, lưu căn cứ; PENDING chặn phát hành. |
| R25 | Thanh toán thủ công nhiều lần, công nợ và trạng thái — §10/P262–276 | Có nền tảng và khóa chống vượt nợ | Giữ; mở theo dõi bồi thường có phê duyệt, phân biệt thu mới/cấn cọc. |
| R26 | Báo hỏng, ảnh, giao người xử lý, tiến độ, ưu tiên/hạn dự kiến, chi phí — §11/P278–304 | Có phần lớn cho hợp đồng; thiếu phân loại sự cố | Giữ, bổ sung vị trí độc lập và loại sự cố; không đồng nhất chi phí với bồi thường. |
| R27 | Bảo trì định kỳ thiết bị chung — §11/P305 | 410 | Thiết bị chung/điểm bảo trì, lịch và từng lần thực hiện. |
| R28 | Dashboard phòng/hợp đồng/nợ/thu/sự cố — §12/P309–321 | Có nền tảng | Bổ sung nguồn tài chính mới; cọc không tính doanh thu, cấn không tính thu mới. |
| R29 | Báo cáo tháng/quý, nợ chi tiết, lấp đầy theo thời gian, xuất file, sự cố theo loại/thời gian — §12/P324–328 | Có nhiều API; thiếu loại sự cố, định nghĩa lấp đầy cần rõ | Chọn chỉ số có định nghĩa; dùng PDF hoặc CSV cho Excel, không tuyên bố XLSX. |
| R30 | Lịch sử thao tác/AOP, transaction và kiểm thử — §14/P406–412; §16/P499–503 | Có transaction/log/test cũ; thiếu audit nghiệp vụ đầy đủ | Lưu sự kiện quan trọng; thiết kế tiêu chí nghiệm thu mới tại mục 9. |

### 3.2. Bổ sung để giải quyết tình huống người dùng đã hỏi

| Mã | Nguồn | Bổ sung đề xuất và lý do |
| --- | --- | --- |
| B01 | YC: đồ hỏng, bàn giao | Biên bản nhận/trả và chi tiết tình trạng/số lượng, chứng cứ và xác nhận. V1 có tiện nghi nhưng chưa đặc tả quản lý tài sản/bàn giao từng đồ. |
| B02 | YC: cọc thiếu, chưa hoàn, thiệt hại vượt cọc; V1 có luồng nhận/quyết toán cọc | Lịch sử tiền cọc thực nhận/hoàn, số dư, khoản bồi thường chi tiết và các lần thu/cấn. |
| B03 | YC: phòng trống, đồ chung; V1 §11/P305 | Vị trí yêu cầu không phụ thuộc việc có hợp đồng; trách nhiệm trả tiền xác định riêng. |
| B04 | YC: dịch vụ tự chọn, gửi xe, thay người giữa tháng | Đăng ký/ngừng/số lượng theo hiệu lực. V1 liệt kê gửi xe nhưng không quy định đủ cách đăng ký và công thức xe-ngày. |
| B05 | YC: đổi đại diện, khách giữa tháng | Khi thực sự thay bên chịu trách nhiệm: trách nhiệm trước/sau được xác nhận, cùng mốc bàn giao khi có đổi cư trú, không chuyển nợ/cọc ngầm. Chỉ đổi liên hệ không bắt lập hợp đồng mới. |
| B06 | YC: kỳ lẻ, chuỗi chỉ số, thay công tơ | Khoảng đo và mốc dùng chung, cảnh báo theo thời lượng; thay/reset công tơ là giới hạn phải chốt, không mặc định đã hỗ trợ. |
| B07 | Đề xuất từ E12 và nguyên tắc bất biến | Phiên bản hợp đồng đã ký và quy trình điều chỉnh chứng từ đã phát hành. Phần điều chỉnh sau phát hành không được V1 mô tả rõ, chờ D13. |
| B08 | YC rà thực tế; quy trình thu tiền công khai tại mục 11 | Phiếu thu, phân bổ, tiền trả trước/thừa/chưa rõ, thu gộp và hoàn đúng nguồn. |
| B09 | YC rà thực tế; quy trình báo trả tại mục 11 | Thông báo trả, sửa/hủy thông báo, ngày bàn giao khác ngày hết tính tiền, ở quá hạn và lịch khách kế tiếp. |
| B10 | Phân tích vai trò thực tế, cần chủ trọ xác nhận | Tách bên ký/chịu trách nhiệm, người liên hệ, tài khoản và cư trú thực tế; không mặc định người ký luôn ở. |
| B11 | YC rà thực tế; hướng dẫn gia hạn tại mục 11 | Gia hạn kèm giá/cọc mới có hiệu lực, không sửa lịch sử và không bắt hợp đồng mới chỉ vì đổi giá. |
| B12 | YC rà thực tế; hướng dẫn chuyển phòng tại mục 11 | Chuyển phòng, chốt hai nơi, chuyển cọc có căn cứ/phê duyệt, không tự chuyển nợ hoặc tạo dòng tiền mới. |
| B13 | Rà soát tình huống vận hành | Thay công tơ, thiếu/ước tính số đo có đánh dấu, thu phòng trước điện nước, giữ chỗ/hủy, miễn giảm, tranh chấp và sửa khẩn. Các mức hỗ trợ cần chốt D11–D24. |

### 3.3. Phần để sau có căn cứ

V1 §18/P543–555 đã ghi không bắt buộc bản đầu: QR/ngân hàng thật, AI đọc công tơ, IoT, lịch máy giặt, hộ chiếu căn phòng, push notification và mobile. Email cho thông báo được V1 §7.1/P228 để giai đoạn sau; điều này không tự loại yêu cầu khôi phục mật khẩu bằng mã ở §5.1.

Mua sắm/khấu hao tài sản, quản lý serial mọi đồ vật, tự động chuyển nghĩa vụ tài chính, nhiều instance backend, cho thuê từng giường và chia công tơ tổng nhiều phòng vẫn là giới hạn cần chốt. Sau rà thực tế, **thay công tơ thủ công, chuyển cọc có chứng từ, phiếu thu/phân bổ và gia hạn đổi giá được đề nghị đưa vào phạm vi**, không còn xếp mặc định để sau. Phí chậm trả thuộc V1 nhưng được đề nghị lùi ở D15; không xếp chung thành “V1 vốn không yêu cầu”.

## 4. Tác nhân và giới hạn quyền đề xuất

Chỉ có ba vai trò đăng nhập: ADMIN, STAFF, TENANT. Khách chưa có tài khoản và người ở cùng chưa được cấp tài khoản là đối tượng nghiệp vụ; bộ lập lịch là tiến trình hệ thống. Không tạo thêm vai trò chỉ vì có thêm loại dữ liệu.

| Hoạt động | ADMIN | STAFF | TENANT có quyền hợp đồng/tài chính | Người thực ở theo D02 |
| --- | --- | --- | --- | --- |
| Quản lý tài khoản/phân quyền/phân công | Toàn bộ | Không | Đổi mật khẩu của mình | Chưa có tài khoản mặc định |
| Quản lý khu/danh mục/giá/ngưỡng | Toàn bộ | Xem trong phạm vi; sửa tình trạng phòng/ghi chú được giao | Xem thông tin phòng được phép | Hồ sơ cư trú |
| Tiếp nhận người thuê | Tạo/sửa/ghép trùng có kiểm soát | Tiếp nhận theo D03, không tra cứu tự do cả hệ thống | Xem hồ sơ mình; đề nghị sửa | Quản lý ghi nhận giúp |
| Lập hợp đồng/cư trú/bàn giao | Toàn bộ | Trong phòng/khu được giao | Bên được ủy quyền xem/xác nhận phần được cấp | Người ở có tài khoản được xem/xác nhận phần của mình |
| Ghi chỉ số/xử lý cảnh báo/lập và phát hành hóa đơn | Toàn bộ | Trong phạm vi | Xem bản phát hành và căn cứ; không tự xác nhận chỉ số/tiền | Không mặc nhiên xem tài chính |
| Nhận tiền cọc/thu hóa đơn | Toàn bộ | Trong phạm vi; có chứng từ | Xem của mình; không tự ghi “đã thanh toán” | Có thể là người nộp hộ, không vì vậy có quyền đọc dữ liệu |
| Bồi thường/cấn cọc/hoàn/điều chỉnh | Duyệt và thực hiện | Đề nghị hoặc thực hiện trong hạn mức/ủy quyền D14 | Xem căn cứ/trạng thái theo quyền tài chính | Không mặc nhiên có quyền tài chính/quản lý |
| Báo/xử lý sự cố | Báo, giao và xử lý mọi vị trí | Báo và xử lý vị trí được giao | Báo theo quyền cư trú/được ủy quyền | Có tài khoản và cư trú hợp lệ thì báo/theo dõi yêu cầu của mình |
| Thông báo | Gửi theo khu/đối tượng | Nhận, xử lý công việc theo phạm vi | Nhận và đánh dấu đã đọc của mình | Không mặc định là người nhận |
| Báo cáo/audit/vi phạm | Toàn bộ | Báo cáo và thao tác vận hành trong phạm vi; không đọc audit toàn hệ thống | Hợp đồng/chứng từ/sự cố của mình | Không đọc thông tin người khác |

Quyền STAFF là hợp của **các khu được giao** và **các phòng được giao riêng**; giao một phòng không cấp quyền toàn khu. Sự cố thiết bị chung chỉ thuộc người được giao cả khu hoặc người được giao tác vụ đó theo chính sách đã duyệt. Thu hồi phân công phải có hiệu lực ở backend cho danh sách, chi tiết, tải ảnh/PDF, báo cáo và thao tác ghi.

Luồng D03 cần sửa đúng chỗ vướng E10: nhân viên chọn phòng được giao, nhập hồ sơ và hợp đồng nháp trong cùng transaction. Với CCCD đã tồn tại nhưng ngoài phạm vi, trả thông báo cần ADMIN xác minh/ghép hồ sơ, không trả dữ liệu riêng của người đó. Nháp gắn phòng tạo căn cứ phạm vi; không cấp STAFF quyền sở hữu một hồ sơ toàn cục chỉ vì từng nhập nó. Sau khi hủy nháp, việc lưu/tra cứu hồ sơ chưa thuê theo chính sách của ADMIN.

Bên có trách nhiệm/quyền tài chính ở hợp đồng cũ tiếp tục xem chứng từ cũ để trả nợ/nhận hoàn. Người chỉ là liên hệ không tự nhận quyền đó sau khi bị thu hồi. Người đã rời đi vẫn theo dõi yêu cầu do mình gửi/căn cứ quyết toán được phép, không thấy sự cố hoặc người ở mới của hợp đồng kế tiếp. Khóa tài khoản nhân viên/người thuê không làm mất lịch sử tiền đã ghi. Ngay cả ADMIN cũng không được bỏ qua bất biến/chống ghi trùng; sửa sai phải có lịch sử.

## 5. Danh mục Use Case và đối chiếu dữ liệu

Mã `PA` dùng riêng cho phương án này, không ghi đè UC01–UC35 trong slide cũ. **A** = ADMIN; **S** = STAFF trong phạm vi/ủy quyền; **T** = TENANT với quyền tương ứng vai trò bên thuê/liên hệ/cư trú, không mặc nhiên chỉ là đại diện; **HT** = tác vụ hệ thống. Các tên dữ liệu ghi “mới” là nhóm ứng viên tại mục 7. Mỗi lỗi phải trả lý do và không để dữ liệu dở dang.

| Mã / Use Case | Ai thao tác | Dữ liệu hiện có | Cần thêm hoặc quy tắc ngoại lệ/lịch sử | Nguồn |
| --- | --- | --- | --- | --- |
| PA01 — Đăng nhập hệ thống | A/S/T | users; SessionRegistry | Kiểm tra khóa/hết hạn, không trả thông tin nhạy cảm; giới hạn một instance | R01 |
| PA02 — Đăng xuất phiên | A/S/T | SessionRegistry | Thu hồi phiên của mình; refresh hết hạn/dùng lại bị từ chối | R01 |
| PA03 — Đổi mật khẩu | A/S/T | users | Xác minh mật khẩu cũ, thu hồi phiên; audit không chứa mật khẩu/hash | R01 |
| PA04 — Khôi phục mật khẩu | A xác minh, người dùng nhập mã | users | Mã khôi phục mới; một lần, hết hạn/giới hạn thử; thu hồi phiên sau reset | R02 |
| PA05 — Quản lý tài khoản | A | users, tenant_profiles | Tạo/khóa/mở; liên kết TENANT đúng hồ sơ, không xóa người đã có chứng từ | R03 |
| PA06 — Phân công nhân viên | A | employee_assignments | Phân công phòng và hiệu lực/thu hồi mới; giao một phòng không mở cả khu | R03 |
| PA07 — Tra cứu nhật ký nghiệp vụ | A, S hạn chế | Audit log ứng dụng | Audit bền vững mới, bộ lọc theo đối tượng/quyền; lưu cả sự kiện sửa/xóa quan trọng | R30 |
| PA08 — Quản lý khu nhà | A | properties | Ngừng khai thác không xóa chứng từ; lịch sử thay đổi chính sách | R04 |
| PA09 — Quản lý phòng và tầng | A, S sửa phần được giao | rooms.floor_number | Mã phòng duy nhất/khu; không hạ sức chứa dưới số cư trú đỉnh hợp lệ | R04, D16 |
| PA10 — Chuyển trạng thái khai thác | A/S | rooms, contracts | Có thuê/giữ chỗ thì không chuyển bảo trì tùy tiện; giữ lịch sử cho báo cáo | R04, R29 |
| PA11 — Cập nhật tiện nghi | A/S | rooms.description | Danh mục/liên kết số lượng mới; không sửa ngược biên bản bàn giao đã xác nhận | R05, B01 |
| PA12 — Quản lý ảnh phòng | A/S, T xem | Chưa có | Ảnh phòng mới; quyền tải ảnh, thứ tự; ảnh bị gỡ khỏi tin phòng không làm mất chứng cứ đã chốt | R06 |
| PA13 — Cập nhật ghi chú nội bộ | A/S | rooms.internal_note | Thao tác cập nhật và audit; T không thấy ở API, PDF hoặc export | R07 |
| PA14 — Thiết lập giá niêm yết | A | rooms.base_price | Lịch sử giá mới; không sửa giá hợp đồng đã ký; chặn hiệu lực trùng | R08 |
| PA15 — Tiếp nhận người thuê | A/S theo D03 | tenant_profiles | Ngữ cảnh phòng/hợp đồng nháp; CCCD trùng cần xác minh, không lộ hồ sơ ngoài quyền | R09 |
| PA16 — Cập nhật hồ sơ người thuê | A, S theo luồng được phép | tenant_profiles | Audit và kiểm tra trùng; thay thông tin hiện tại không sửa bản hợp đồng đã ký | R09 |
| PA17 — Tra cứu lịch sử cư trú | A/S/T đúng quyền | contracts, contract_members, tenant_profiles | Hiển thị vai trò/thời gian; không gộp hồ sơ chỉ dựa trên trùng tên/điện thoại | R09 |
| PA18 — Lập hợp đồng thuê | A/S | contracts, rooms, tenant_profiles | Trạng thái nháp/ký; kiểm tra thời gian, quyền, sức chứa, phòng bảo trì; bản ký bất biến | R10, R12 |
| PA19 — Ghi nhận cư trú | A/S | contract_members và đại diện đang được tính ngầm | Ghi từng người thực ở, kể cả người ký nếu có ở; nháp bị ảnh hưởng lập lại, ISSUED dùng điều chỉnh; ký hợp đồng không tự cộng một người | R09, R17, B10 |
| PA20 — Xuất hợp đồng PDF | A/S/T | contracts và PdfService | Chọn bản đã ký/phụ lục, không dùng thông tin hiện tại để tái tạo sai bản cũ | R12 |
| PA21 — Gia hạn hợp đồng | A/S theo ủy quyền | contracts | Phụ lục/điều kiện giá/cọc có hiệu lực; không hồi tố; không tự đóng hợp đồng vì thay giá | R10, B11 |
| PA22 — Cập nhật liên hệ hoặc bên thuê | A/S, các bên xác nhận phần trách nhiệm | contracts, contract_members | Phân biệt đổi liên hệ với đổi bên chịu trách nhiệm theo D08; giữ vai trò/lịch sử nợ | B05, B10 |
| PA23 — Xử lý vi phạm hợp đồng | A/S, T xem phần liên quan | Chưa có hiện hành | Hồ sơ vi phạm và sự kiện mới; lý do, trạng thái; không tự sinh tiền phạt | R13 |
| PA24 — Lập biên bản bàn giao | A/S, T xác nhận | Chưa có | Biên bản/chi tiết mới; tình trạng, số lượng, mốc chỉ số và chứng cứ; thiếu xác nhận ghi rõ | B01 |
| PA25 — Quản lý danh mục dịch vụ | A | service_items | Phân biệt đơn vị và cơ sở tính; dịch vụ đã dùng chỉ ngừng áp dụng, không đổi ý nghĩa lịch sử | R17 |
| PA26 — Thiết lập giá dịch vụ | A | service_price_configs | Hiệu lực theo khu/dịch vụ; không chèn giá hồi tố làm thay bản phát hành | R14 |
| PA27 — Đăng ký dịch vụ sử dụng | A/S, T đề nghị | Chưa có | Đăng ký/số lượng theo thời gian mới; không cho hai khoảng cùng dịch vụ chồng nhau vô ý | B04 |
| PA28 — Ghi chỉ số công tơ | A/S | meter_readings | Công tơ/điểm ghi/khoảng đo mới; nhỏ hơn mốc trước hoặc sai công tơ thì từ chối | R14, B06 |
| PA29 — Sửa chỉ số chưa khóa | A/S | meter_readings | Lịch sử trước/sau, lý do/người/thời gian; đã dùng thì chặn hoặc qua điều chỉnh, không UPDATE trực tiếp | R15 |
| PA30 — Xử lý cảnh báo tiêu thụ | A/S | consumption_alerts | Căn cứ khoảng đo, baseline/thời lượng mới; sửa chỉ số phải đánh giá lại; PENDING chặn phát hành | R24 |
| PA31 — Cấu hình ngưỡng cảnh báo | A | properties | Ghi đè theo phòng mới; cửa sổ/mức áp dụng và thời điểm; không đổi kết quả lịch sử | R16 |
| PA32 — Lập hóa đơn nháp | A/S | invoices, invoice_items | Lấy dịch vụ đã đăng ký, kiểu tính và khoảng đo đúng; chặn trùng hợp đồng/kỳ | R18 |
| PA33 — Lập lại hóa đơn nháp | A/S | invoices, invoice_items | Audit trước khi xóa, lý do, khóa nghiệp vụ để truy lịch sử; không mất dấu nháp cũ | R21 |
| PA34 — Phát hành hóa đơn | A/S | invoices, invoice_items, consumption_alerts | Đủ căn cứ của các khoản đang phát hành/đúng phiên bản; PENDING cần xử lý, thiếu baseline không cấm thu; khóa chứng từ/nguồn cùng transaction | R18, R24, D24 |
| PA35 — Xem hóa đơn minh bạch | A/S/T | Các view hóa đơn | Hiện cửa sổ đo, ngày/ngày-người/xe-ngày, giá và phép tính; T không thấy DRAFT | R18 |
| PA36 — Xuất hóa đơn PDF | A/S/T | invoices và PdfService | Xuất đúng bản phát hành và quyền; điều chỉnh phải hiện riêng, không thay PDF gốc | R19 |
| PA37 — Điều chỉnh chứng từ phát hành | A/S theo ủy quyền | Chưa có | Chứng từ điều chỉnh theo D13; lý do, liên kết gốc, tác động nợ/hoàn; phát sinh mới có căn cứ riêng theo D24, không thu lặp | B07, D24 |
| PA38 — Ghi nhận tiền cọc | A/S | contracts.deposit_amount chỉ là thỏa thuận | Giao dịch cọc thực nhận mới; tiền dương, mã duy nhất; thiếu cọc cần duyệt nhận phòng | R10, B02 |
| PA39 — Phân bổ tiền thanh toán | A/S | payment_transactions | Theo phiếu thu/số dư nguồn và nợ đích; phân bổ không vượt nguồn/nợ, không bỏ tiền thực nhận vượt nợ | R25, B08 |
| PA40 — Tra cứu công nợ và cọc | A/S/T | v_invoice_balances | View hợp nhất nợ hóa đơn/bồi thường/điều chỉnh và sổ cọc; không trộn cọc với doanh thu | R25, B02 |
| PA41 — Chốt trả phòng | A/S theo ủy quyền | contracts, checkout_records | Biên bản trả, ngày hết tính tiền theo căn cứ, kết thúc cư trú/hoàn tất tài chính riêng; thiếu căn cứ giữ nháp | R10, B01–B02, B09 |
| PA42 — Cấn tiền cọc | A/S được quyền, các bên xác nhận khi cần | DEPOSIT_OFFSET | Dựa số dư cọc khả dụng và khoản nợ đủ điều kiện; khóa nguồn/đích, không tạo tiền mới thu | B02 |
| PA43 — Hoàn tiền cọc | A/S được ủy quyền | Chưa có chứng từ thực hoàn | Giao dịch chi thật, người nhận hợp lệ/phương thức/mã/thời điểm; không vượt phần được hoàn | B02 |
| PA44 — Duyệt khoản bồi thường | A/S theo hạn mức | checkout_records.damage_amount | Phân biệt hao mòn/lỗi và khoản đang tranh chấp; số được duyệt, căn cứ, điều kiện cấn và nợ còn lại | B01–B02, D14/D23 |
| PA45 — Gửi yêu cầu sửa chữa | A/S/T | maintenance_requests, maintenance_images | Đích hợp đồng hoặc phòng hoặc thiết bị; T chỉ báo trong phạm vi; không cần dựng hợp đồng giả | R26, B03 |
| PA46 — Xử lý yêu cầu sửa chữa | A/S | maintenance_requests | Loại sự cố, giao người, lịch sử trạng thái/chi phí; chi phí sửa không tự tăng công nợ người thuê | R26 |
| PA47 — Thực hiện bảo trì định kỳ | A/S/HT | Chưa có hiện hành | Thiết bị chung/lịch/lần thực hiện mới; tránh sinh hai lần công việc một kỳ | R27 |
| PA48 — Gửi thông báo nghiệp vụ | A/S theo quyền, HT | Chưa có hiện hành | Thông báo/người nhận mới; loại, thời gian, khóa chống nhắc trùng; chỉ gửi sau commit nghiệp vụ | R11, R19, R23 |
| PA49 — Đọc thông báo cá nhân | A/S/T | Chưa có hiện hành | `read_at` theo người nhận; một người đọc không làm cả khu thành đã đọc | R23 |
| PA50 — Xem và xuất báo cáo | A/S | Các view và ReportController | Nợ bồi thường/cọc, loại sự cố; định nghĩa kỳ tiền/ngày tiền, lấp đầy; export cùng phạm vi | R28–R29 |
| PA51 — Gửi thông báo trả phòng | T theo quyền, A/S nhập giúp | Chưa có | Ngày báo/dự kiến/lý do; không giải phóng phòng hoặc kết thúc tính tiền ngay | B09 |
| PA52 — Thay đổi kế hoạch trả phòng | T đề nghị, A/S xác nhận | Chưa có | Lưu sửa/hủy kế hoạch, kiểm tra khách kế tiếp; không xóa lịch sử thông báo | B09 |
| PA53 — Chuyển phòng thuê | A/S, các bên xác nhận | Chưa có luồng hoàn chỉnh | Chốt phòng cũ/nhận phòng mới, tiền/công tơ/giá từng nơi; chuyển cọc có chứng từ riêng | B12 |
| PA54 — Ghi nhận phiếu thu | A/S | Chỉ có giao dịch gắn hóa đơn | Tiền thực nhận, người nộp, ngày nhận/ngày ghi, tài khoản nhận, tham chiếu; có thể chưa rõ đích | B08 |
| PA55 — Phân bổ tiền trả trước/thừa | A/S được giao | Chưa có | Số dư tách cọc; một phiếu thu nhiều phân bổ, không vượt nguồn và không tự chuyển hợp đồng | B08 |
| PA56 — Hoàn tiền chưa sử dụng | A/S theo ủy quyền | Chưa có đầy đủ | Hoàn tiền trả trước/thừa khác hoàn cọc; người nhận hợp lệ, nguồn rõ; tiền chưa chi vẫn chờ hoàn | B08 |
| PA57 — Thay công tơ | A/S có quyền | Chưa có | Ngày thay, điểm chốt cũ/mở mới, ảnh/lý do; thiếu điểm thì trạng thái cần xác minh | B06, B13 |
| PA58 — Xử lý cư trú quá hạn | A/S | end_date/actual_checkout_date hiện tại chưa đủ | Theo dõi còn chiếm phòng, giá/phê duyệt và xung đột lịch; không tự trống/tự gia hạn pháp lý | B09 |
| PA59 — Quản lý giữ chỗ | A/S, T đề nghị | Chưa có | Nếu D22 giữ: thời hạn, tiền/điều kiện hủy/hoàn/chuyển sang cọc được xác nhận | B13 |
| PA60 — Ghi nhận miễn giảm | A/S theo hạn mức | Chưa có | Căn cứ thỏa thuận, khoản/kỳ, số tiền, người duyệt; sau phát hành là chứng từ giảm, không sửa gốc | B13 |

## 6. Quy tắc và các luồng nghiệp vụ

### 6.1. Những khái niệm phải tách

- **Hết hạn thỏa thuận**, **ngừng ở thực tế**, **bàn giao lại quyền sử dụng phòng**, **hết nghĩa vụ tính tiền**, **hết nợ** và **đã hoàn tiền** là các mốc khác nhau. Khách chuyển đồ đi chưa giao chìa khóa, trả sớm có thỏa thuận báo trước, hoặc còn chiếm phòng sau hạn cần được ghi riêng; phần mềm không tự quyết định nghĩa vụ pháp lý.
- **Giá niêm yết phòng** phục vụ chào thuê; **giá hợp đồng** là thỏa thuận đã ký; **giá snapshot trên dòng hóa đơn** là giá đã áp dụng. Đổi giá niêm yết không sửa hai loại sau.
- **Tiện nghi** cho biết phòng có gì; **tài sản/hạng mục bàn giao** cho biết số lượng và tình trạng giao nhận; **chi phí sửa** là chi phí vận hành; **bồi thường** là khoản đã xác định người thuê phải chịu. Không suy ra bồi thường chỉ từ hóa đơn thợ sửa.
- **Cọc cam kết** khác **cọc thực nhận**, **cọc còn giữ**, **cọc cần nhận thêm**, **số được hoàn** và **số đã hoàn**. Sau khi kết thúc hợp đồng, cọc chưa nộp đủ không tự biến thành khoản nợ mới nếu nghĩa vụ cọc đã chấm dứt; phải thu bồi thường/nợ thuê theo chứng từ thật.
- Khoảng ngày áp dụng được biểu diễn **[ngày bắt đầu, ngày kết thúc không tính)**. Cư trú, giữ quyền thuê, nghĩa vụ tiền phòng và từng dịch vụ có thể khác khoảng. Quy ước “ngày ra không tính tiền” là chính sách mặc định của đồ án cần xác nhận, không khẳng định là quy tắc chung cho mọi hợp đồng. Phải phân biệt “thuê đến hết ngày 30/09” với ngày kết thúc không tính 01/10 để không mất một ngày tiền.
- Hợp đồng mới không nhận trách nhiệm tài chính cũ chỉ vì cùng phòng hoặc cùng một vài thành viên.

### 6.2. Luồng L1 — Tiếp nhận → hợp đồng → nhận cọc → bàn giao

**Tác nhân:** A/S; T xác nhận phần được phép. **Tiền điều kiện:** phòng trong phạm vi, đang khai thác, khoảng thuê còn trống; hồ sơ được xác minh tối thiểu. **Dữ liệu:** hồ sơ, hợp đồng, thành viên, giao dịch cọc, biên bản và mốc chỉ số.

1. Chọn phòng và khoảng thuê, kiểm tra quyền/khai thác/sức chứa/chồng lịch. STAFF tiếp nhận khách theo D03.
2. Lập hợp đồng nháp: bên ký/chịu trách nhiệm, người liên hệ, những người thực ở, khoảng thuê/cư trú, giá, cọc cam kết, lịch thu và dịch vụ. Nếu nhận tiền trước lúc ký thì ghi phiếu thu/giữ chỗ, không tạo hợp đồng giả để chứa tiền.
3. Xác nhận/ký hợp đồng; giữ phiên bản đã ký, không chỉ giữ một PDF tái sinh từ dữ liệu có thể sửa.
4. Khi thực nhận tiền, ghi phiếu thu gốc rồi phân bổ phần cọc/tiền thuê/trả trước theo xác nhận. Một lần chuyển gồm nhiều khoản chỉ là một lần thu tiền thật. Cọc nhận từng phần là tổng các phân bổ vào quỹ cọc, không cộng từ số cam kết.
5. Lập biên bản nhận phòng: hạng mục/số lượng/tình trạng, ảnh cần thiết, điểm chỉ số đầu công tơ, người giao/nhận/xác nhận.
6. Chỉ xác nhận nhận phòng khi đủ điều kiện; thiếu cọc theo D05 cần phê duyệt rõ. Kích hoạt cư trú và lưu dấu ai đã cho phép.

Đề xuất nháp tiếp nhận chưa tự giữ chỗ vô thời hạn. Bước xác nhận/ký phải khóa và kiểm tra lại khoảng thuê; chỉ một hợp đồng được xác nhận cho cùng khoảng phòng. Nếu anh cần nghiệp vụ giữ chỗ riêng thì phải chốt thời hạn và trạng thái riêng, chưa được tính là đã có trong phương án cơ sở.

**Nhánh lỗi:** hồ sơ trùng chưa xác minh → dừng ghép và chuyển A; ngoài quyền/phòng bảo trì → từ chối; hai người cùng chốt phòng → chỉ một khoảng được giữ thành công; giao dịch cọc trùng mã → không cộng hai lần; chưa ký/thiếu bàn giao → giữ chờ, không báo “đã bàn giao”. Khách không ký biên bản: lưu chưa xác nhận, ảnh/người chứng kiến và cách xử lý được duyệt; không tự tạo chữ ký. Hủy sau nhận tiền phải có quyết định hoàn/khấu trừ theo thỏa thuận và giao dịch thật; tiền giữ chỗ khác cọc bảo đảm, không tự mặc định mất cọc.

**Hậu điều kiện:** xác định được ai ở, từ khi nào, thỏa thuận gì, đã nhận bao nhiêu cọc và đã giao tài sản/chỉ số nào. Ký hợp đồng và nhận cọc có thể là hai lần làm việc; không giữ DB transaction mở để đợi khách chuyển tiền.

### 6.3. Luồng L2 — Ghi chỉ số → cảnh báo → hóa đơn → thu tiền

**Tác nhân:** A/S ghi và xử lý; T xem, thanh toán ngoài hệ thống; A/S xác nhận đã nhận. **Tiền điều kiện:** kỳ có ngày cư trú, giá và dữ liệu áp dụng đủ. **Dữ liệu:** công tơ/điểm ghi/khoảng tiêu thụ, đăng ký dịch vụ, cảnh báo, hóa đơn/dòng/giao dịch.

1. Ghi điểm chỉ số có thời gian thực đo. Hệ thống xác định điểm trước trên cùng công tơ, mốc bàn giao và hợp đồng chịu khoảng sử dụng; không để nhập lại một chỉ số đầu độc lập cho khách kế tiếp.
2. Kiểm tra mới ≥ cũ, chuỗi liên tục, đúng công tơ và ngày. Nếu đang đổi khách, chia trách nhiệm tại mốc chung; điện/nước lúc phòng trống thuộc chủ nhà, không dồn sang khách mới.
3. Đánh giá cảnh báo theo khoảng có độ dài hợp lệ và nhóm cư trú so sánh được. Chưa đủ lịch sử ghi “chưa đủ dữ liệu so sánh”, không chặn phát hành chỉ vì khách/phòng mới; có tăng vượt ngưỡng thì tạo PENDING kèm căn cứ. Thiếu baseline khác thiếu chỉ số để tính tiền.
4. A/S kiểm tra: xác nhận lượng dùng thật hoặc sửa sai chỉ số chưa khóa có lý do. Mỗi lần sửa lưu trước/sau và đánh giá lại. Đã có nháp phụ thuộc thì lưu lịch sử và xóa/lập lại nháp trước khi sửa.
5. Lập hóa đơn nháp theo lịch thu: có thể thu tiền phòng tháng T trước, kèm điện/nước của cửa sổ đã chốt T−1. Mỗi dòng lưu khoảng dịch vụ riêng và giá đã thỏa thuận/phiên bản hiệu lực, không ép điện/nước và tiền phòng cùng tháng đo. Tiền phòng theo quyền thuê/nghĩa vụ đã xác nhận, dịch vụ theo cơ sở tính từng loại; thiếu giá hoặc chỉ số phải hiện thiếu/chờ bổ sung, không dùng 0 giả.
6. Phát hành sau khi kiểm tra đủ căn cứ, lịch phát hành và không còn PENDING. Khóa bản phát hành cùng các tham chiếu/căn cứ cần bảo toàn; không cho việc đổi danh mục làm đổi công thức cũ.
7. Người được cấp quyền xem chi tiết/PDF/nợ. A/S ghi đúng số tiền thực nhận vào phiếu thu, sau đó phân bổ không vượt số dư nguồn/nợ đích. Tiền trả thừa/trước vẫn còn số dư riêng; ảnh chuyển khoản chỉ là chứng cứ đề nghị, chưa phải xác nhận tiền về. Cấn cọc không là phiếu thu mới.
8. Sau commit, tạo thông báo hóa đơn mới/nhắc nợ theo mốc. Không báo đã phát hành nếu transaction bị rollback.

**Nhánh lỗi:** thiếu chỉ số/giá/căn cứ của khoản đang định thu → chưa phát hành khoản đó; chưa đến kỳ đo thì không phát minh chỉ số để đủ dòng; có PENDING cần xử lý → chưa phát hành phần liên quan; số sai sau ISSUED → qua L6; cùng tháng có tiền đã thu trước phải khấu trừ đúng nguồn. Không cư trú nhưng còn giữ quyền thuê không tự miễn tiền phòng. Thu trùng cùng giao dịch thật không ghi lần hai; hai khoản chuyển thật khác nhau đều ghi nhận, dù khoản sau thành tiền dư; tranh chấp phân bổ phải giữ nguyên tổng đã nhận.

**Hậu điều kiện:** truy ra được công thức, nguồn đo, người kiểm tra và từng lần giảm nợ. Nợ còn lại tính từ chứng từ/giao dịch có hiệu lực, không nhập tay một cột “còn nợ”.

### 6.4. Luồng L3 — Báo hỏng → sửa → xác định chi phí và bên chịu

**Tác nhân:** người ở có quyền T báo hỏng; A/S báo cả phòng trống/thiết bị chung; người được ủy quyền duyệt tài chính. **Dữ liệu:** vị trí/hạng mục, yêu cầu, ảnh, lịch sử xử lý, chi phí, khoản bồi thường nếu có.

1. Chọn đúng đối tượng: hợp đồng đang ở, phòng cụ thể hoặc thiết bị có vị trí xác định. Thiết bị chung và phòng trống không cần hợp đồng giả.
2. Ghi mô tả, loại/ưu tiên, ảnh, người báo và thời điểm; xác định hạn dự kiến. Khi phân công phải kiểm tra phạm vi của người xử lý tại vị trí đó.
3. Tiếp nhận → đang xử lý → hoàn thành. Lưu mỗi lần đổi trạng thái, người làm và ghi chú; không chỉ giữ ghi chú cuối.
4. Ghi chi phí thực tế có căn cứ; nhiều lần chi có thể tổng hợp từ chi tiết, không vừa nhập tổng vừa nhập các dòng độc lập.
5. Đối chiếu biên bản nhận/tình trạng trước đó. Phân biệt hao mòn, lỗi thiết bị, lỗi chủ nhà và lỗi người thuê; nếu chưa rõ thì chờ xác minh, chưa mặc nhiên ghi nợ/cấn. Người thuê tự ứng tiền sửa cần quy trình xác minh khoản phải hoàn/khấu trừ, không được ghi như người thuê đã trả tiền phòng.
6. Nếu người thuê phải chịu, lập từng khoản, giải thích căn cứ/số tiền và gắn đúng trách nhiệm; người có quyền duyệt trước khi khoản thành phải thu. Phê duyệt nội bộ không xóa trạng thái tranh chấp hoặc tự chứng minh được quyền khấu trừ cọc.
7. Thu/cấn khoản được duyệt qua cùng cơ chế chống vượt nợ; không tự thêm lại khoản đó trong quyết toán.

**Nhánh lỗi:** chưa rõ thiết bị nhưng đã rõ vị trí/nguy cơ → tiếp nhận và bổ sung sau; sự cố khẩn không đợi chốt bồi thường mới xử lý; giao sai phạm vi → từ chối hoặc giao người có quyền; báo lặp → liên kết nguồn, không nhân đôi chi phí; tranh chấp → theo dõi riêng; hao mòn → chi phí chủ nhà, bồi thường 0. Hoàn thành rồi tái phát được mở đợt xử lý mới/liên kết yêu cầu trước, không sửa mất lịch sử hoàn thành. Sửa chữa không mặc nhiên khóa cả phòng; chỉ khóa khai thác khi thực sự không thể bàn giao.

**Hậu điều kiện:** có thể hoàn thành sửa chữa dù chưa thống nhất bồi thường; trạng thái sửa chữa và trạng thái thu tiền được theo dõi riêng.

### 6.5. Luồng L4 — Trả phòng → chốt nợ → cấn cọc → hoàn/thu thêm

**Tác nhân:** A/S theo ủy quyền kiểm tra/lập/duyệt, bên có quyền T xác nhận/nhận hoặc nộp tiền. **Dữ liệu:** hợp đồng, biên bản trả, chỉ số, hóa đơn, bồi thường, quyết toán, sổ cọc/trả trước và giao dịch.

1. Nhận thông báo trả và xác nhận kế hoạch; đến lúc trả kiểm tra bàn giao/chìa khóa/quyền sử dụng thực tế. Ghi riêng ngày hết tính tiền theo thỏa thuận và ngày giải phóng phòng; không lấy ngày báo trả thay ngày bàn giao. Nếu chỉ một người rời thì đóng cư trú người đó, không kết thúc nguyên hợp đồng tự động.
2. So sánh từng hạng mục lúc nhận và lúc trả, xác định các khoản đề nghị bồi thường. Không tự xem mọi đồ hỏng là lỗi người thuê.
3. Chốt các khoảng điện/nước và khoản còn thiếu; kiểm tra tiền phòng đã thu trước. Tiền phòng phải giảm/hoàn do chấm dứt được xác nhận thì lập chứng từ giảm; điện/nước kỳ cuối chưa thu thì lập chứng từ phát sinh/quyết toán có nguồn, không yêu cầu kỳ đó chưa từng có hóa đơn. Một khoản sử dụng chỉ thu một lần.
4. Nếu thiếu căn cứ hoặc còn tranh chấp, giữ hồ sơ quyết toán DRAFT. Việc đã trả phòng có thể được ghi nhận riêng, không ép giữ phòng “đang thuê” chỉ vì còn nợ/đợi hoàn.
5. Trước khi duyệt, khóa hợp đồng và các khoản nợ liên quan, lấy **số dư cọc thực còn giữ**, nợ hóa đơn và khoản bồi thường đã duyệt. Chụp snapshot các số trước cấn và danh sách nguồn; không lấy `deposit_amount` thay tiền thật.
6. Duyệt quyết toán và phân bổ cấn trong một transaction ngắn: chỉ dùng cọc khả dụng cho khoản đủ điều kiện theo D07/thỏa thuận; khoản tranh chấp không tự đưa vào thứ tự khấu trừ. Mỗi đồng cọc chỉ giảm một khoản một lần. Thiếu cọc vẫn giữ nợ chưa trả đúng bên/hợp đồng chịu trách nhiệm.
7. Nếu còn tiền được hoàn, ghi nghĩa vụ hoàn/chờ thực hiện. Chỉ lúc thực chi mới lập giao dịch hoàn, có người nhận/phương thức/chứng từ; có thể hoàn từng phần.
8. Nếu còn phải thu, tiếp tục ghi các lần thu vào đúng khoản bồi thường/hóa đơn còn nợ. Hợp đồng đã kết thúc cư trú vẫn xuất hiện trong danh sách công nợ.

**Nhánh lỗi:** quyết toán đã duyệt → không ghi đè, có bổ sung thì chứng từ mới; gọi lại mã thao tác → không cấn lần hai; thiếu căn cứ hoặc tranh chấp → chưa chốt phần đó, phần không tranh chấp xử lý riêng theo phê duyệt; chưa biết chỉ số cuối → giữ nghĩa vụ chờ xác minh, không ghi đã tất toán; hóa đơn cũ sai → qua L6; tiền phòng đã thu trước → xác định phần được hưởng/phải hoàn theo căn cứ, không hoàn tự động mọi ngày vắng. Phòng chỉ trở lại trạng thái sẵn sàng sau bàn giao và kiểm tra khai thác, không phải cứ đã trả là cho khách mới vào ngay.

**Hậu điều kiện:** biết phòng đã được trả chưa, nợ còn bao nhiêu, cọc còn giữ bao nhiêu và tiền hoàn đã chi bao nhiêu. “Đã duyệt quyết toán” không đồng nghĩa “đã hoàn tất mọi giao dịch tiền”.

### 6.6. Luồng L5 — Gia hạn và đổi đại diện

**Gia hạn theo D09:** A/S kiểm tra quan hệ thuê, hạn mới, lịch phòng và thỏa thuận; lập phụ lục có điều kiện giá/cọc cũ/mới, ngày ký/ngày hiệu lực/người duyệt. Khóa, kiểm tra lại rồi lưu phụ lục/phiên bản áp dụng; không sửa giá của khoảng đã tính tiền. Cọc tăng tạo nghĩa vụ bổ sung, chưa phải đã nhận; cọc giảm cần quyết định giữ/chuyển/hoàn có chứng từ. Cư trú tiếp tục nếu thực sự còn ở; người đã rời không tự được mở lại.

**Nhánh lỗi gia hạn:** đã hủy/đã bàn giao trả → không gia hạn trực tiếp; có khách tương lai → cảnh báo và xử lý lịch, không tự đẩy khách đó đi; đã quá hạn nhưng còn chiếm phòng → qua PA58, không làm phòng trống hoặc tự hợp pháp hóa gia hạn; thay giá hồi tố của khoản đã phát hành → chứng từ điều chỉnh có căn cứ, không UPDATE giá cũ. Thay giá tương lai có phụ lục, không bắt ký hợp đồng kế tiếp chỉ vì DB khóa trường giá.

**Đổi liên hệ theo D08:** ghi kết thúc vai trò liên hệ cũ/mở vai trò mới từ D, cập nhật người nhận thông báo/quyền được cấp. Bên ký/chịu trách nhiệm, nợ, cọc và chỉ số không thay chỉ vì đổi liên hệ. Nếu người cũ rời thực tế thì đóng khoảng cư trú riêng.

**Thay bên thuê/chịu trách nhiệm:** xác nhận thỏa thuận giữa các bên và mốc trách nhiệm; phương án đơn giản là kết thúc phần thuê cũ/lập hợp đồng mới, cùng điểm bàn giao khi có đổi người chịu sử dụng. Nợ trước mốc ở bên cũ trừ khi có thỏa thuận chuyển nghĩa vụ được xác minh; cọc không tự chuyển. Không buộc người ở lại phải chuyển đồ ra rồi vào lại để lập biên bản giao trách nhiệm. Chỉ đổi liên hệ tuyệt đối không kích hoạt cả luồng quyết toán này.

**Nhánh lỗi:** chưa có thỏa thuận nhận trách nhiệm mới → không đổi bên chịu nợ; thiếu quyền của người đề nghị → chưa thực hiện; số người thực ở/sức chứa sai → sửa căn cứ cư trú; chỉ có người nộp tiền hộ → không coi họ thành bên thuê hoặc người có quyền nhận cọc. Vai trò trước/sau và quyền xem chứng từ lịch sử phải truy được.

### 6.7. Luồng L6 — Điều chỉnh sau phát hành, chỉ khi D13 được duyệt

Phát hiện sai → lập đề nghị chỉ rõ hóa đơn/dòng gốc, lý do, giá trị đúng/phần chênh → người có quyền kiểm tra/duyệt → sổ nợ phản ánh tăng/giảm. Bản gốc/dòng gốc giữ nguyên. Khoản giảm có thể giảm nợ mở hoặc phục hồi phần tiền đã dùng về đúng nguồn/phải hoàn; không ghi “thanh toán âm” để xóa dấu. Khoản mới phát sinh ở kỳ dịch vụ khác dùng chứng từ phát sinh/quyết toán có nguồn riêng, không giả là sửa một số đo cũ vốn đúng.

Mỗi chứng từ điều chỉnh có mã duy nhất, trạng thái và nguồn; cùng một đề nghị không được áp dụng hai lần. Không tự cấn khoản cần hoàn sang hợp đồng mới. Nếu D13 chưa được chấp thuận, thiết kế chỉ có thể phát hiện và dừng tình huống này; chưa thể tuyên bố đã hỗ trợ đầy đủ sửa sai sau phát hành.

### 6.8. Luồng L7 — Nhận tiền thật → xác minh → phân bổ → hoàn phần dư

Ghi nhận chứng cứ/đề nghị → xác minh tiền đã vào tiền mặt/tài khoản → xác nhận **một** phiếu thu đủ số thực nhận → chọn phân bổ vào các khoản được phép/cọc/trả trước → giữ phần chưa rõ/chưa dùng ở số dư riêng. Gửi lại cùng giao dịch thật không tạo phiếu mới; hai lần chuyển thật khác nhau vẫn ghi đủ hai lần. Phiếu thu có thể có ngày thực nhận khác ngày nhập, cả hai được giữ.

Ví dụ nhận 5.000.000, phân bổ 2.000.000 vào cọc, 2.700.000 vào nợ, còn 300.000 trả trước/chưa phân bổ. Tiền thu thật là 5.000.000, không thành 9.700.000 do cộng cả phiếu và phân bổ. Khách gửi ảnh nhưng chưa có tiền về thì trạng thái chờ xác minh, chưa giảm nợ. Chuyển thiếu nội dung thì chưa đoán người/đích; tiền có thật vẫn được ghi nhận để đối soát. Phần hoàn chỉ giảm số dư sau thực chi; người nhận được xác minh theo quyền/thỏa thuận, không tự gửi cho người nộp hộ.

### 6.9. Luồng L8 — Chuyển phòng đang thuê

Chọn phòng đích và mốc chuyển → kiểm tra quyền ở cả hai nơi/lịch khai thác → chốt chỉ số và trách nhiệm tài sản phòng cũ → lập biên bản nhận phòng mới/chỉ số mới → tạo hợp đồng kế tiếp hoặc văn bản thay phòng có lịch sử theo phương án đã chốt → xác nhận phân bổ nợ/cọc. Khuyến nghị đồ án dùng hợp đồng kế tiếp để dễ tách giá/chỉ số/công nợ hai phòng, nhưng liên kết hai hồ sơ là một lần chuyển chỗ; không tạo tiền thu/hoàn nếu chỉ chuyển số cọc được duyệt.

Hai phòng có khoảng tiền thuê riêng; ngày chuyển không tự được tính hai lần nếu không có thỏa thuận thuê đồng thời. Phòng đích không sẵn sàng thì chưa xác nhận chuyển. Sự cố cũ vẫn ở vị trí cũ; di chuyển một tài sản phải có sự kiện bàn giao/đổi vị trí, không sửa room_id làm sai quá khứ.

## 7. Phương án dữ liệu logic — chưa phải thiết kế vật lý

### 7.1. Cách xử lý 17 bảng hiện có

| Nhóm | Thành phần giữ và phần cần xem lại |
| --- | --- |
| Tài khoản | Giữ `users` và liên kết tùy chọn `tenant_profiles.user_id`; bổ sung mã khôi phục, phân công phòng và lịch sử phân công. Phiên trong bộ nhớ không tự đòi thêm bảng nếu vẫn chấp nhận một instance. |
| Khu/phòng | Giữ `properties`, `rooms`; thêm dữ liệu ảnh/tiện nghi/giá niêm yết có hiệu lực. `base_price` chỉ được giữ như giá hiện tại nếu có một cơ chế đồng bộ rõ; phương án ưu tiên lấy giá từ lịch sử giá, không cho sửa hai nguồn độc lập. |
| Cư trú | Giữ tách hồ sơ với tài khoản, nhưng xem lại việc đếm người ký ngầm là một người ở. Cần dữ liệu bên ký/chịu trách nhiệm, vai trò liên hệ có hiệu lực và mọi khoảng cư trú thực tế. Người ký chỉ được tính cư trú nếu có bản ghi thực ở. Bổ sung bản ký/phụ lục, bàn giao và vi phạm. |
| Dịch vụ | Giữ danh mục và giá khu theo thời gian; bổ sung đăng ký hợp đồng và số lượng có hiệu lực. Không dùng cấu hình giá thay cho đăng ký dịch vụ. |
| Đo đếm/cảnh báo | Cần thiết kế lại trách nhiệm của `meter_readings`: điểm đo vật lý khác khoảng tính tiền thuộc hợp đồng. Giữ cảnh báo có người kiểm tra nhưng đổi nguồn đánh giá cho phù hợp kỳ lẻ. |
| Tài chính | Giữ hóa đơn tháng/dòng/bất biến và nhiều lần giảm nợ, nhưng tách phiếu thu tiền thật khỏi phân bổ; thêm số dư cọc/trả trước/chưa rõ, chi hoàn và chuyển nội bộ. Mỗi dòng có kỳ dịch vụ riêng. `checkout_records` giữ phê duyệt/snapshot, không làm sổ ghi mọi giao dịch tiền. |
| Sự cố | Giữ yêu cầu và ảnh; mở đối tượng phát sinh ngoài hợp đồng, thêm lịch sử tiến độ/phân loại, lịch bảo trì và từng lần thực hiện. |

### 7.2. Nhóm thực thể ứng viên và thuộc tính cốt lõi

Các tên tiếng Anh nhằm tham chiếu khi thảo luận. Một nhóm có thể thành một hoặc nhiều bảng sau khi chốt; không cộng số dòng dưới đây để kết luận số bảng.

| Nhóm ứng viên | Dữ kiện tối thiểu cần lưu | Khóa/ràng buộc đáng chú ý |
| --- | --- | --- |
| Danh mục tiện nghi + tiện nghi phòng (`amenities`, `room_amenities`) | Mã/tên loại; phòng, loại, số lượng, ghi chú hiện tại | Mã loại UNIQUE; cặp phòng–loại UNIQUE; số lượng ≥ 0. Tên loại thuộc danh mục, không lặp ở liên kết. |
| Hạng mục có mã / thiết bị chung | Mã nội bộ, loại, vị trí, tình trạng khai thác, thời gian dùng | Chỉ theo dõi từng đồ khi cần phân biệt; thiết bị ở phòng thì suy ra khu qua phòng. Thiết bị chung gắn khu/điểm chung riêng; không lưu cả hai FK vị trí mâu thuẫn. |
| Biên bản bàn giao + chi tiết | Hợp đồng, nhận/trả, thời điểm, người giao/nhận, trạng thái xác nhận; từng hạng mục, số lượng, tình trạng, ảnh/căn cứ | Mã biên bản UNIQUE; số dòng UNIQUE/biên bản. Bản đã xác nhận bất biến. Không để tên/tình trạng ở danh mục hiện tại thay cho nội dung đã giao. |
| Ảnh phòng | Phòng, mã tệp/URL, chú thích, thứ tự, người/thời điểm thêm | FK phòng; mỗi dòng một ảnh. Ảnh chứng cứ biên bản đã chốt có vòng đời riêng với ảnh giới thiệu phòng. |
| Lịch sử giá phòng | Phòng, ngày hiệu lực, đơn giá, người xác lập | `(room_id, effective_from)` UNIQUE; lấy mốc gần nhất không sau ngày cần tra. Không cần vừa lưu `effective_to` vừa suy ra nó nếu không có lý do. |
| Bản hợp đồng ký / phụ lục / điều kiện giá | Hợp đồng, số phiên bản, ngày ký/hiệu lực, hạn/giá/cọc và căn cứ được xác nhận | Phiên bản UNIQUE/hợp đồng; các khoảng giá thuê áp dụng không chồng vô ý. Giá/cọc mới không làm đổi khoảng cũ; bản ký bất biến. |
| Bên hợp đồng / vai trò liên hệ / cư trú | Ai ký và chịu trách nhiệm theo thỏa thuận; ai là liên hệ từ–đến; từng người thực ở từ–đến | Không dùng tài khoản/primary_tenant_id suy ra cả ba vai trò. Bản đầu có thể chỉ một bên thuê chính, nhưng vai trò liên hệ và số người ở phải độc lập; quyền tài chính được cấp riêng. |
| Thông báo trả / bàn giao / khoảng nghĩa vụ thuê | Ngày báo, dự kiến, sửa/hủy, bàn giao thực tế, kết thúc tính tiền, lý do và xác nhận; trạng thái còn giữ phòng/quá hạn | Báo trả không tự kết thúc; phòng chưa bàn giao không tự trống. Ngày hiệu lực nghiệp vụ khác ngày nhập, không ghi lùi thời điểm thao tác. |
| Phiếu giữ chỗ, nếu D22 duyệt | Phòng, người đề nghị, thời hạn, tiền/điều kiện hủy/hoàn, trạng thái, liên kết hợp đồng khi chốt | Giữ chỗ có hiệu lực tham gia kiểm tra xung đột phòng; hết hạn/hủy không tự xóa nghĩa vụ tiền. Chuyển tiền giữ chỗ sang cọc không tạo phiếu thu mới. |
| Đăng ký dịch vụ (`contract_service_subscriptions`) | Hợp đồng, dịch vụ, bắt đầu/kết thúc, số lượng dùng trong khoảng, người ghi | Khóa nghiệp vụ hợp đồng–dịch vụ–mốc bắt đầu; khoảng cùng dịch vụ không chồng. Đổi số xe thì đóng khoảng cũ, thêm khoảng mới, không sửa ngược. |
| Công tơ + điểm đo + khoảng tiêu thụ | Công tơ thuộc phòng/dịch vụ; điểm đo có thời gian/chỉ số/người ghi; khoảng có hai điểm và hợp đồng chịu trách nhiệm hoặc chủ nhà | Công tơ đang dùng duy nhất/phòng/dịch vụ theo hiệu lực. Hai điểm cùng công tơ, thời gian tăng, chỉ số không giảm. Khoảng không bị tính hai lần. |
| Lịch sử sửa điểm đo | Điểm đo, giá trị trước/sau, người, thời gian, lý do | FK nguồn được giữ; không thay người ghi gốc bằng người sửa. Điểm đã khóa không sửa trực tiếp. |
| Cấu hình/căn cứ cảnh báo | Ngưỡng khu, ghi đè phòng tùy chọn; phương pháp, cửa sổ, baseline đã dùng, thời lượng, các nguồn lịch sử và kết quả/kiểm tra | Không lưu lại lượng dùng nếu đã suy ra từ khoảng đo khóa; lưu baseline/phương pháp tại lần đánh giá để giải thích được kết quả. |
| Phiếu thu / phiếu chi hoàn (`cash_receipts`, `cash_refunds`) | Số tiền thật, ngày nhận/chi và ngày ghi, phương thức/tài khoản, người nộp/nhận, tham chiếu giao dịch, người xác minh/trạng thái | Mã nội bộ UNIQUE; tham chiếu ngân hàng chống ghi lại cùng giao dịch khi đủ định danh. Chờ xác minh khác đã nhận. Có thể chưa rõ hợp đồng; không bắt FK hóa đơn để ghi nhận tiền thật. |
| Phân bổ tiền và quỹ cọc/trả trước | Phần tiền từ phiếu thu hoặc số dư nguồn, đích khoản phải thu/quỹ cọc/trả trước, số tiền, người/thời điểm, điều kiện sử dụng | Tổng phân bổ không vượt nguồn khả dụng; đích rõ bằng FK. Sổ quỹ chỉ nhận phần đã phân loại đúng; chưa phân bổ không tự thành cọc. Khoản giảm nợ không tạo dòng tiền vào lần hai. |
| Chuyển cọc nội bộ / khoản tạm giữ | Nguồn–đích, cùng số tiền, người có quyền/duyệt/căn cứ; khoản tạm giữ có lý do, số tiền, mốc rà lại và trạng thái | Hai đầu chuyển commit/rollback cùng nhau; không lớn hơn số dư khả dụng, không tự chuyển sang bên thuê khác. Tạm giữ không là tiền bồi thường đã thu; không giữ vô thời hạn thiếu căn cứ. |
| Khoản phải hoàn / người thuê ứng chi | Người có quyền nhận, khoản tiền/căn cứ/người duyệt; nguồn chi hộ, phần đã hoàn hoặc được đồng ý bù trừ | Bù trừ khoản phải hoàn với nợ thuê cần liên kết hai nghĩa vụ và cùng transaction; không giả một khoản thu/chi tiền thật. Chi phí sửa không tự trở thành tiền người thuê đã ứng nếu chưa xác minh. |
| Hồ sơ bồi thường + chi tiết (`compensation_cases`, `compensation_items`) | Hợp đồng chịu trách nhiệm, trạng thái/duyệt; hạng mục, nguyên nhân, chứng cứ, nguồn bàn giao/sự cố, số tiền được duyệt | Dòng có số thứ tự/khóa nguồn chống tạo lại cùng khoản. Chỉ APPROVED tạo phải thu; tiền không âm. Tổng hồ sơ lấy từ dòng, không nhập hai lần. |
| Khoản giảm nợ / phân bổ thanh toán | Nguồn phiếu thu/cọc/trả trước hoặc khoản phải hoàn được duyệt để bù trừ; đích hóa đơn/bồi thường/chứng từ phát sinh, số tiền | Đúng một nguồn và một đích bằng FK phù hợp, không dùng `target_type/target_id` chuỗi thiếu FK. Mã thao tác chống ghi lặp; kiểm tra quyền/điều kiện nguồn và đích cùng lúc. |
| Chứng từ điều chỉnh / đảo giao dịch, nếu D13 duyệt | Nguồn gốc, lý do, chênh lệch tăng/giảm hoặc khoản đảo, người duyệt, thời điểm | Liên kết FK bản gốc, mã duy nhất; không đảo quá phần chưa đảo. Phân biệt đề nghị hoàn với chứng từ đã chi; mục 7.3. |
| Quyết toán và nguồn được chốt | Hợp đồng, biên bản trả, trạng thái/phê duyệt, số dư cọc và nợ trước cấn, các nguồn tính, người/thời điểm | Tối đa một quyết toán được duyệt/hợp đồng trong phạm vi này; có thể có nháp. Không giữ tổng damage độc lập nếu đã có chi tiết được chốt. |
| Yêu cầu sửa chữa và chi phí | Đích hợp đồng/phòng/thiết bị, người báo/giao, loại/ưu tiên, hạn; sự kiện tiến độ và khoản chi thực tế | Đúng một đích; trạng thái đi đúng luồng. Tổng chi lấy từ chi tiết nếu chọn quản lý nhiều khoản, không nhập cả tổng hiện hành. |
| Lịch bảo trì + lần thực hiện | Thiết bị chung/điểm bảo trì, chu kỳ, ngày đến hạn; từng kỳ công việc, người làm, kết quả, chi phí | `(schedule_id, scheduled_for)` UNIQUE; hoàn thành một kỳ không xóa lịch sử kỳ trước. |
| Vi phạm và xử lý | Hợp đồng, loại/nội dung, ngày, trạng thái, lý do/xử lý, người thực hiện | FK hợp đồng, lịch sử trạng thái. Quyết định phạt tiền nếu có phải thành chứng từ khác được duyệt. |
| Thông báo + người nhận | Loại/nội dung/nguồn sự kiện/thời gian; từng user nhận, `read_at` | `(notification_id, user_id)` UNIQUE. Chưa đọc biểu diễn bằng `read_at IS NULL`, không cần cột boolean riêng phải đồng bộ. |
| Phân công phòng và lịch sử phân công | Nhân viên, khu **hoặc** phòng, mốc cấp/thu hồi, người cấp | Có thể giữ bảng khu và thêm bảng phòng; không cần lặp property_id trong phân công phòng. Không cho khoảng cùng cặp nhân viên–đối tượng chồng nhau. |
| Nhật ký nghiệp vụ / lịch sử chuyên biệt | Người, thao tác, đối tượng/khóa nghiệp vụ, thời điểm, lý do, phần trước/sau được phép lưu | Actor có FK; sự kiện xóa phải tồn tại sau khi đối tượng bị xóa. Không bắt audit có FK sống tới một nháp đã xóa; định danh lịch sử có chủ đích. |
| Mã khôi phục mật khẩu | User, hash mã, phát/hết hạn, người cấp, số lần thử, dùng lúc nào | Một lần, hết hạn, giới hạn thử; không lưu/ghi log mã thô, mật khẩu hay token đăng nhập. |
| Lịch sử khai thác phòng phục vụ thống kê | Ngày bắt đầu/ngừng đưa phòng vào danh mục, các khoảng bảo trì/khai thác | Cần nếu tính mẫu số lấp đầy lịch sử đúng. Không tự dựng thời điểm lịch sử khi dữ liệu cũ chưa lưu. |

Lịch sử sửa chỉ số, phụ lục ký và audit chung phục vụ ba mục đích khác nhau: phép đo cần cấu trúc để tra soát; phụ lục là sự kiện thỏa thuận; audit ghi dấu thao tác. Có thể dùng chung hạ tầng ghi sự kiện, nhưng không lưu ba bản có thể sửa độc lập của cùng một dữ kiện.

Người nhận thông báo được đề xuất theo loại: hóa đơn mới/nhắc nợ gửi đại diện và quản lý liên quan; sắp hết hạn gửi đại diện và A/S phụ trách; cảnh báo tiêu thụ chưa xác minh gửi A/S xử lý; cập nhật sửa chữa gửi người báo và người xử lý; thông báo chung gửi nhóm người nhận do ADMIN chọn trong đúng phạm vi. Quyền mở nguồn thông báo vẫn được kiểm tra khi bấm xem, không chỉ dựa vào việc từng nhận thông báo. Ký/xác nhận bàn giao và phụ lục trong phương án này là ghi nhận thủ công, chưa đề xuất tích hợp chữ ký số.

### 7.3. Nguồn sự thật của tiền và công nợ

**Sau rà thực tế, khuyến nghị thay mô hình “một giao dịch thu bắt buộc gắn một hóa đơn” bằng phiếu thu nguồn và các phân bổ.** Vẫn ghi nhận thủ công, không cần tích hợp ngân hàng hoặc xây một hệ kế toán đầy đủ. Số tiền ghi ở phiếu là sự kiện nhận thật; số tiền trên phân bổ là sử dụng một phần nguồn đó, không phải một lần nhận mới.

- `contracts.deposit_amount`: **cọc cam kết**, không cộng vào số tiền đang giữ.
- Phiếu thu/chi hoàn: dòng tiền thật đã xác minh, không bị cắt bớt vì lớn hơn nợ hóa đơn; phiếu chờ xác minh chưa tạo tiền khả dụng.
- Phân bổ: mỗi phần tiền đi vào nợ hóa đơn/bồi thường/khoản phát sinh, quỹ cọc hoặc trả trước. Phần chưa biết đích vẫn có số dư chưa phân bổ để đối soát. Một phiếu có nhiều phân bổ, nhưng không phân bổ trùng cùng một đồng tiền.
- Sổ cọc và sổ trả trước: nhận tác động từ phân bổ tiền nguồn, chuyển nội bộ, cấn, hoàn và bút toán đảo. Hai loại tiền có mục đích khác nhau, không dùng lẫn theo ý hệ thống.
- `payment_transactions` hiện hành chỉ có thể tái sử dụng sau khi làm rõ trách nhiệm là **khoản giảm nợ có nguồn**, không vừa là phiếu thu gốc vừa là mọi phân bổ. `DEPOSIT_OFFSET` là giảm cọc/giảm nợ, không thêm phiếu thu.
- `compensation_items`: từng khoản bồi thường được duyệt, tiền lấy tại đây; quyết toán chỉ tham chiếu, không sinh bản sao để thu lại.
- Quyết toán: kết quả phê duyệt và snapshot trước cấn. Số dư hiện tại và tổng đã hoàn luôn đọc từ giao dịch, không lấy snapshot cũ làm số dư mới.

Công thức khái niệm, các khoản đảo có liên kết được phản ánh đúng chiều:

```text
Cọc còn giữ = phần nhận vào cọc + chuyển cọc vào + khôi phục cấn
              − cấn cọc − thực hoàn cọc − chuyển cọc ra
Cọc khả dụng = cọc còn giữ − khoản tạm giữ có căn cứ/hiệu lực
Tiền trả trước/chưa phân bổ được theo dõi riêng, không cộng vào cọc
Nợ từng đích = khoản phải thu được duyệt + điều chỉnh tăng − điều chỉnh giảm
              − phân bổ tiền/cọc/trả trước có hiệu lực
Tổng tiền thực nhận = tổng phiếu thu đã xác minh, không cộng thêm phân bổ/cấn/chuyển
Tổng tiền thực chi hoàn = tổng phiếu chi đã thực hiện, không phải tổng đề nghị hoàn
```

Mỗi lần cấn không vượt `min(cọc khả dụng được phép dùng, nợ đích đủ điều kiện còn lại)`. Thứ tự được thể hiện để người có quyền xác nhận; không mặc định khoản bồi thường đang tranh chấp được ưu tiên. Cọc đủ để cấn không có nghĩa được phép cấn trước thời điểm/thỏa thuận cho phép. Tiền có thể hoàn được xác định sau các nghĩa vụ/khoản giữ có căn cứ; phần chưa đủ căn cứ theo dõi chờ xử lý có mốc rà lại, không kết luận đã tất toán hoặc đã tịch thu.

Ví dụ **nhận thiếu cọc**: cam kết 3.000.000, thực nhận 1.000.000, chưa hoàn/cấn, nợ hóa đơn 700.000, bồi thường đã duyệt 500.000. Cấn 500.000 cho bồi thường và 500.000 cho hóa đơn → cọc còn 0, nợ hóa đơn 200.000. Không được báo hoàn 1.800.000 dựa trên cọc cam kết; cũng không tự thêm 2.000.000 cọc chưa nộp vào nợ trả phòng.

Ví dụ **thiệt hại vượt cọc**: thực giữ 1.000.000, bồi thường 1.600.000, không nợ hóa đơn. Cấn 1.000.000 → bồi thường còn phải thu 600.000; thu thêm 200.000 thì còn 400.000 và truy ra được từng giao dịch. Chi phí sửa có thể là số khác 1.600.000, không dùng chung trường.

Ví dụ **tính ra tiền hoàn nhưng chưa hoàn**: thực giữ 3.000.000, nợ hóa đơn 1.680.000, bồi thường 200.000 → cấn 1.880.000, còn phải hoàn 1.120.000. Trước giao dịch chi: đã hoàn 0, vẫn còn giữ 1.120.000. Hoàn thực tế 500.000 → còn phải hoàn 620.000. Không lấy alias `actual_refund_amount` của view cũ làm bằng chứng đã chi.

Ba ví dụ cấn trên giả định khoản nợ/bồi thường không tranh chấp, đủ điều kiện sử dụng cọc và các bên đã xác nhận phân bổ. Chúng là ví dụ số học, không là thứ tự cấn bắt buộc cho mọi cơ sở.

Nếu D13 được duyệt: số phải trả của từng đích gồm chứng từ gốc cộng các chênh lệch được duyệt. Giao dịch đảo có FK tới giao dịch gốc và số tiền dương, chiều tác động ngược; tổng đảo không vượt khoản chưa đảo. Khi đã giảm nợ bằng cọc, đảo phần cấn sẽ phục hồi cọc; khi hoàn khoản từng thu bằng tiền, phải có chứng từ thực chi. Không tự chi tiền mặt chỉ vì đang đảo một lần cấn cọc. Chứng từ giảm tạo số phải hoàn nếu đã thu quá số đúng; đến lúc có giao dịch hoàn/đảo hợp lệ thì nghĩa vụ đó mới giảm.

Các báo cáo tách tiền nhận/chi thật, tiền cọc/trả trước còn giữ, tiền đã phân bổ, doanh số lập chứng từ và công nợ. Không cộng cấn/chuyển/phân bổ thành thu tiền mới. “Còn tiền của khách” và “khách còn nợ” có thể cùng xuất hiện khi tiền chưa được phép phân bổ vào khoản đang tranh chấp; không tự bù trừ cho đẹp số. Hoàn cọc, hoàn tiền dư và hoàn do điều chỉnh có nguồn/căn cứ riêng, nhưng cùng ghi chi thật một lần.

Ví dụ **chuyển thừa**: nợ 3.000.000, tiền thực nhận 3.500.000 → phân bổ 3.000.000, nợ 0, còn tiền dư 500.000. Ví dụ **chưa rõ người chuyển**: ghi nhận 1.200.000 đã vào tài khoản, chưa gán hợp đồng → tiền chưa phân bổ 1.200.000, không làm giảm nợ một phòng đoán theo số tiền. Ví dụ **thu gộp**: nhận 4.000.000, phân bổ 1.500.000 cho nợ cũ và 2.500.000 cho kỳ mới → phiếu thu vẫn 4.000.000.

Quy tắc chống thu vượt trong thiết kế cũ được đổi thành **chống phân bổ vượt nguồn/nợ, chống ghi trùng cùng lần nhận tiền và ghi đủ tiền thật**. Hai người cùng bấm phân bổ 600 từ cùng một phiếu nguồn/nợ chỉ còn 1.000 thì không thể ghi 1.200; nếu là hai khoản chuyển ngân hàng thật 600 riêng biệt thì phải ghi đủ 1.200, phần chưa dùng còn tiền dư.

### 7.4. Vị trí sự cố và công tơ: tránh FK dư nhưng vẫn đủ nghiệp vụ

**Sự cố:** một trong ba đích `contract_id`, `room_id`, `equipment_id` là bắt buộc và hai đích còn lại NULL. Đích hợp đồng suy ra phòng; đích phòng suy ra khu; đích thiết bị suy ra vị trí đã xác định của thiết bị. Nhờ đó có thể báo cho hợp đồng đang ở, phòng trống hoặc thiết bị chung mà không nhập cả hợp đồng–phòng–khu rồi phải sửa ba nơi. Bồi thường gắn hợp đồng chịu trách nhiệm riêng, không suy ra tự động từ người báo sự cố. Nếu sau này cho di chuyển thiết bị, phải có lịch sử vị trí hoặc snapshot vị trí sự cố; phạm vi cơ sở coi thiết bị cố định tại vị trí đã đăng ký.

**Công tơ:** công tơ thuộc phòng/dịch vụ; điểm đo không thuộc riêng một khách. Khoảng tiêu thụ tham chiếu điểm đầu/điểm cuối cùng công tơ và hợp đồng chịu trách nhiệm, hoặc đánh dấu chủ nhà khi phòng trống. Không lưu thêm room_id/service_id trên khoảng nếu đã suy ra từ công tơ. Hai khách đổi ở mốc D cùng tham chiếu một điểm đo D, không tạo hai giá trị nhập tay có thể khác nhau.

Đề xuất ràng buộc: một khoảng không tính trùng cho hai bên; một nguồn lượng dùng không gắn hai dòng hóa đơn để thu lặp; hóa đơn một hợp đồng/kỳ có thể có nhiều khoảng đo khi cần. Khóa cũ `(contract_id, billing_period, service_item_id)` và UNIQUE một dòng/dịch vụ phải được xem lại nếu một dịch vụ có nhiều khoảng/căn cứ; không giữ chúng máy móc rồi gộp mất dấu nguồn. Lượng dùng là chênh hai điểm, không nhập thêm `consumption` hoặc `amount` vào khoảng.

Thay công tơ thủ công được đề nghị hỗ trợ: ghi ngày tháo/lắp, chỉ số chốt công tơ cũ, chỉ số mở công tơ mới và chứng cứ. Ví dụ cũ 1.000 → 1.080, mới 5 → 35 thì dùng 80 + 30 = 110, không lấy 35 − 1.000. Gặp số giảm trên cùng mã chưa có sự kiện thay/reset thì dừng để xác minh. Không có chỉ số tháo cũ thì chưa biết lượng thực; chỉ cho lập căn cứ ước tính riêng nếu được thỏa thuận/duyệt, đánh dấu ESTIMATED và đối soát sau. Không giả thành số đo thực tế.

Nhập nhầm một điểm chưa khóa có thể sửa có lịch sử; phát hiện sai điểm đã dùng phải tạo phiên bản/căn cứ điều chỉnh, không làm thay mọi đoạn đã thu. Điểm đầu được dùng cho nhiều khoảng liền kề là bình thường; chỉ **lượng của cùng một đoạn** không được thu trùng. Nếu hai phòng chung công tơ, phương án cơ sở phải báo chưa hỗ trợ cách phân bổ đó thay vì gán toàn bộ số dùng cho mỗi phòng.

### 7.5. Cách tính dịch vụ và cảnh báo đề xuất

```text
Tiền phòng chia ngày = tổng (giá của đoạn × ngày thuộc nghĩa vụ thuê ở đoạn / ngày tháng)
Phí dịch vụ chia ngày = giá áp dụng × số ngày/ngày-người/xe-ngày đúng phạm vi / ngày tháng
Phí cả kỳ/vé tháng = giá thỏa thuận × số lượng đủ điều kiện của kỳ, không tự chia ngày
Điện/nước đo đếm = tổng (chênh từng đoạn đúng công tơ × giá áp dụng của đoạn)
Khoản một lần/bồi thường = số được duyệt theo căn cứ; không chia cho số ngày tháng
```

Chỉ đếm **những người có khoảng cư trú thực tế được ghi nhận**, người ký không ở thì không cộng một người ngầm. Vắng tạm thời/đi nghỉ nhưng vẫn giữ phòng không tự đóng cư trú hoặc miễn phí; việc miễn phí đầu người trong lúc vắng phải có chính sách. Ngày bắt đầu tính tiền phòng có thể khác ngày chuyển đồ vào, vì đó là thời điểm quyền thuê/nghĩa vụ được thỏa thuận. Ngày hết tính tiền cũng không tự lấy từ ngày gửi thông báo trả.

Từng dòng có khoảng dịch vụ/cơ sở tính/giá và căn cứ riêng. Phí tháng có thể theo giá đầu kỳ theo thỏa thuận; giá thuê qua phụ lục có phiên bản hiệu lực. Không mặc định mọi thay giá điện/nước được trì hoãn tới kỳ sau; phải xác định cách áp dụng phù hợp nguồn giá được sử dụng. Nếu cần chia giá giữa kỳ mà không có số đo ở mốc chia thì phải có phương pháp phân bổ được xác nhận/ghi rõ, không bịa chỉ số ở ngày đó.

Khuyến nghị làm tròn từng dòng tiền đến hai chữ số thập phân rồi cộng; đơn vị tiền hiển thị có thể định dạng đồng nhưng nguồn đối soát phải dùng cùng quy tắc. Số ngày tháng là 28/29/30/31 từ lịch, không cố định 30. Nếu phí cùng dịch vụ có nhiều khoảng đăng ký cùng giá thì có thể cộng xe-ngày/ngày-người và vẫn truy được các nguồn; không gộp các mức giá khác nhau bằng một trung bình không giải thích được.

Đối với cảnh báo, so **lượng/ngày thực đo** với baseline có trọng số theo thời lượng của các khoảng phù hợp. Cùng phòng nhưng nhóm người thuê hoặc số người đã đổi thì không coi baseline cũ là tương đương tuyệt đối; ghi lý do/cấp độ tham khảo hoặc ưu tiên lịch sử cùng nhóm cư trú. Nếu chuẩn hóa theo người-ngày thì cần dữ liệu người-ngày tương ứng, không suy từ số người cuối tháng. Ba kỳ/7 ngày chỉ là mặc định đề xuất chưa hiệu chỉnh bằng dữ liệu thật, không phải tiêu chuẩn thống kê đã chứng minh. Kỳ ngắn/thiếu lịch sử: chưa đủ dữ liệu so sánh, vẫn có thể phát hành khi giá và lượng dùng đủ căn cứ. Baseline 0 cần ngưỡng tuyệt đối/quy tắc kiểm tra riêng, không chia cho 0.

Ví dụ khi đã đủ số kỳ lịch sử: bình quân 0,3 m³/ngày, tương đương 9 m³ trong 30 ngày. Kỳ lẻ 6 m³/10 ngày → 0,6 m³/ngày, tăng 100%; nếu chỉ so 6 với 9 sẽ bỏ sót. Lưu nguồn baseline, cửa sổ, phương pháp và giá trị đã dùng. Thay số người có thể ảnh hưởng tiêu thụ; cảnh báo chỉ yêu cầu kiểm tra, không là bằng chứng người thuê dùng sai hoặc nợ thêm.

Kỳ hóa đơn, cửa sổ từng dịch vụ và ngày thanh toán là dữ kiện riêng: hóa đơn kỳ 10 có thể thu phòng [01/10,01/11) và điện đo [28/08,28/09). Phải hiện cả hai; mốc đổi khách chen vào chuỗi, điện khi phòng trống thuộc chủ nhà. Lần đầu nhận phòng chưa có điện kỳ trước của mình thì không bắt có dòng điện 0 để phát hành. Trả giữa kỳ sau khi đã đóng trước tiền phòng phải xác định khoản giảm/hoàn và điện kỳ cuối bằng chứng từ phát sinh/quyết toán; không phá bản gốc hoặc thu lại khoản đã có. D24 là thay đổi cách hiểu “hóa đơn theo kỳ” cần anh chốt rõ so với ràng buộc cũ bắt mọi chỉ số cùng `billing_period` hóa đơn.

Với lấp đầy, đề xuất chỉ số theo kỳ = tổng ngày-phòng có cư trú / tổng ngày-phòng có trong danh mục khai thác của kỳ, tính cả ngày bảo trì trong mẫu số để thấy mức sử dụng toàn bộ danh mục. Nếu muốn loại bảo trì khỏi mẫu số thì báo thêm một chỉ số riêng và ghi đúng định nghĩa. Tỷ lệ chụp tại một ngày là chỉ số khác; không dùng số cuối tháng thay cho lấp đầy cả tháng. Cần thời gian đưa phòng vào/ngừng danh mục và trạng thái lịch sử; không thể tái tạo mẫu số cũ chỉ từ trạng thái hiện tại.

## 8. Chứng minh quan hệ, toàn vẹn và chuẩn hóa

Đây là chứng minh cho các quyết định logic đã mô tả; chưa phải chứng nhận một schema mới hoàn chỉnh. Sau khi chốt cần lập danh sách thuộc tính, khóa ứng viên và phụ thuộc cho **từng bảng cuối cùng**, rồi mới xuất DBML/SQL.

### 8.1. Quan hệ và lực lượng

| Quan hệ | Biểu diễn / điều phải giải thích |
| --- | --- |
| Tài khoản — hồ sơ người thuê | 0..1–0..1 với `user_id` nullable + UNIQUE. Tài khoản quản lý có thể không có hồ sơ thuê; hồ sơ người ở có thể không có đăng nhập. |
| Hợp đồng — quyết toán được duyệt | 1–0..1: bản quyết toán bắt buộc thuộc một hợp đồng, mỗi hợp đồng tối đa một bản đã duyệt. Chưa trả phòng có thể chưa có quyết toán. |
| Hợp đồng — bản ký gốc | Khi hợp đồng đã ký, phải có đúng một bản ký gốc; các phụ lục là quan hệ 1–N riêng. FK UNIQUE chỉ bảo đảm tối đa một bản gốc, điều kiện “đã ký phải có bản” cần quy tắc vòng đời/transaction. |
| Khu — phòng; hợp đồng — hóa đơn; biên bản — chi tiết; hồ sơ bồi thường — chi tiết | 1–N: FK NOT NULL ở phía nhiều; không có bản con mồ côi. |
| Công tơ — điểm đo; hóa đơn — dòng; khoản phải thu — giao dịch | 1–N. Một điểm đo có thể là cuối khoảng trước và đầu khoảng sau; không gắn UNIQUE toàn cục lên một điểm làm mất tính liên tục. |
| Phiếu thu — các khoản phải thu | N–N qua phân bổ: một phiếu có thể trả nhiều đích, một đích nhận từ nhiều phiếu. Cọc/trả trước cũng là nguồn riêng có lịch sử, không tạo lại tiền thật khi áp vào nợ. |
| Phòng — tiện nghi | N–N qua liên kết phòng–loại tiện nghi, có số lượng; không dùng danh sách mã trong TEXT. |
| Nhân viên — khu / phòng | N–N qua phân công và khoảng hiệu lực; giao phòng không cần lặp khu suy ra. |
| Hợp đồng — người thực ở | N–N theo khoảng cư trú; gồm người ký nếu thực ở. Vai trò bên ký/liên hệ là quan hệ riêng, không dùng để cộng số người. Một người có thể rời rồi quay lại, nên khóa không chỉ là cặp hợp đồng–người. |
| Hợp đồng — dịch vụ | N–N qua đăng ký có khoảng hiệu lực và số lượng. Lịch sử giá khu–dịch vụ là quan hệ khác, không thay thế đăng ký. |
| Thông báo — tài khoản nhận | N–N qua người nhận thông báo; trạng thái đọc là thuộc tính của cặp, không phải thông báo chung. |

Không thêm một bảng 1–1 không có dữ kiện riêng chỉ để đạt tiêu chí chấm. Một quan hệ 1–0..1 là trường hợp tùy chọn của kết nối một–một; cần nói đúng lực lượng và điều kiện, không chỉ vẽ nhãn “1–1”.

### 8.2. Khóa, kiểu dữ liệu và các lớp ràng buộc

| Nội dung | Đề xuất |
| --- | --- |
| PK nội bộ | BIGINT IDENTITY; mã nghiệp vụ riêng có UNIQUE. Không dùng họ tên/điện thoại làm PK. |
| Tiền / đơn giá | NUMERIC chính xác; có thể tiếp tục NUMERIC(12,2) cho tiền dòng và NUMERIC(14,2) cho tổng/số dư theo giới hạn hiện tại. Không dùng float/double cho tiền. |
| Chỉ số / số lượng | NUMERIC theo độ chính xác công tơ; số người/xe là số nguyên không âm; ngày-người/xe-ngày tính từ các khoảng. Không dùng `invoice_items.quantity` nguyên hiện tại cho mọi loại lượng một cách máy móc. |
| Ngày và thời điểm | DATE cho ngày hiệu lực/kỳ; TIMESTAMPTZ cho ghi/duyệt/thực thu/chi/điểm đo. Giữ ngày sự kiện thực tế và thời điểm nhập riêng; khoảng giá/nghĩa vụ/cư trú riêng, ngày nghiệp vụ dùng Asia/Ho_Chi_Minh. |
| CCCD/điện thoại | VARCHAR; chuẩn hóa khoảng trắng/NULL. CCCD có giá trị thì kiểm tra UNIQUE; không kết luận hai hồ sơ là một chỉ vì trùng tên. |
| FK | Tất cả tham chiếu nghiệp vụ sống có FK; lịch sử tài chính, hợp đồng ký và biên bản dùng RESTRICT khi xóa cha. CASCADE chỉ dùng ở thành phần phụ thực sự được phép xóa cùng nháp. |
| UNIQUE nghiệp vụ | Mã hợp đồng/phiếu/giao dịch; phòng trong khu; hợp đồng–tháng cho hóa đơn tháng; phòng–loại tiện nghi; thông báo–người nhận; lịch bảo trì–kỳ công việc; khóa chống lặp nguồn bồi thường/điều chỉnh. |
| CHECK một dòng | Tiền giao dịch > 0; giá/bồi thường ≥ 0; ngày đầu < ngày cuối; trạng thái hợp lệ; đúng một đích FK cho sự cố/thanh toán; trạng thái duyệt khớp người và thời điểm duyệt. |
| Quy tắc thời gian/liên bảng | Lịch thuê đã xác nhận không chồng; bàn giao mới phải kiểm tra người đang thực giữ phòng và mức sẵn sàng. Cư trú đúng sức chứa; đăng ký/giá đúng phạm vi; hai điểm cùng công tơ; nguồn đo/biên bản đúng vị trí. Không dùng ngày hết hạn tự đóng khoảng chiếm giữ thực tế. |
| Cách thi hành | UNIQUE/FK/CHECK cho điều kiện biểu diễn trực tiếp; EXCLUDE với khoảng được lưu trên cùng bảng; trigger/service có khóa cho tổng, quyền, liên bảng. Không tuyên bố CHECK đơn giản đã kiểm tra được tổng nhiều dòng hoặc một EXCLUDE đã tự kiểm soát mọi JOIN. |

Chưa có quyết định một người tuyệt đối không được gắn hai hợp đồng khác nhau trong cùng thời gian: V1 không nói đủ điều này. Ràng buộc bắt buộc hiện có là không thuê nguyên phòng chồng thời gian và không đếm trùng một người trong cùng hợp đồng. Nếu muốn cấm cư trú đồng thời ở hai phòng thì phải chốt thêm chính sách, nhất là khi đại diện ký thay nhưng không cư trú.

Lịch tương lai và tình trạng giữ phòng thực tế cần kiểm tra ở hai thời điểm: có thể nhận lịch khách kế tiếp sau ngày hết hạn dự kiến, nhưng **không kích hoạt bàn giao khi phòng vẫn bị người cũ chiếm giữ hoặc chưa sẵn sàng**. Quá hạn tạo xung đột vận hành để xử lý, không tự đổi lịch/hợp đồng khách sau. Sức chứa tính người đăng ký cư trú trong khoảng; không dùng người ký thay hoặc tài khoản đăng nhập để cộng số người.

### 8.3. Các phụ thuộc dữ liệu để chứng minh 2NF/3NF

| Quan hệ và khóa nghiệp vụ | Phụ thuộc đúng / dữ liệu không đặt lặp |
| --- | --- |
| Tiện nghi phòng: `(room_id, amenity_id)` | Cả cặp xác định số lượng của loại đó trong phòng. `amenity_id → tên loại` nên tên loại ở danh mục; `room_id → mã/khu phòng` nên không lặp ở liên kết. |
| Thành viên: `(contract_id, tenant_id, effective_from)` | Cả lần cư trú xác định ngày rời/quan hệ. `tenant_id → họ tên/CCCD hiện tại` nên thông tin người ở tenant_profiles. Cùng cặp có thể có nhiều lần cư trú. |
| Giá dịch vụ: `(property_id, service_item_id, effective_from)` | Ba thành phần xác định đơn giá tại mốc. Tên khu phụ thuộc property_id, tên dịch vụ phụ thuộc service_item_id; hai tên không đặt ở mỗi dòng giá. |
| Chi tiết bàn giao: `(handover_id, line_no)` | Xác định hạng mục/số lượng/tình trạng ghi nhận của lần giao. `handover_id → hợp đồng, thời điểm, loại nhận/trả` nên không lặp các trường đầu biên bản trên mọi dòng. |
| Người nhận thông báo: `(notification_id, user_id)` | Xác định thời điểm người đó đọc. Nội dung/loại thông báo phụ thuộc notification_id nên đặt ở phần thông báo, không sao chép cho từng người nhận. |
| Điểm đo: `reading_id` | Xác định meter_id, thời điểm/chỉ số. `meter_id → room_id, service_item_id`, `room_id → property_id`; không lưu chuỗi room/property/service lặp trên điểm đo. |
| Phiếu thu và phân bổ | receipt_id xác định lần nhận tiền thật; allocation_id xác định phần tiền đi từ nguồn nào vào đích nào. Người nộp khác người có nghĩa vụ/nhận hoàn, không suy ra cả ba từ một user_id. Không sao toàn bộ tiền phiếu vào từng phân bổ. |
| Hợp đồng và phụ lục | contract_id xác định hợp đồng; `(contract_id, amendment_no)` xác định một sự kiện thay đổi. Ngày kết thúc được ghi tại phụ lục là điều kiện ở sự kiện đó, không có phụ thuộc “contract_id xác định một ngày kết thúc cho mọi phụ lục”. |

**1NF:** tiện nghi, ảnh, thành viên và dòng biên bản là từng bản ghi, không lưu danh sách mã/ảnh/người vào một cột để truy vấn nghiệp vụ.

**2NF:** với mọi khóa ứng viên ghép, thuộc tính phi khóa phải phụ thuộc đầy đủ vào khóa. Ví dụ số lượng tiện nghi phụ thuộc phòng **và** loại; tên loại chỉ phụ thuộc một phần nên phải tách. Thêm `id` không xóa phụ thuộc một phần của khóa ứng viên còn tồn tại. Khóa nullable UNIQUE như CCCD không được trình bày như khóa ứng viên của toàn bộ quan hệ nếu vẫn cho nhiều NULL.

**3NF:** xét các phụ thuộc bắc cầu, ví dụ điểm đo → công tơ → phòng → khu; tách danh mục/quan hệ như trên để không duy trì nhiều nguồn của cùng dữ kiện. Một FK cần để biểu diễn vai trò riêng vẫn có ý nghĩa: khoảng tiêu thụ cần công tơ qua hai điểm và hợp đồng chịu trách nhiệm; đây là hai vai trò nghiệp vụ, không phải lý do thêm tiếp `room_id` suy ra.

### 8.4. Dữ liệu hiện tại, lịch sử, snapshot và tính toán

| Loại | Ví dụ / nguyên tắc |
| --- | --- |
| Dữ liệu hiện tại | Tên tiện nghi, thông tin liên hệ hiện tại, tình trạng khai thác. Chỉ có một nguồn chỉnh sửa được phép. |
| Lịch sử sự kiện | Nhận cọc, hoàn cọc, thu tiền, gia hạn, bàn giao, đổi trạng thái sửa chữa. Có người/thời gian/căn cứ; sự kiện đã chốt chỉ sửa qua sự kiện đảo/điều chỉnh phù hợp. |
| Snapshot cần giữ | Tên/đơn vị/giá/cơ sở tính áp dụng trên hóa đơn; tên/tình trạng vật tại bàn giao; nội dung hợp đồng ký; số dư trước cấn. Các giá trị này mô tả thời điểm giao dịch, không có nghĩa “giá hiện tại”. |
| Dữ liệu tính | Chênh chỉ số, tiền dòng, tổng hóa đơn, nợ còn lại, cọc còn giữ, tổng bồi thường, số phải hoàn/đã hoàn và tỷ lệ lấp đầy. Tính từ nguồn đã khóa hoặc view, không nhập độc lập lần nữa. |

Snapshot không phải giấy phép sao chép mọi trường. Nếu đã tham chiếu **phiên bản đơn giá bất biến** mà phiên bản đó quyết định đúng toàn bộ đơn giá áp dụng, có thể lấy từ phiên bản thay vì vừa lưu FK vừa sao giá. Nếu chọn giữ snapshot độc lập để bảo toàn chứng từ thì phải chỉ rõ nguồn có thẩm quyền, thời điểm đóng băng và ngoại lệ dư thừa được kiểm soát; không nói mọi trường lặp tự nhiên đều đạt 3NF. Nội dung ảnh/điều khoản/giá có khả năng bị sửa phải được khóa hoặc có phiên bản, nếu không view vẫn có thể làm lịch sử thay đổi.

### 8.5. Giao dịch đồng thời và chống ghi lại

- Phân bổ/hoàn/cấn/chuyển tiền đã rõ chủ thể khóa theo thứ tự thống nhất: các hợp đồng liên quan, nguồn phiếu/quỹ theo ID, rồi các đích nợ theo ID. Chuyển giữa hai hợp đồng phải khóa cả hai; không đọc tổng rồi INSERT mà bỏ khóa. Nhận/xác minh phiếu chưa rõ hợp đồng là transaction riêng, không đòi phải có hợp đồng mới ghi được tiền thật.
- Thao tác thay lịch thuê/gia hạn/bàn giao đổi khách lấy khóa phòng trước, rồi các hợp đồng theo thứ tự ID. Luồng chỉ thu tiền không quay lại lấy khóa phòng sau khi đã khóa nợ; trigger/service cũng phải theo cùng thứ tự để tránh khóa chéo.
- Trong transaction ghi sổ: kiểm tra quyền/trạng thái mới nhất → khóa → tính lại số dư nguồn/nợ đích → ghi chứng từ/phân bổ → kiểm tra bất biến → commit. Nhận tiền, phân bổ và thực chi có thể là các sự kiện khác ngày; không giữ transaction chờ người dùng hoặc chờ chuyển khoản.
- Mã giao dịch/yêu cầu duy nhất bảo vệ việc bấm lại/gửi lại. Cùng mã và cùng nội dung có thể trả lại kết quả; cùng mã khác số tiền/đích thì trả lỗi xung đột. Chống gửi lặp không thay thế khóa chống hai mã khác nhau cùng vượt nợ/cọc.
- Tham chiếu ngân hàng khi có phải gắn đúng tài khoản nhận/giao dịch để ngăn hai nhân viên ghi cùng lần chuyển bằng hai mã nội bộ. Không dùng UNIQUE chỉ trên số tiền/ngày/người để chặn nhầm hai lần chuyển thật giống số tiền. Ảnh chụp không là định danh giao dịch đã xác minh.
- Sửa nháp/chỉ số phải kiểm tra phiên bản hoặc đọc lại sau khóa; không cho người mở màn hình cũ ghi đè sửa mới. Cảnh báo, nguồn đo, đăng ký dịch vụ và ngày cư trú không được thay đổi làm lệch hóa đơn đã phát hành.
- Tạo thông báo và lịch công việc có khóa nghiệp vụ chống chạy hai lần. Lỗi rollback không để lại dòng thu tiền, cấn cọc, thông báo phát hành hoặc công việc bảo trì giả.

Nhắc nợ cần nhận diện tiền đã nhận/chưa phân bổ hoặc đề nghị đang xác minh để không gửi thông điệp sai rằng chưa có bất kỳ khoản thanh toán nào. Đồng thời không đổi hóa đơn thành đã trả chỉ từ ảnh hoặc tiền chưa được phép áp vào khoản đó. Báo cáo doanh số lập, tiền thu và chi phí không mặc nhiên là báo cáo lãi; phạm vi hiện tại chưa phải hệ kế toán đầy đủ.

## 9. Tình huống nghiệm thu đề xuất — CHƯA CHẠY

Các số dưới đây là dữ liệu giả lập cho nghiệm thu. Kết quả yêu cầu không phải kết quả test đã đạt. D01–D24 chưa chốt nên một số ca phụ thuộc lựa chọn phạm vi. Ví dụ chia ngày giả định hợp đồng/dịch vụ đã chấp nhận cách chia đó; ví dụ cấn giả định đủ điều kiện, không có tranh chấp/phần giữ chưa xử lý.

| Mã | Dữ liệu / thao tác | Kết quả bắt buộc khi triển khai phương án | Luồng / Use Case |
| --- | --- | --- | --- |
| NT01 | Khách A ở [01/09,16/09), B ở [16/09,01/10), giá 3.000.000; chỉ số 1000 → 1050 → 1120 | Mỗi khách 15 ngày tiền phòng; lượng điện A=50, B=70. Nợ A còn nguyên ở A; không có một hóa đơn gộp phòng bắt B trả nợ A | L2/L5, PA22/28/32 |
| NT02 | A là bên thuê chịu trách nhiệm rời ngày 16; các bên xác nhận B thay trách nhiệm từ 16, B/C ở lại | Phương án hợp đồng kế tiếp giữ nợ trước mốc ở A, không chuyển cọc ngầm; biên bản giao trách nhiệm, đếm đúng B/C thực ở. Chỉ đổi liên hệ thì không dùng nhánh này | L4/L5, PA19/22/24 |
| NT03 | Cam kết cọc 3.000.000, nhận 1.000.000; nợ hóa đơn 700.000, bồi thường 500.000 | Sau cấn: cọc 0, bồi thường 0, hóa đơn còn 200.000. Cọc chưa nhận không thành tiền hoàn hoặc khoản thu giả | L1/L4, PA38/40/42 |
| NT04 | Điều hòa hỏng vì hao mòn, chi sửa 400.000 | Chi phí bảo trì 400.000, bồi thường người thuê 0; có lý do/căn cứ | L3, PA44/46 |
| NT05 | Chi sửa 800.000, phần lỗi người thuê được duyệt 500.000 | Phải thu chỉ 500.000; không tự lấy chi phí thực tế làm nợ. Thu/cấn một lần, quyết toán không sinh thêm 500.000 | L3/L4, PA39/44 |
| NT06 | Phòng 203 không có hợp đồng, quản lý báo rò nước | Tạo/điều phối sự cố theo phòng đúng quyền; không cần hợp đồng giả | L3, PA45/46 |
| NT07 | Bảo trì máy bơm chung đến hạn, hai lần chạy tác vụ cùng kỳ | Chỉ một công việc cho lịch/kỳ; lưu lần thực hiện và chi phí, không chỉ dời ngày rồi mất lịch sử | PA47 |
| NT08 | Cọc thực giữ 1.000.000, bồi thường 1.600.000; sau cấn thu tiếp 200.000 | Sau cấn còn phải thu 600.000; thu 200.000 còn 400.000, xem được chứng từ trên hợp đồng đã kết thúc | L4, PA39/40/42/44 |
| NT09 | Cọc 3.000.000, nợ hóa đơn 1.680.000, bồi thường 200.000; chưa chi tiền hoàn | Sau cấn cần hoàn 1.120.000, đã hoàn 0. Hoàn 500.000 thì còn 620.000; không báo hoàn xong từ phép trừ | L4, PA41–43 |
| NT10 | Dịch vụ internet tùy chọn có giá tại khu nhưng hợp đồng không đăng ký | Không thu internet. Nếu là dịch vụ bắt buộc thì quy tắc áp dụng phải được xác lập rõ, không để thiếu giá rồi tính 0 | L2, PA25–27/32 |
| NT11 | Tháng 09, đại diện ở 30 ngày, thành viên ở [11/09,21/09), phí 30.000/người/tháng | 40 ngày-người, phí 40.000; thành viên vào/rời không làm đếm theo số người cuối tháng | L2, PA19/27/32 |
| NT12 | Gửi 1 xe trong [01/09,16/09), 2 xe trong [16/09,01/10), giá 60.000/xe/tháng | 45 xe-ngày, phí 90.000; có hai khoảng số lượng, không sửa ngược thành 2 xe cả tháng | L2, PA27/32 |
| NT13 | Hợp đồng có hóa đơn tháng 09 ISSUED, gia hạn từ 01/11 đến 01/12, không có lịch chồng | Gia hạn hợp lệ, có phụ lục và người duyệt; hóa đơn/PDF tháng 09 giữ nguyên | L5, PA20/21 |
| NT14 | Gia hạn trên nhưng phòng đã có hợp đồng kế tiếp từ 15/11 | Từ chối vì chồng lịch, không bỏ qua do khác mã hợp đồng | L5, PA18/21 |
| NT15 | Cùng nguồn tiền đã nhận 1.000, nợ 1.000; hai người đồng thời phân bổ 600 | Chỉ một phân bổ 600 thành công, nợ/nguồn còn 400; không phân bổ 1.200. Nếu có hai lần nhận tiền thật riêng thì xem NT41 | L2/L7, PA39/55 |
| NT16 | Cọc còn 1.000, hai hóa đơn đều nợ 800; đồng thời cấn 800 cho mỗi hóa đơn | Tổng cấn ≤ 1.000; yêu cầu sau phải đọc lại số còn, không để cả hai cùng cấn 800 | L4, PA42 |
| NT17 | Cùng một số dư cọc, thao tác hoàn và cấn/duyệt lại chạy đồng thời | Không âm cọc; hoàn không vượt phần được duyệt, không phát sinh cấn hai lần hoặc chi hai lần khi gửi lại | L4, PA41–43 |
| NT18 | Điểm trước 1200, nhập mới 1100 hoặc đổi mã mà chưa có sự kiện thay/reset được xác minh | Từ chối tự tính; không lượng âm/không tự reset 0. Khai báo thay công tơ đúng căn cứ thì dùng từng khoảng như NT50 | L2, PA28/57 |
| NT19 | A trả tại 1000; phòng trống đến lúc B nhận tại 1010; B trả tại 1100 | 10 đơn vị giai đoạn trống thuộc chủ nhà; B chịu 90, không chịu cả 100 | L1/L2, PA24/28/32 |
| NT20 | Đủ 3 kỳ lịch sử, baseline tương đương 9 m³/30 ngày; khoảng đang xét 6 m³/10 ngày, ngưỡng 50% | Theo phương pháp đề xuất tăng 100%/ngày, tạo cảnh báo. Khoảng dưới số ngày tối thiểu báo chưa đủ điều kiện so sánh | L2, PA30/31 |
| NT21 | Cư trú [01/02/2028,16/02/2028), giá 2.900.000 | 15 ngày trong tháng 29 ngày, tiền phòng 1.500.000; không chia cố định 30 | L2, PA32 |
| NT22 | Đã phát hành, sau đó đổi giá niêm yết/tên dịch vụ/điều khoản hợp đồng hiện hành | Hóa đơn và hợp đồng bản ký cũ giữ đúng nội dung gốc; không phát sinh giá/formula mới qua JOIN danh mục thay đổi | L2/L5, PA14/20/25/35 |
| NT23 | Xóa nháp để sửa chỉ số, nhập lý do rồi lập lại | Audit nháp cũ còn truy theo hợp đồng/kỳ; lịch sử chỉ số có trước/sau/người/thời gian/lý do, giữ người ghi gốc | L2, PA07/29/33 |
| NT24 | STAFF chỉ được giao phòng 203 thử đọc/thu/xuất PDF phòng 204; đồng thời tiếp nhận khách mới cho 203 | 204 bị chặn ở tất cả đường truy cập; 203 tiếp nhận được theo D03, không buộc khách phải từng có hợp đồng trong khu | L1, PA06/15/18/39 |
| NT25 | Người ở cùng có tài khoản nhưng chưa có quyền tài chính; người liên hệ đã bị thay; bên ký còn trách nhiệm | Người ở được báo sự cố của mình, không xem nợ/CCCD cả phòng; liên hệ cũ mất quyền theo cấp; bên ký vẫn tra cứu chứng từ cũ đúng quyền, không thấy hợp đồng người mới | PA17/22/35/40/45 |
| NT26 | Một thông báo gửi hai tài khoản, một người đọc; job nhắc chạy lại cùng mốc | Chỉ người đó có read_at; người kia vẫn chưa đọc. Không có hai thông báo cùng sự kiện/người nhận/mốc nhắc | PA48/49 |
| NT27 | Dùng mã khôi phục đã hết hạn, đã dùng, hoặc thử vượt giới hạn | Từ chối; reset hợp lệ thu hồi phiên; DB/log không chứa mã thô hoặc mật khẩu | PA04 |
| NT28 | D13: hóa đơn 3.000.000 đã thu đủ, điều chỉnh giảm 500.000 | Bản gốc nguyên; có nghĩa vụ hoàn 500.000. Chỉ chứng từ thực chi/đảo đúng nguồn làm giảm nghĩa vụ; không xóa/sửa giao dịch cũ | L6, PA37 |
| NT29 | Hóa đơn 3.680.000, thu mới 2.000.000, cấn 1.680.000; cấn thêm 200.000 cho bồi thường | Hóa đơn hết nợ; tiền mới thu hóa đơn vẫn 2.000.000, cấn tổng 1.880.000; bồi thường trình bày riêng, không cộng hai lần vào hóa đơn | PA40/42/50 |
| NT30 | Một phòng có trong danh mục đủ tháng 30 ngày, cho thuê 15 ngày rồi trống | Lấp đầy theo phòng-ngày là 50%; số chụp cuối tháng có thể là 0%, phải ghi đúng tên chỉ số và mẫu số, không trộn hai khái niệm | PA10/50 |
| NT31 | A chỉ là liên hệ được ủy quyền, đổi liên hệ sang B trong cùng hợp đồng | Có lịch sử vai trò/quyền từ mốc đổi, không quyết toán, không đổi bên chịu nợ/cọc và không chốt lại công tơ vô cớ | L5, PA22 |
| NT32 | Cha/mẹ ký và trả tiền nhưng không ở; hai người con thực ở | Chỉ tính 2 người cư trú, không cộng người ký thành người thứ 3; quyền xem/thanh toán theo cấp và nghĩa vụ đã xác nhận | PA18/19/22 |
| NT33 | Gia hạn từ 01/11, giá tăng 3.000.000 → 3.300.000, cọc cam kết tăng 500.000 chưa nộp | Tháng 10 giữ giá 3.000.000; tháng 11 dùng 3.300.000 theo phụ lục. Cọc còn phải bổ sung 500.000, cọc thực giữ chưa tự tăng | L5, PA21/38 |
| NT34 | Nhận thật 5.000.000 gồm cọc 2.000.000, trả nợ 2.700.000, dư 300.000 | Phiếu thu 5.000.000; nợ giảm 2.700.000; cọc 2.000.000; còn trả trước/chưa phân bổ 300.000. Không cộng tiền gốc và phân bổ thành thu mới | L7, PA54/55 |
| NT35 | Nợ 3.000.000, khách chuyển thật 3.500.000 | Ghi đủ 3.500.000, giảm nợ tối đa 3.000.000; còn 500.000 được giữ/hoàn theo căn cứ, không bỏ khoản nhận hoặc làm nợ âm lẫn với cọc | L7, PA54–56 |
| NT36 | Khách nộp trước 9.000.000, mới có khoản phải thu 3.000.000 | Dùng 3.000.000, còn trả trước 6.000.000; các kỳ sau áp dần. Không tạo điện/nước chưa đo hoặc nhận cọc giả để chứa tiền | L2/L7, PA54/55 |
| NT37 | Một lần chuyển 4.000.000 trả nợ cũ 1.500.000 và kỳ mới 2.500.000 | Một phiếu thu, hai phân bổ; truy cả hai đích và số dư, không sinh hai lần thu đủ 4.000.000 | L7, PA39/54 |
| NT38 | Tiền vào 1.200.000 nhưng nội dung sai, chưa xác minh người/đích | Lưu đã nhận/chưa phân bổ; không đoán giảm nợ phòng có số tiền gần giống. Khi xác minh gán nguồn không thu lần nữa | L7, PA54/55 |
| NT39 | Khách tải ảnh chuyển 3.000.000 nhưng tài khoản chưa nhận tiền | Trạng thái đề nghị/chờ xác minh; nợ và tiền khả dụng chưa đổi. Tài khoản TENANT không tự xác nhận đã thu | L7, PA54 |
| NT40 | Nhận tiền thực tế 30/09, nhân viên nhập 02/10 | Giữ cả ngày nhận và thời điểm nhập; báo cáo dòng tiền theo ngày nhận, audit ghi 02/10. Kỳ đã khóa cần điều chỉnh có quyền, không sửa timestamp để giấu nhập muộn | PA07/50/54 |
| NT41 | Nợ 1.000, hai lần chuyển thật 600 khác nhau được xác minh | Ghi đủ tiền nhận 1.200; tổng giảm nợ tối đa 1.000, còn dư 200. Không bỏ giao dịch thật thứ hai theo quy tắc chống phân bổ vượt | L7, PA54/55 |
| NT42 | Đã trả tiền phòng tháng 09 là 3.000.000, trả và hết tính tiền từ 16/09 được xác nhận, điện cuối 200.000 | Tiền phòng đúng 1.500.000, điện 200.000, tổng nghĩa vụ 1.700.000; có giảm/phát sinh đúng nguồn, còn phải hoàn/trả trước 1.300.000. Bản tháng gốc giữ nguyên | L4/L6, PA37/41/56 |
| NT43 | Báo trả 16/09, sau đó xin hủy/đổi ngày trước bàn giao | Lưu thông báo cũ và quyết định mới; không tự kết thúc hợp đồng/hoàn cọc. Nếu đã nhận khách kế tiếp phải báo xung đột, không âm thầm đẩy lịch họ | PA51/52 |
| NT44 | Hết hạn/khách mới dự kiến nhận 01/10, người cũ thực tế chưa giao phòng đến 03/10 | Phòng không tự trống ngày 01/10; chưa bàn giao mới, báo xung đột. Thời gian/giá tiếp tục giữ phòng được xác minh/phê duyệt, không tự coi đã gia hạn | PA18/58 |
| NT45 | Chuyển đồ đi ngày 15 nhưng đến 20 mới giao chìa khóa/quyền thuê; thỏa thuận tính tiền đến 20 | Không lấy ngày chuyển đồ làm mốc miễn 5 ngày. Ghi ngày rời chỗ/ngày giao và nghĩa vụ có căn cứ riêng | L4, PA41/51 |
| NT46 | Khách về quê 10 ngày nhưng vẫn giữ phòng, không có thỏa thuận giảm | Không tự trừ 10 ngày tiền phòng hoặc đóng cư trú; phí dịch vụ xử lý theo chính sách từng loại, không theo điểm danh ngủ đêm | PA19/27/32 |
| NT47 | Giao trả/nhận mới cùng ngày, số đo tại mốc bàn giao; hai bên dùng phòng ở hai khoảng giờ khác nhau | Chuỗi chỉ số theo mốc thời gian, không tính hai lần; tiền phòng theo quy tắc ngày đã xác nhận, không suy một quy tắc tính tiền giờ chưa có | L1/L4, PA24/28 |
| NT48 | Chuyển ngày 16/09: phòng cũ 3.000.000, phòng mới 3.600.000; không thuê đồng thời | Khoản chia ngày là 1.500.000 + 1.800.000 = 3.300.000; chỉ số/tài sản hai phòng riêng, không dùng chỉ số cũ của phòng A làm đầu phòng B | L8, PA53 |
| NT49 | Duyệt chuyển 800.000 cọc từ hợp đồng/phòng cũ sang mới cùng bên có quyền | Nguồn −800.000, đích +800.000, tiền thu/chi thật mới bằng 0; một đầu lỗi thì cả hai rollback. Không tự chuyển nợ cũ | L8, PA42/53 |
| NT50 | Thay công tơ: cũ 1000 → 1080, mới mở 5 và chốt 35 | Lượng thực 80 + 30 = 110; có sự kiện lắp/tháo/chứng cứ. Không lấy 35−1000 hoặc ép đầu công tơ mới bằng đầu cũ | PA28/57 |
| NT51 | Không đọc được chỉ số khi khách trả/công tơ hỏng | Trạng thái thiếu căn cứ/ước tính nếu được duyệt; không giả số đo thật, không tự chia chủ nhà/khách. Khi có số thật, đối soát bằng nguồn điều chỉnh, không tính lặp | PA28/37/41/57 |
| NT52 | Nhóm cũ một người, nhóm mới bốn người; lượng/ngày tăng | Hiển thị thay đổi cư trú, baseline tham khảo/không tương đương; cảnh báo yêu cầu kiểm tra, không kết luận lỗi hoặc tự cộng phạt | PA19/30 |
| NT53 | Baseline bằng 0 hoặc chưa đủ lịch sử | Không chia cho 0; trạng thái chưa đủ căn cứ so sánh/quy tắc ngưỡng tuyệt đối. Giá/lượng đủ vẫn phát hành được, không buộc có ba tháng mới thu tiền | PA30/34 |
| NT54 | Một xe đăng ký nửa tháng, giá vé tháng 60.000; đối chiếu chế độ cả kỳ với chia ngày | Chính sách cả kỳ thu 60.000; nếu thỏa thuận chia 15/30 thì 30.000. Hiển thị cơ sở tính, không dùng một công thức xe-ngày cho mọi vé | PA25/27/32 |
| NT55 | Giảm 300.000 tiền thuê một kỳ vì thỏa thuận sửa phòng, sau khi đã phát hành | Có chứng từ giảm 300.000 và người duyệt; giữ giá/bản gốc, không xóa phiếu thu đã nhận hoặc giảm cọc cam kết để bù | PA37/60 |
| NT56 | Người thuê tự ứng sửa 400.000, chủ nhà đồng ý hoàn/khấu trừ | Ghi chi phí/nghĩa vụ hoàn được duyệt, hoàn thật hoặc bù trừ có căn cứ; không ghi giả khách đã chuyển thêm 400.000 tiền phòng | L3/L6, PA46/56/60 |
| NT57 | Cọc 3.000.000, nợ thuê đủ căn cứ 1.000.000, bồi thường 600.000 đang tranh chấp | Có thể chốt/cấn 1.000.000 đủ điều kiện; 600.000 không tự thành bồi thường đã thu. Nếu có căn cứ giữ 600.000 thì theo dõi giữ/rá lại, phần có thể hoàn 1.400.000 xử lý riêng | L3/L4, PA42–44 |
| NT58 | Rò nước/chập điện khẩn, chưa biết thiết bị hoặc bên trả chi phí | Ghi vị trí/người báo và điều phối ngay theo quyền; bổ sung thiết bị/chi phí sau, không đợi quyết toán bồi thường mới xử lý | L3, PA45/46 |
| NT59 | Khách đã trả nhưng phòng cần vệ sinh/sửa chưa an toàn để nhận mới | Không hiển thị sẵn sàng nhận khách chỉ vì đã kết thúc hợp đồng; tách trống về cư trú với sẵn sàng khai thác, có người xác nhận hoàn tất | PA10/24/53 |
| NT60 | Sự cố đã hoàn thành, sau đó tái phát; hoặc nhiều người báo cùng một lỗi | Mở lần xử lý mới/liên kết nguồn, giữ lịch sử cũ; không nhân đôi khoản chi/bồi thường chỉ vì có nhiều người báo | PA45/46 |
| NT61 | Nhận giữ chỗ 500.000 rồi hủy theo điều kiện được hoàn | Còn nghĩa vụ hoàn 500.000, chưa ghi chi khi chỉ hủy; sau thực chi mới hết chờ hoàn. Không tự ghi mất cọc/chuyển thành doanh thu | PA59/56 |
| NT62 | Giữ chỗ 500.000 được chuyển sang cọc bảo đảm khi ký, cọc cam kết 2.000.000 | Cọc thực nhận từ nguồn chuyển 500.000, còn cần 1.500.000; tổng thu thật vẫn 500.000, không tạo lần nhận tiền thứ hai | PA18/38/59 |
| NT63 | Đề nghị hoàn đã duyệt, lệnh chuyển thất bại/chưa trừ tài khoản; người dùng bấm lại | Vẫn chờ thực chi, số dư không bị trừ hai lần; chỉ một giao dịch chi thật được xác nhận, lưu lần thất bại để tra soát | PA43/56 |
| NT64 | Phân bổ nhầm phiếu thật sang hóa đơn khác rồi phát hiện | Đảo phân bổ sai có liên kết, giữ nguyên phiếu nhận tiền; phân bổ đúng sau kiểm tra quyền/nguồn, không nhận tiền lại hoặc xóa dấu sai | PA07/39/55 |
| NT65 | Khóa/thu hồi nhân viên đã ghi tiền hoặc thay vị trí một tài sản | Quyền hiện tại bị thu hồi nhưng phiếu/audit vẫn giữ người đã thực hiện; thay vị trí có lịch sử, sự cố/biên bản trước vẫn chỉ đúng vị trí thời điểm đó | PA06/07/24/46 |

## 10. Đối chiếu tiêu chí chấm và thứ tự làm sau khi chốt

### 10.1. Minh chứng cần chuẩn bị

| Tiêu chí người dùng cung cấp | Tỷ trọng | Phần phương án đáp ứng / việc còn phải làm |
| --- | --- | --- |
| Tác nhân và giới hạn quyền | 10% | Mục 4 và NT24–25; sau chốt chuyển thành ma trận endpoint/dữ liệu được phép. |
| Use Case / User Story | 15% | PA01–PA60, truy tới R/B và dữ liệu; không dùng danh sách CRUD để thay các luồng nghiệp vụ. |
| Hai luồng phức tạp có nhánh lỗi | 15% | L2/L4 là hai luồng chính; L1/L3/L5/L6/L7/L8 xử lý tiếp nhận, sửa chữa, gia hạn/đổi vai trò, điều chỉnh, tiền nguồn và chuyển phòng. Hiện chỉ đặc tả văn bản, chưa vẽ. |
| Thực thể, thuộc tính, kiểu dữ liệu | 20% | Mục 7 và 8.2; sau chốt hoàn thiện từ điển dữ liệu từng thực thể. |
| PK/FK, tránh mồ côi | 15% | Mục 8.1–8.2, quy tắc vòng đời/xóa; cần schema và kiểm chứng sau triển khai. |
| 1–1, 1–N, N–N | 15% | Mục 8.1; ghi đúng tùy chọn/bắt buộc, bảng trung gian và thời gian. |
| 2NF/3NF | 10% | Mục 8.3–8.4; sau chốt phải kiểm tra toàn bộ phụ thuộc của schema cuối, ghi rõ snapshot/ngoại lệ. |

Đây là kế hoạch minh chứng theo tiêu chí, không là dự báo điểm và không khẳng định “đúng và đủ” sau một lần đọc hoặc chỉ vì test đạt.

### 10.2. Thứ tự đề xuất, chỉ thực hiện khi được duyệt

1. Chốt D01–D24, nhất là phân biệt vai trò/cư trú, kỳ từng khoản, phiếu thu/phân bổ, gia hạn có giá, trả/quá hạn/chuyển phòng, cọc/tiền dư/ước tính và tranh chấp. Các lựa chọn cũ trong D02/D07–D14/D18 đã được sửa sau rà thực tế, chưa coi là anh đã duyệt.
2. Ghi phạm vi được duyệt thành yêu cầu; khóa các công thức, trạng thái, thời điểm và ca nghiệm thu tương ứng. Bỏ hoặc sửa những đề xuất anh không chọn.
3. Hoàn thiện mô hình logic và từ điển dữ liệu, chứng minh khóa/phụ thuộc từng bảng, rồi mới vẽ ERD và cập nhật DBML/SQL.
4. Đánh giá chuyển đổi trên bản sao DB: giữ V1–V10 đã áp dụng; nếu triển khai thì dùng migration tiến tiếp, đối soát từng khoản/cư trú, không sửa checksum hoặc chạy lại script tạo DB lên dữ liệu đang dùng.
5. Chỉ lấy lại dữ liệu/chức năng từ `prototype_v9` sau khi xác minh còn phù hợp. Archive không có các sự kiện mới sau V10; không thể khôi phục nghiệp vụ bằng cách đảo schema đơn giản.
6. Không suy số cọc đã nhận từ cọc ghi trong hợp đồng khi chuyển đổi. Trường hợp thiếu chứng từ phải có đối soát và số dư mở sổ được xác nhận, ghi đúng nguồn/ngày xác minh; không tạo giao dịch thu tiền giả tại ngày migration. Nếu dùng số dư mở sổ, sổ cọc phải cộng nguồn này riêng và báo cáo không coi nó là tiền mới nhận trong kỳ.
7. Triển khai backend/API theo quyền và transaction đã duyệt; chạy ca DB, tích hợp, đồng thời và bảo mật phù hợp. Kết quả test 17 bảng trước đây chỉ làm mốc so sánh, không thay nghiệm thu NT01–NT65.
8. Khi mô hình/luồng đã thống nhất mới cập nhật nội dung thuyết trình, ERD và slide; giữ nhóm/sáu thành viên và chờ thông tin phân công thật. Không chạy nhầm bộ sinh slide 29 bảng.

### 10.3. Những điều còn chưa được xác nhận

- Toàn bộ V1 có phải phạm vi cuối thầy yêu cầu hay không; nhận xét của thầy chưa chỉ ra lỗi cụ thể. Danh mục này chỉ xác minh V1 có viết gì và bản hiện tại biểu diễn đến đâu.
- D01–D24, các tham số cảnh báo chưa hiệu chỉnh bằng số liệu, cách chốt/thu thực tế và việc cho phép giảm một số mục V1.
- Dữ liệu thực tế về nhận cọc/hoàn cọc, tình trạng đồ lúc bàn giao, ngày vận hành phòng và lịch sử cũ: không thể tự tái tạo nếu trước đây chưa lưu.
- Đã đề xuất tách người ký/người liên hệ/người ở và thu gộp/trả trước; còn cần xác nhận với cơ sở thật. Chưa tự kết luận trách nhiệm pháp lý chung của nhiều người ký, quyền giữ/khấu trừ tiền trong tranh chấp, thời hạn báo trả/hoàn tiền hoặc tiêu chuẩn bồi thường.
- Trạng thái DB đang chạy và kết quả các ca mới chưa kiểm chứng runtime. Trong lần làm này chỉ đọc và viết tài liệu; không chạy migration, server, build ứng dụng hoặc bộ sinh slide.

Tài liệu sau rà thực tế có 24 quyết định, 30 mục đối chiếu V1, 13 mục bổ sung, 60 Use Case và 65 ca nghiệm thu đề xuất. Đếm mã/liên kết và mô phỏng số học không thay nghiệm thu ứng dụng. Những thay đổi có sẵn ở README, script chạy local và các tệp chưa theo dõi được bảo toàn; lần rà này chỉ cập nhật tài liệu phân tích, không commit.

## 11. Rà soát áp dụng thực tế và những giới hạn còn lại

### 11.1. Nguồn đối chiếu ngoài repository

Đối chiếu ngày 19/10/2026. Các nguồn dưới đây là tài liệu do chính đơn vị sản phẩm/cơ quan công bố. Một sản phẩm có quy trình là bằng chứng tình huống được họ hỗ trợ; không chứng minh mọi chủ trọ Việt Nam vận hành giống vậy. Các công thức và ca NT trong tài liệu này là phương án do nhóm cần chốt, không phải sao chép hoặc khẳng định là tiêu chuẩn của các nhà cung cấp.

| Nguồn | Điều nguồn hỗ trợ / cách dùng trong phân tích |
| --- | --- |
| [TenantCloud — Gia hạn hợp đồng](https://support.tenantcloud.com/en/articles/11942552-how-do-i-renew-a-lease) | Quy trình gia hạn cho phép cập nhật giá và điều kiện kỳ tiếp theo. Dùng để bác giả định chỉ được gia hạn cùng giá; không bắt đồ án sao chép cách TenantCloud tạo lease kế tiếp. |
| [TenantCloud — Thanh toán nhiều hóa đơn](https://support.tenantcloud.com/en/articles/11931805-how-do-i-pay-multiple-invoices) | Có thanh toán nhiều khoản trong một lần, số tiền từng khoản có thể tùy điều kiện. Dùng làm căn cứ tách tiền nguồn/phân bổ; đồ án vẫn ghi nhận thủ công. |
| [TenantCloud — Trả tiền trước](https://support.tenantcloud.com/en/articles/11931823-how-do-i-pay-rent-in-advance) | Có quy trình trả trước kỳ tương lai. Đồ án đề xuất số dư trả trước để không cần dựng số điện/nước tương lai; không khẳng định đây là đúng cách lưu DB của TenantCloud. |
| [TenantCloud — Hoàn tiền cọc](https://support.tenantcloud.com/en/articles/11955779-how-do-i-return-a-deposit) | Phân biệt ghi nhận hoàn và giao dịch thực hiện hoàn, có thể hoàn một phần. Củng cố việc không coi số tính ra cần hoàn là đã chi. |
| [Buildium — Notice to Vacate](https://help.buildium.com/hc/s/article/Notice-to-Vacate) | Theo dõi đề nghị trả, ngày dự kiến, thay đổi/hủy và lịch sử. Nội dung đối chiếu từ bản được công cụ tìm kiếm lập chỉ mục; trang trực tiếp có lúc trả CSS Error. Không lấy yêu cầu báo trước theo địa phương của họ làm quy định ở Việt Nam. |
| [NhaTroPro — Tính tiền điện nước/dịch vụ](https://nhatropro.com/tinh-nang/tinh-tien-dien-nuoc) | Mô tả sổ số, nhật ký sửa, dịch vụ theo phòng/người/xe. Dùng đối chiếu sự khác nhau về cơ sở tính; không coi tính năng quảng bá là bằng chứng nghiệm thu sản phẩm hoặc bảng giá hợp pháp cho đồ án. |
| [SmartHome — Trung tâm hướng dẫn](https://www.phongtrovietnam.com/support) | Danh mục có chuyển phòng, tất toán giữa kỳ, công tơ chung và vé xe giữa kỳ. Chỉ dùng nhận diện phạm vi có thể phát sinh, không suy rằng các mô hình này đều phải được triển khai trong đồ án. |
| [UBND Hà Nội — Chỉ thị thực hiện giá bán lẻ điện tại điểm cho thuê nhà, bản EVNHANOI công bố năm 2026](https://evnhanoi.vn/assets/FileUpload/TaiLieuGiaBanDien/CT04.pdf) | Nguồn chính thức cho thấy áp giá ở nhà cho thuê cần đúng đối tượng/quy định, không phải tùy ý nhập một con số. Đây là tài liệu địa bàn Hà Nội; triển khai ở nơi khác phải xác định nguồn áp dụng tương ứng. Không chốt biểu giá toàn quốc trong phân tích này. |

Các mức 3.500 đ/kWh, 20.000 đ/m³ hoặc tiền thuê trong ví dụ chỉ phục vụ số học. Cần giữ căn cứ nguồn giá, chế độ áp dụng và thời điểm hiệu lực; không kết luận giá thực tế hợp lệ vì phần mềm cho phép cấu hình. Chứng từ mang tên “hóa đơn” ở đây là chứng từ tính tiền của hệ thống; chưa có căn cứ tuyên bố đã tích hợp hóa đơn điện tử thuế hoặc quy trình đăng ký cư trú với cơ quan nhà nước.

### 11.2. Luồng mẫu xuyên suốt sau khi sửa giả định

Dữ liệu minh họa: thuê nguyên phòng từ 01/09, giá 3.000.000/tháng, cam kết cọc 3.000.000. Thỏa thuận chia ngày và cho chấm dứt nghĩa vụ từ ngày bàn giao 16/09; không có tranh chấp. Dòng tháng 09 thu trước tiền phòng, điện/nước chốt sau.

| Mốc | Việc thật và dữ liệu ghi nhận | Kết quả đối soát |
| --- | --- | --- |
| Trước nhận phòng | Nhận một lần 5.000.000, xác minh rồi phân bổ 2.000.000 cọc và 3.000.000 trả trước | Thu thật 5.000.000; cọc thực có 2.000.000, còn thiếu cam kết 1.000.000; phải có quyền chấp nhận nhận phòng thiếu cọc. |
| 01/09 | Phát hành hóa đơn tiền phòng 3.000.000; dùng 3.000.000 trả trước | Nợ tiền phòng 0, trả trước còn 0; điện chưa đến kỳ không được giả có số để bắt đủ dòng. Bàn giao giữ mốc chỉ số đầu. |
| Trong kỳ | Thay người liên hệ nhưng không đổi bên ký; có người vắng tạm thời | Lưu quyền/vai trò từ–đến, không quyết toán hoặc miễn tiền phòng tự động. Cư trú/số người thu phí theo dữ kiện đã ghi và chính sách. |
| Trước trả | Khách gửi thông báo, hai bên chốt lịch kiểm tra/bàn giao | Chưa giải phóng phòng, chưa chi tiền hoàn chỉ vì nhận thông báo. |
| 16/09 | Bàn giao và xác nhận ngày hết tính tiền, chốt điểm đo; phòng cần vệ sinh trước nhận mới | Tiền phòng đúng 1.500.000. Có hao mòn chủ nhà chịu 100.000, điện/nước cuối 180.000, bồi thường được xác nhận 200.000. Không coi hao mòn là tiền khách nợ. |
| Chốt các khoản | Chứng từ giảm tiền phòng 1.500.000, khôi phục phần tiền đã dùng về trả trước; bổ sung 180.000 điện/nước và 200.000 bồi thường có nguồn | Tổng nghĩa vụ khách 1.500.000 + 180.000 + 200.000 = 1.880.000. Hóa đơn tháng gốc vẫn còn lịch sử; không tạo một hóa đơn tháng 09 thứ hai để thu lại. |
| Phân bổ quyết toán | Dùng 180.000 từ tiền trả trước đã khôi phục và 200.000 từ cọc theo xác nhận | Trả trước còn 1.320.000; cọc còn 1.800.000; tổng tiền phải hoàn 3.120.000. Không cộng 1.000.000 cọc chưa nhận thành phải thu mới sau kết thúc. |
| Chưa chi / chi một phần | Mới duyệt hoàn thì đã hoàn 0. Thực chi 1.000.000, phân bổ 800.000 hoàn cọc + 200.000 hoàn trả trước | Còn cọc 1.000.000 và trả trước 1.120.000; tổng cần hoàn tiếp 2.120.000. Tổng thu 5.000.000 − nghĩa vụ 1.880.000 − đã hoàn 1.000.000 = 2.120.000. |

Mẫu này buộc mô hình xử lý được tiền nhận trước, thu thiếu cọc, khác kỳ dịch vụ, liên hệ khác nghĩa vụ, trả sớm sau phát hành, bồi thường khác chi phí và hoàn từng phần. Nếu bỏ phiếu thu/phân bổ hoặc khoản điều chỉnh/phát sinh thì không thể đi hết mẫu mà vẫn giữ lịch sử đúng.

### 11.3. Trạng thái nên tách khi triển khai

| Trục nghiệp vụ | Phân biệt cần giữ | Hậu quả nếu gộp |
| --- | --- | --- |
| Thỏa thuận thuê | Nháp, đã ký/đang hiệu lực, hết hạn, chấm dứt/hủy theo căn cứ | Hết hạn giấy tờ bị dùng làm bằng chứng phòng đã trống hoặc khách đã hết nghĩa vụ. |
| Kế hoạch và chiếm giữ thực tế | Thông báo/dự kiến trả; còn giữ phòng; đã bàn giao; quá hạn cần xử lý | Giao phòng mới cho người đến đúng lịch trong khi người cũ chưa giao; hoặc không nhận được lịch tương lai vì một khoảng ở đang mở. |
| Khai thác phòng | Đang có người giữ, trống nhưng cần chuẩn bị/bảo trì, sẵn sàng bàn giao | Phòng trống về người ở nhưng chưa an toàn vẫn được đánh dấu sẵn sàng. |
| Tiền | Đề nghị/chờ xác minh; tiền nhận thật; đã phân bổ; tiền chưa dùng; đề nghị hoàn; đã chi | Ảnh chuyển khoản thành tiền có sẵn; tiền thừa bị bỏ; đề nghị hoàn bị báo hoàn xong. |
| Khoản phải thu | Đã đủ căn cứ, chờ xác minh, tranh chấp; số chưa trả/trả một phần/đủ được tính từ sổ | Tranh chấp bị tự xóa nợ hoặc tự cấn; một trạng thái PAID nhập tay che lệch tiền. |
| Sửa chữa | Tiến độ việc sửa, tính khẩn, chi phí, xác định bên chịu, thu/hoàn riêng | Phải chờ thống nhất bồi thường mới sửa lỗi nguy hiểm; hoàn thành sửa bị hiểu là đã thu đủ. |

Đây là các khái niệm độc lập, không yêu cầu tạo một bảng hoặc cột trạng thái cho mỗi ô. Thông tin đã suy ra chắc chắn từ ngày/sự kiện/giao dịch dùng view; chỉ lưu trạng thái khi có một quyết định nghiệp vụ độc lập cần lưu.

### 11.4. Mức ưu tiên và giới hạn hỗ trợ đề xuất

**Cần chốt trước khi thiết kế:** vai trò/cư trú, khoảng tính tiền, phiếu thu/phân bổ, cọc thực giữ, thay công tơ có căn cứ, gia hạn giá, báo trả/quá hạn, điều chỉnh sau phát hành, chuyển phòng và quyền ủy nhiệm. Đây là các điểm có thể làm sai tiền/lịch phòng trong những tình huống đã nêu; khuyến nghị đưa vào phạm vi cơ sở của bản hướng thực tế, vẫn cần anh duyệt.

**Chủ trọ phải chọn chính sách:** thu trước/sau; từng loại dịch vụ chia ngày hay thu cả kỳ; thời hạn thông báo, điều kiện kết thúc tính tiền, ngày hiệu lực giá; được dùng cọc khi nào và vào khoản gì; hạn mức STAFF; giữ chỗ/hủy, miễn giảm, khoản đang tranh chấp, căn cứ ước tính khi thiếu số. Không có một lựa chọn đúng cho mọi nhà trọ để AI tự chốt thay.

**Cần công khai giới hạn thay vì giả đã hỗ trợ:** thuê từng giường, nhiều phòng chung công tơ, định mức/bậc giá phức tạp chưa có căn cứ tính, tự động đối soát ngân hàng, chữ ký số, thuế/hóa đơn điện tử, đăng ký cư trú tự động, nhiều đồng tiền và chuyển nghĩa vụ giữa các bên không có thỏa thuận. Nếu muốn đưa một mục vào phạm vi, phải bổ sung dữ liệu/quy trình/ca nghiệm thu tương ứng; không chỉ thêm một cột hoặc nhãn trên slide.

Độ sát thực tế cuối cùng cần kiểm chứng bằng tài liệu đã ẩn danh của một cơ sở: hợp đồng và phụ lục giá; hai kỳ hóa đơn kèm ngày đo/ngày thu; một lần nhận tiền nhiều khoản; một lần trả phòng/hoàn thật; cách giao quyền cho nhân viên. Không cần sao chép CCCD, tài khoản hay mật khẩu thật vào repo. Khi chưa có bộ này, kết luận đúng mức là **đã rà logic và mở rộng tình huống**, chưa phải đã nghiệm thu vận hành tại nhà trọ.

### 11.5. Kiểm tra đã thực hiện ở lần rà này

- Đối chiếu lại các giả định trong tài liệu và nguồn vận hành ở mục 11.1; cập nhật các phần quyền, luồng, dữ liệu và ca nghiệm thu cùng nhau.
- Tính độc lập bằng số thập phân chính xác cho **20 ví dụ số học**, gồm chia ngày/tháng nhuận, ngày-người/xe-ngày, cọc thiếu/vượt, hoàn từng phần, tiền dư/trả trước/thu gộp, chuyển cọc, thay công tơ và mẫu xuyên suốt mục 11.2: các giá trị kỳ vọng khớp. Đây là kiểm tra số học, không mô phỏng đầy đủ trạng thái/quyền/đồng thời của ứng dụng.
- Kiểm tra đủ chuỗi mã D01–D24, R01–R30, B01–B13, PA01–PA60 và NT01–NT65, không trùng/thiếu; 21 liên kết tệp nội bộ tồn tại. Các nguồn web có liên kết trực tiếp; giới hạn đọc Buildium đã ghi tại mục 11.1.
- So SHA-256 với mốc trước rà: chỉ tài liệu phân tích này thay đổi; **270 tệp có trước khác được giữ nguyên**. Không chạy SQL, migration, backend, trình duyệt ứng dụng hoặc bộ sinh slide; chưa có kết quả kiểm thử runtime cho phương án mới.

**Điểm dừng:** chờ anh chốt D01–D24. Chưa vẽ ERD, chưa sửa schema/migration/backend/slide; các ca NT là tiêu chí cho bản tương lai, không phải chức năng hiện hành đã chạy đạt.
