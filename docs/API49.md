# API hiện hành — V13 / 49 bảng

Tiền tố `/api`. Xác thực Bearer như trước; tài khoản ADMIN/STAFF/TENANT còn hiệu lực mới được dùng API. Mặc định ứng dụng profile `dev`/`prod` kích hoạt `model49`. Các controller V10 chỉ dùng cho kiểm thử lịch sử.

## Hợp đồng và sử dụng phòng

| Method | Đường dẫn | Ý nghĩa |
| --- | --- | --- |
| POST | `/contracts` | Nháp: `code,roomId,tenantId,startDate,endDate,rentalPrice,deposit,paymentDay` |
| POST | `/contracts/{id}/activate` | Ghi ngày đã ký: `signedAt`; tạo quỹ cọc/trả trước, chưa tạo tiền |
| POST | `/contracts/{id}/renew` | Phụ lục: `effectiveFrom?,endDate,signedAt,reason,rentalPrice?,deposit?` |
| POST | `/contracts/{id}/members` | Người ở thực: `tenantId,effectiveFrom,effectiveTo?,relationship?` |
| POST | `/contracts/{id}/members/{member}/depart` | Ghi `effectiveTo` |
| POST | `/contracts/{id}/contacts` | Liên hệ: `tenantId,effectiveFrom,canViewBilling,reason` |
| POST | `/contracts/{id}/possessions` | Giao thực tế: `takenAt,reason?`; thiếu cọc cần quyền WAIVE_DEPOSIT |
| POST | `/contracts/{id}/move-out-notices` | Đề nghị: `requestedDate,reason,supersedesNoticeId?` |
| POST | `/contracts/{id}/checkout` | Trả vật lý: `billingEnd,returnedAt,notes?` |
| POST | `/contracts/{id}/checkout/approve` | Duyệt: `reason,applyAdvance?,useDeposit?,pendingUtilityReason?` |
| POST | `/contracts/{id}/reconcile` | ADMIN xác minh hồ sơ cũ: `confirmed=true,reason` |
| GET | `/contracts/{id}` | Bản điều kiện, người ở, bên ký, liên hệ, quỹ và quyết toán |
| GET | `/contracts/{id}/settlement` | ADMIN/STAFF trong phạm vi: các khoản phải thu/điều chỉnh, nguồn cấn, tạm giữ, tiền hoàn thực tế và khoản chưa xác minh |
| GET | `/contracts/{id}/billing-calendar?period=2026-02` | Ngày chốt gần nhất trước/ngay ngày phát hành, ngày phát hành và hạn thanh toán theo bản điều kiện |
| GET | `/contracts/{id}/pdf?version=1` | Bản điều kiện theo phiên bản; ngày ký do người vận hành ghi nhận |

`deposit` khi lập/gia hạn là **cam kết**, không phải tiền thực nhận. Thời điểm chấp nhận ISO có offset hoặc ngày giờ địa phương Việt Nam; ưu tiên gửi `2026-10-26T10:00:00+07:00`.

Tiếp nhận khách: gửi **một trong hai** `tenantId` (hồ sơ đã xác minh trong phạm vi) hoặc `tenant={fullName,identityNumber?,phone?,email?}`. STAFF không được dùng payload khách mới để bỏ qua kiểm tra phạm vi của hồ sơ có sẵn. Trả phòng vật lý chỉ áp dụng hợp đồng ACTIVE đã ghi nhận giao phòng; không dùng checkout thay cho hủy hợp đồng nháp.

## Chứng từ và tiền

| Method | Đường dẫn | Payload chính |
| --- | --- | --- |
| POST | `/invoices` | `contractId,period` nhãn YYYY-MM; `issueDate?,dueDate?` |
| POST | `/billing-documents` | `contractId` hoặc `reservationId`, `code,documentType,issueDate,dueDate?,period?,relatedDocumentId?,reason` |
| POST | `/billing-documents/{id}/items` | `itemKind,direction,name,unitPrice,quantity,calculationBasis,serviceFrom,serviceTo` và FK nguồn khi có |
| POST | `/billing-documents/{id}/issue` | Phát hành và khóa đầu–chi tiết |
| POST | `/billing-items/{id}/meter-corrections` | `code,correctedClosingIndex,reason`; chỉ dòng công tơ gốc đã phát hành, cần quyền ADJUST |
| POST | `/invoices/{id}/payments` | `code,amount,method,cashAccountId?,occurredAt?,externalReference?,payerId?`; tiền dư vào ADVANCE |
| POST | `/cash-transactions` | `cashAccountId,code,amount,method,occurredAt,confirmed,contractId?,fundType?` |
| POST | `/cash-transactions/{id}/confirm` | Xác nhận tiền vào; số chưa rõ mục đích vào quỹ UNAPPLIED |
| GET | `/cash-transactions` | Phiếu tiền thật theo quyền |
| GET | `/fund-accounts` | Quỹ và số dư theo quyền quản lý |
| GET | `/contracts/{id}/funds` | Cọc, trả trước, số đang tạm giữ |
| POST | `/fund-entries` | `code,entryType,amount,reason,fromFundId?,toFundId?,debitItemId?,creditItemId?,originalEntryId?` |
| POST | `/fund-accounts/{id}/reconcile` | ADMIN: `verifiedBalance,reason`, chỉ quỹ PENDING |
| POST | `/fund-accounts/{id}/refund` | `cashAccountId,code,amount,method?,occurredAt,paid=true,reason,externalReference?` |
| GET/POST | `/fund-accounts/{id}/holds` | Tạo: `amount,reviewOn,reason,billingItemId?` |
| POST | `/fund-holds/{id}/release` | `reason` |
| POST | `/reservations/{id}/fund` | Mở quỹ giữ chỗ; nhận tiền bằng phiếu thu rồi chuyển từ UNAPPLIED |

