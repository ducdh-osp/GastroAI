-- Toàn vẹn dữ liệu cảnh báo cấp cứu: trước giờ DB chỉ kiểm tra từng cột riêng lẻ (status, outcome,
-- patient_response thuộc danh sách cho phép), không kiểm tra các cột có KHỚP NHAU không.
-- Code hiện tại ghi đúng (mỗi UPDATE trong TriageAlertRepository ghi cả nhóm cột cùng lúc);
-- các ràng buộc này là lớp chặn cuối để bug sau này không lưu được dữ liệu vô lý.
-- 'DOCTOR' vẫn được phép ở cột loại người vì dữ liệu mẫu cũ (V46) còn dùng.
ALTER TABLE triage_alerts
    -- Đã đóng <=> có giờ đóng <=> có kết quả.
    ADD CONSTRAINT ck_triage_alerts_resolved_consistent
        CHECK ((status = 'RESOLVED') = (resolved_at IS NOT NULL)
           AND (status = 'RESOLVED') = (outcome IS NOT NULL)),
    -- Người đóng: loại và id đi cùng nhau, chỉ có khi đã đóng.
    ADD CONSTRAINT ck_triage_alerts_resolver_pair
        CHECK ((resolved_by_type IS NULL) = (resolved_by_id IS NULL)
           AND (resolved_by_type IS NULL OR status = 'RESOLVED')),
    ADD CONSTRAINT ck_triage_alerts_resolver_type
        CHECK (resolved_by_type IS NULL OR resolved_by_type IN ('ADMIN', 'PATIENT', 'DOCTOR')),
    -- Người đang liên hệ: cả 3 cột cùng có hoặc cùng trống.
    ADD CONSTRAINT ck_triage_alerts_claim_triplet
        CHECK ((claimed_by_type IS NULL) = (claimed_by_id IS NULL)
           AND (claimed_by_type IS NULL) = (claimed_at IS NULL)),
    ADD CONSTRAINT ck_triage_alerts_claimer_type
        CHECK (claimed_by_type IS NULL OR claimed_by_type IN ('ADMIN', 'DOCTOR')),
    -- Bệnh nhân trả lời: câu trả lời và giờ trả lời đi cùng nhau.
    ADD CONSTRAINT ck_triage_alerts_patient_response_pair
        CHECK ((patient_response IS NULL) = (patient_responded_at IS NULL)),
    -- Cần can thiệp thì phải biết từ lúc nào.
    ADD CONSTRAINT ck_triage_alerts_intervention_time
        CHECK (status <> 'NEEDS_INTERVENTION' OR needs_intervention_at IS NOT NULL);