package com.hostel.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge(Exception e) {
        return ResponseEntity.status(413)
                .body(Map.of("message", "Ảnh phải nhỏ hơn hoặc bằng 6 MB"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("message", e.getReason() == null ? "Request failed" : e.getReason()));
    }

    private static final java.util.List<String[]> CONFLICT_MESSAGES = java.util.List.of(
            new String[] {"Unresolved verification or consumption alert",
                    "Không thể phát hành: còn khoản chờ xác minh hoặc cảnh báo điện nước chưa được kiểm tra"},
            new String[] {"Room capacity exceeded",
                    "Vượt quá sức chứa tối đa của phòng"},
            new String[] {"Contract period overlaps",
                    "Thời hạn hợp đồng bị chồng lấn với hợp đồng khác của phòng này"},
            new String[] {"Active contract overlaps",
                    "Thời hạn hợp đồng bị chồng lấn với hợp đồng khác của phòng này"},
            new String[] {"Contract conflicts with held reservation",
                    "Thời hạn hợp đồng xung đột với lượt giữ chỗ đang hiệu lực"},
            new String[] {"Room is still possessed",
                    "Phòng đang có hợp đồng khác nắm giữ thực tế"},
            new String[] {"Room is not ready for handover",
                    "Phòng chưa ở trạng thái sẵn sàng (READY) để giao phòng"},
            new String[] {"Meter interval is outside actual possession",
                    "Khoảng đo điện nước nằm ngoài thời gian nhận và trả phòng thực tế"},
            new String[] {"Possession change conflicts with assigned consumption",
                    "Thời điểm nhận/trả phòng xung đột với khoảng đo điện nước đã gán cho hợp đồng"},
            new String[] {"Insufficient available funds",
                    "Số dư khả dụng trong quỹ không đủ (có thể một phần đang bị tạm giữ)"},
            new String[] {"Allocation exceeds eligible debt",
                    "Số tiền phân bổ vượt quá công nợ còn lại hoặc chứng từ chưa phát hành"},
            new String[] {"Confirmed record is immutable",
                    "Bản ghi đã xác nhận, không thể sửa hoặc xóa trực tiếp"},
            new String[] {"Issued documents are immutable",
                    "Chứng từ đã phát hành, không thể sửa hoặc xóa; hãy lập chứng từ điều chỉnh"},
            new String[] {"Issued billing lines are immutable",
                    "Dòng hóa đơn đã phát hành, không thể sửa hoặc xóa; hãy lập chứng từ điều chỉnh"},
            new String[] {"Approved terms are immutable",
                    "Phiên bản điều khoản đã ký duyệt, không thể sửa; hãy tạo phụ lục gia hạn"},
            new String[] {"Reading must be monotonic",
                    "Chỉ số công tơ không được nhỏ hơn mốc đo trước hoặc lớn hơn mốc đo sau"},
            new String[] {"Measurement conflicts with an earlier or later reading",
                    "Chỉ số công tơ xung đột với mốc đo trước hoặc sau đó trên cùng công tơ"},
            new String[] {"Consumption interval overlaps",
                    "Khoảng thời gian đo bị chồng lấn với khoảng đo đã ghi trên cùng công tơ"},
            new String[] {"Hold exceeds fund balance",
                    "Số tiền tạm giữ vượt quá số dư quỹ hiện có"},
            new String[] {"Issue date or missing lines prevents issue",
                    "Ngày phát hành ở tương lai hoặc chứng từ chưa có dòng thu nào"},
            new String[] {"uq_external_cash",
                    "Mã tham chiếu ngân hàng này đã được ghi nhận cho một phiếu thu đã xác nhận"},
            new String[] {"uq_handover_item_original",
                    "Món đồ này đã được đối chiếu trong biên bản trả phòng"});

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException e) {
        String cause = e.getMostSpecificCause() != null && e.getMostSpecificCause().getMessage() != null
                ? e.getMostSpecificCause().getMessage()
                : "";
        for (String[] pair : CONFLICT_MESSAGES) {
            if (cause.contains(pair[0])) {
                return ResponseEntity.status(409).body(Map.of("message", pair[1]));
            }
        }
        return ResponseEntity.status(409)
                .body(Map.of("message", "Dữ liệu trùng lặp, tham chiếu không hợp lệ hoặc xung đột nghiệp vụ"));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        IllegalArgumentException.class,
        java.time.DateTimeException.class
    })
    ResponseEntity<?> invalid(Exception e) {
        String message =
                e instanceof MethodArgumentNotValidException v
                        ? v.getBindingResult().getFieldErrors().stream()
                                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                                .reduce((a, b) -> a + "; " + b)
                                .orElse("Invalid request")
                        : "Invalid request values";
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