Loại chứng từ: MONTHLY, FINAL_CHARGE, COMPENSATION, ADJUSTMENT, REIMBURSEMENT, OPENING_CHARGE. MONTHLY cần `period`; ADJUSTMENT cần `relatedDocumentId` và căn cứ.

Loại bút toán API chung: PAYMENT, TRANSFER, CREDIT_TO_FUND, ADJUSTMENT_RELEASE, REVERSAL. RECEIVE/REFUND/OPENING_BALANCE đi qua nghiệp vụ chuyên dụng. Đảo bút toán tiền thật chưa được hỗ trợ bằng REVERSAL chung; cần đối soát phiếu thu/chi và chứng từ thực tế riêng.

Ví dụ thu 1 triệu cọc, chưa trả hóa đơn:

```json
{
  "cashAccountId": 1,
  "contractId": 12,
  "fundType": "DEPOSIT",
  "code": "RC-20261026-001",
  "amount": 1000000,
  "method": "CASH",
  "occurredAt": "2026-10-26T10:00:00+07:00",
  "confirmed": true
}
```

`code` dùng chống gửi lại phiếu/bút toán cùng nội dung; mã đã dùng với nội dung khác bị từ chối. Không dùng lại mã thu của một hợp đồng cho hợp đồng khác.

Hoàn tiền cũng kiểm tra phương thức, thời điểm và tham chiếu ngân hàng khi gửi lại. Khi trả sớm, quyết toán lập điều chỉnh cho tiền phòng và dịch vụ PRORATED đã phát hành, dùng ngày-người thực ở cho dịch vụ theo người; FULL_PERIOD/ONE_TIME giữ chế độ thu đã chọn. Phần phân bổ dư trở lại đúng nguồn quỹ, chưa có nghĩa đã chi hoàn.

Từ V13, phiếu thu qua `/invoices/{id}/payments` lưu `payment_document_id`: không dùng lại cùng mã cho hóa đơn khác ngay cả trong cùng hợp đồng. Số tiền, phương thức, thời điểm và tham chiếu ngân hàng gửi lại phải khớp khoản gốc. Khi không gửi thời điểm, lần thử lại giữ thời điểm đã lưu. Hai mã yêu cầu khác nhau với cùng `externalReference` của tài khoản nhận không được tính tiền hai lần. Tiền thật và bút toán nhận tối đa hai chữ số thập phân; chỉ số tối đa ba chữ số, không âm.

Lập hóa đơn tháng được gửi lại cho cùng hợp đồng/kỳ và nhận lại chứng từ cũ, không tính lại đơn giá hoặc lượng dùng. Nếu gửi mã hoặc ngày khác bản đã có thì bị từ chối. Ngày cấu hình 29–31 được chặn trên ngày cuối tháng ngắn; ngày chốt là mốc cấu hình gần nhất không sau ngày phát hành, tính cả ngày chốt và chỉ lấy khoảng đo đã hoàn tất.

Ví dụ sửa dòng điện đã tính 50 kWh về 40 kWh, đơn giá gốc 3.500: gọi `/billing-items/{lineId}/meter-corrections` với `{"code":"CORR-001","correctedClosingIndex":40,"reason":"Đã đối chiếu ảnh công tơ"}` khi chỉ số đầu là 0. Backend tạo điều chỉnh giảm 35.000 theo giá gốc. Nếu đã thu đủ, phần phân bổ dư trở về đúng quỹ nguồn; không tạo phiếu chi hoàn. Chỉ số và hóa đơn gốc được giữ nguyên; một phiên bản điểm đo mới liên kết `correction_item_id` làm số đầu kỳ sau. Lần sửa tiếp theo tính chênh lệch với lượng đã điều chỉnh, không giảm lại toàn bộ từ đầu. Nếu đã có khoảng kế tiếp phụ thuộc điểm này, API từ chối toàn bộ giao dịch để yêu cầu đối soát cả chuỗi; chưa có thao tác tự động sửa dây chuyền các kỳ đã phát hành.

Phiếu chờ xác nhận vẫn chưa tăng quỹ. `/cash-transactions/{id}/confirm` đưa tiền vào UNAPPLIED; người quản lý phân loại qua TRANSFER. Khi gửi phiếu với `confirmed=true,contractId,fundType`, xác nhận và phân loại được thực hiện trong cùng giao dịch.

## Điện nước và cảnh báo

| Method | Đường dẫn | Ý nghĩa |
| --- | --- | --- |
| POST | `/meter-readings` | Nhập khoảng thực: `meterId,contractId?,startAt,endAt,oldIndex,newIndex`; bỏ contract là phần chủ nhà chịu |
| POST | `/meter-points` | `meterId,observedAt,readingValue,supersedesReadingId?,reason?` |
| POST | `/consumption-segments` | `startReadingId,endReadingId,contractId?,supersedesSegmentId?` |
| POST | `/consumption-segments/{id}/revise` | `newIndex,reason`; tạo điểm và khoảng thay thế, giữ lịch sử |
| POST | `/meters/{id}/replace` | `replacedAt,closingIndex,newMeterCode,openingIndex,reason` |
| GET | `/meter-readings`, `/alerts` | Danh sách theo quyền |
| PUT | `/alerts/{id}` | `status=CONFIRMED|DISMISSED,note`; không sửa lịch sử đã phát hành |

Chỉ số giảm trên cùng công tơ bị từ chối, kể cả ghi lùi ngày làm vượt chỉ số kỳ sau. Điểm đo phải nằm trong tuổi thọ công tơ và không ở tương lai. Khoảng đã gắn hóa đơn không sửa đè; bỏ nháp hoặc dùng chứng từ điều chỉnh cho hóa đơn đã phát hành. Khoảng thuộc khách thuê phải nằm trong thời gian nhận/trả thực tế; không được lùi ngày trả làm mất trách nhiệm của khoảng đã gán.

## Danh mục và vận hành

`GET /catalog/{resource}`, `GET /catalog/{resource}/{id}`, `POST /catalog/{resource}`, `PUT /catalog/{resource}/{id}`. Cập nhật cần `reason`. Backend dùng danh sách trường cho phép; không nhận tên bảng/cột SQL từ người dùng.

Resource: properties, rooms, tenants, assignments, staff-permissions, room-prices, room-operating-periods, asset-types, assets, asset-placements, reservations, parties, subscriptions, handovers, handover-items, violations, services, prices, meters, thresholds, maintenance-schedules, cash-accounts. Khóa payload dùng camelCase tương ứng từ điển dữ liệu. PRIMARY đã ký không được đổi qua CRUD; chuyển trách nhiệm bằng hợp đồng kế tiếp.

- Sự cố: `POST /maintenance-requests` chọn **một** `roomId`, `propertyId` hoặc `assetPlacementId`; thêm `title,description,priority,incidentType`. Người thuê phải là người đang cư trú để báo cho phòng.
- Tiến độ: `PUT /maintenance-requests/{id}` với `status,assignedTo?,expectedDate?,note`; chi phí thêm qua `POST /maintenance-requests/{id}/costs`.
- Lịch bảo trì: `POST /maintenance-schedules/{id}/complete` tạo phiếu cho lần đến hạn và dời lịch. Tên endpoint được giữ để tương thích; công việc vẫn phải xử lý tới COMPLETED riêng.
- Tệp: `POST /files/{kind}/{target}` multipart `file`; kind gồm room, asset, handover-item, meter-reading, maintenance-request, maintenance-cost, contract-term, billing-document, violation, fund-entry. GET cùng đường dẫn liệt kê; GET `/uploads/{key}` tải có quyền.
- Thông báo: `POST /contracts/{id}/announcements` với `code,title,body`; GET `/notifications`; PUT `/notifications/{id}/read`.
- Khôi phục: ADMIN POST `/users/{id}/reset-code` với `reason`; nhận code một lần. POST công khai `/auth/reset-password` với `resetId,code,newPassword`; không ghi code vào log/ảnh minh chứng.
- Báo cáo: dashboard, debts, revenue, revenue-quarterly, occupancy, occupancy-history, maintenance dưới `/reports`; PDF hóa đơn và báo cáo còn dùng.

Kết quả kiểm tra và giới hạn: [bản triển khai](TRIEN-KHAI-MO-HINH-49-BANG.md).
